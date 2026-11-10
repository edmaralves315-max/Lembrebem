import test from 'node:test';import assert from 'node:assert/strict';
import {localDB} from '../src/local-db.mjs';import {handle} from '../src/worker.mjs';import {Store} from '../src/store.mjs';import {tick,scheduleAt} from '../src/scheduler.mjs';import {messages,localized} from '../src/i18n.mjs';import {verifyFirebase} from '../src/auth.mjs';
const person='10000000-0000-4000-8000-000000000001',plan='20000000-0000-4000-8000-000000000001';
function setup(){const env={DB:localDB(),PUSH_MODE:'demo'};return {env,store:new Store(env.DB),async req(method,path,body,uid='owner'){const r=await handle(new Request('https://example.test/v1'+path,{method,headers:{'content-type':'application/json'},...(body?{body:JSON.stringify(body)}:{})}),env,async()=>uid);return {status:r.status,data:await r.json()};}};}
async function seed(s){await s.req('POST','/people',{id:person,preferredLocale:'ja-JP',countryCode:'BR',timeZone:'UTC',consents:{health:true,location:true}});}
async function invite(s,uid,locale='en-US',scopes=['health','location','alerts']){const a=await s.req('POST',`/people/${person}/invites`,{scopes,consentConfirmed:true});return s.req('POST','/invites/accept',{code:a.data.code,preferredLocale:locale},uid);}
const schedule={id:plan,version:1,approved:true,sourceReviewed:true,name:'Fictício A',dosage:'Texto literal fictício',time:'08:00',days:[1,2,3,4,5,6,7],startDate:'2025-01-01',timeZone:'UTC'};
test('authentication, tenant isolation, revoke, single-use invite and private export',async()=>{const s=setup();await seed(s);assert.equal((await s.req('GET',`/people/${person}`,null,null)).status,401);assert.equal((await s.req('GET',`/people/${person}`,null,'stranger')).status,403);const inv=await s.req('POST',`/people/${person}/invites`,{scopes:['read','alerts'],consentConfirmed:true});assert.equal((await s.req('POST','/invites/accept',{code:inv.data.code,preferredLocale:'en-US'},'a')).status,200);assert.equal((await s.req('POST','/invites/accept',{code:inv.data.code,preferredLocale:'en-US'},'b')).status,400);assert.equal((await s.req('GET',`/people/${person}/export`,null,'a')).status,403);await s.req('DELETE',`/people/${person}/grants/a`);assert.equal((await s.req('GET',`/people/${person}`,null,'a')).status,403);});
test('medication cron all authorized recipients, 15min, localized, dedup and late acknowledgement',async()=>{const s=setup();await seed(s);await invite(s,'a','en-US');await invite(s,'b','pt-PT');await s.req('POST',`/people/${person}/plans`,schedule);const t=new Date('2026-11-09T08:00:00.000Z');await tick(s.store,s.env,t);await tick(s.store,s.env,t);assert.equal((await s.store.all("SELECT * FROM alerts WHERE kind='dose'")).length,3);await tick(s.store,s.env,new Date(t.getTime()+900000));assert.equal((await s.store.all("SELECT * FROM alerts WHERE kind='unconfirmed'")).length,3);const doseId=plan+':'+t.toISOString();const ack={id:crypto.randomUUID(),type:'dose_ack',occurredAt:t.toISOString(),payload:{doseId,status:'taken'}};assert.equal((await s.req('POST',`/people/${person}/events`,ack)).status,200);assert.equal((await s.req('POST',`/people/${person}/events`,ack)).data.duplicate,true);assert.equal((await s.req('POST',`/people/${person}/events`,{...ack,payload:{doseId,status:'not_yet'}})).status,409);const alerts=(await s.req('GET',`/people/${person}/alerts`,null,'a')).data.alerts;assert.equal(alerts[0].data.locale,'en-US');assert.ok(alerts.find(a=>a.kind==='unconfirmed').data.acknowledgedAt);assert.equal(localized('unconfirmed','en-US').body.includes('No confirmation'),true);});
test('confirmation before 15min cancels additional notification',async()=>{const s=setup();await seed(s);await s.req('POST',`/people/${person}/plans`,schedule);const at='2026-11-09T08:00:00.000Z';await s.req('POST',`/people/${person}/events`,{id:crypto.randomUUID(),type:'dose_ack',occurredAt:at,payload:{doseId:plan+':'+at,status:'taken'}});await tick(s.store,s.env,new Date('2026-11-09T08:15:00Z'));assert.equal((await s.store.all("SELECT * FROM alerts WHERE kind='unconfirmed'")).length,0);});
test('human review and version conflict preserve clinical literals',async()=>{const s=setup();await seed(s);assert.equal((await s.req('POST',`/people/${person}/plans`,{...schedule,approved:false})).status,400);assert.equal((await s.req('POST',`/people/${person}/plans`,schedule)).status,200);assert.equal((await s.req('POST',`/people/${person}/plans`,schedule)).status,409);assert.equal((await s.req('GET',`/people/${person}/plans`)).data.plans[0].data.dosage,schedule.dosage);});
test('battery thresholds deduplicate and real charging state',async()=>{const s=setup();await seed(s);for(const [level,charging] of [[50,false],[30,false],[28,false],[15,false],[14,false],[14,true]]){await s.req('POST',`/people/${person}/events`,{id:crypto.randomUUID(),type:'battery',occurredAt:new Date().toISOString(),payload:{level,charging}});}assert.equal((await s.store.all("SELECT * FROM alerts WHERE kind='battery'")).length,2);assert.equal((await s.store.all("SELECT * FROM alerts WHERE kind='charging'")).length,1);});
test('scope privacy filters health and location events; delete cascades',async()=>{const s=setup();await seed(s);await invite(s,'limited','es-ES',['read','alerts']);await s.req('POST',`/people/${person}/events`,{id:crypto.randomUUID(),type:'geofence_exit',occurredAt:new Date().toISOString(),payload:{latitude:0,longitude:0,accuracy:100,timestamp:new Date().toISOString()}});assert.equal((await s.req('GET',`/people/${person}/sync`,null,'limited')).data.events.length,0);assert.equal((await s.req('GET',`/people/${person}/alerts`,null,'limited')).data.alerts.length,0);await s.req('DELETE',`/people/${person}`);assert.equal((await s.store.all('SELECT * FROM events')).length,0);});
test('heartbeat timeout neutral timestamp and idempotent',async()=>{const s=setup();await seed(s);await s.req('POST',`/people/${person}/heartbeat`,{occurredAt:'2026-11-09T01:00:00Z'});await tick(s.store,s.env,new Date('2026-11-09T04:00:00Z'));await tick(s.store,s.env,new Date('2026-11-09T04:01:00Z'));const alerts=await s.store.all("SELECT * FROM alerts WHERE kind='heartbeat'");assert.equal(alerts.length,1);assert.equal(JSON.parse(alerts[0].data).lastCommunication,'2026-11-09T01:00:00Z');});
test('all five locales complete and DST schedule independent of language',()=>{const expected=Object.keys(messages['pt-BR']).sort();for(const m of Object.values(messages))assert.deepEqual(Object.keys(m).sort(),expected);assert.equal(localized('dose','invalid').locale,'pt-BR');assert.ok(scheduleAt({...schedule,timeZone:'America/New_York'},new Date('2026-03-08T12:00:00Z')));assert.ok(scheduleAt({...schedule,timeZone:'America/New_York'},new Date('2026-03-07T13:00:00Z')));});
test('rate limit per authenticated user',async()=>{const s=setup();let last;for(let i=0;i<121;i++)last=await s.req('GET','/people');assert.equal(last.status,429);});
test('Firebase rejects absent expired wrong issuer and unsigned tokens',async()=>{for(const value of [null,'abc','e30.e30.x'])await assert.rejects(verifyFirebase(value,'project'));});
test('Firebase verifies real RSA signature, audience, expiry and rejects forgery',async()=>{const {b64url}=await import('../src/auth.mjs');const keys=await crypto.subtle.generateKey({name:'RSASSA-PKCS1-v1_5',modulusLength:2048,publicExponent:new Uint8Array([1,0,1]),hash:'SHA-256'},true,['sign','verify']);const jwk=await crypto.subtle.exportKey('jwk',keys.publicKey);jwk.kid='test-key';const now=Date.now(),sec=Math.floor(now/1000),enc=new TextEncoder();const sign=async claims=>{const h=b64url(enc.encode(JSON.stringify({alg:'RS256',kid:'test-key'}))),p=b64url(enc.encode(JSON.stringify(claims)));return h+'.'+p+'.'+b64url(await crypto.subtle.sign('RSASSA-PKCS1-v1_5',keys.privateKey,enc.encode(h+'.'+p)));};const claims={aud:'project',iss:'https://securetoken.google.com/project',sub:'fictitious-user',exp:sec+3600,iat:sec,auth_time:sec};const token=await sign(claims),fetcher=async()=>Response.json({keys:[jwk]});assert.equal(await verifyFirebase(token,'project',now,fetcher),'fictitious-user');await assert.rejects(verifyFirebase(await sign({...claims,exp:sec-1}),'project',now,fetcher));await assert.rejects(verifyFirebase(token,'other',now,fetcher));await assert.rejects(verifyFirebase(token.slice(0,-5)+'aaaaa','project',now,fetcher));});
test('production Worker never accepts demo header',async()=>{const {default:worker}=await import('../src/worker.mjs');const r=await worker.fetch(new Request('https://example.test/v1/people',{headers:{'x-demo-user':'owner'}}),{DB:localDB(),PUSH_MODE:'demo',FIREBASE_PROJECT_ID:'project'});assert.equal(r.status,401);});

test('S-A2 withdrawn category consent hides existing alerts and revokes pending delivery',async()=>{
 for(const category of ['health','location']){const s=setup();await seed(s);await invite(s,'a');s.env.PUSH_MODE='live';
 const at='2026-11-09T08:00:00.000Z';
 if(category==='health'){await s.req('POST',`/people/${person}/plans`,schedule);await tick(s.store,s.env,new Date(at));}
 else await s.req('POST',`/people/${person}/events`,{id:crypto.randomUUID(),type:'geofence_exit',occurredAt:at,payload:{locationUnavailable:true}});
 assert.ok((await s.req('GET',`/people/${person}/alerts`,null,'a')).data.alerts.length>0);
 assert.equal((await s.req('PUT',`/people/${person}`,{preferredLocale:'ja-JP',countryCode:'BR',timeZone:'UTC',consents:{health:category!=='health',location:category!=='location'},heartbeatMinutes:120})).status,200);
 assert.equal((await s.req('GET',`/people/${person}/alerts`,null,'a')).data.alerts.length,0);
 const {deliver}=await import('../src/scheduler.mjs');await deliver(s.store,s.env,new Date(Date.now()+60000));
 assert.equal((await s.store.all("SELECT * FROM alerts WHERE delivery='pending'")).length,0);
 assert.ok((await s.store.all("SELECT * FROM alerts WHERE delivery='revoked'")).length>0);
 assert.equal((await s.req('GET',`/people/${person}/sync`)).data[category==='health'?'plans':'events'].length,0);
 }
});
test('S-A2 removed category scope or disabled preference blocks old alert read and delivery',async()=>{
 const s=setup();await seed(s);await invite(s,'a');s.env.PUSH_MODE='live';
 await s.req('POST',`/people/${person}/events`,{id:crypto.randomUUID(),type:'geofence_exit',occurredAt:new Date().toISOString(),payload:{locationUnavailable:true}});
 assert.equal((await s.req('PUT',`/people/${person}/grants`,{preferredLocale:'en-US',preferences:{geofence_exit:false}},'a')).status,200);
 assert.equal((await s.req('GET',`/people/${person}/alerts`,null,'a')).data.alerts.length,0);
 await s.req('PUT',`/people/${person}/grants`,{preferredLocale:'en-US',preferences:{}},'a');
 assert.equal((await s.req('GET',`/people/${person}/alerts`,null,'a')).data.alerts.length,1);
 await s.store.run('UPDATE grants SET scopes=? WHERE personId=? AND uid=?',JSON.stringify(['read','alerts']),person,'a');
 assert.equal((await s.req('GET',`/people/${person}/alerts`,null,'a')).data.alerts.length,0);
 const {deliver}=await import('../src/scheduler.mjs');await deliver(s.store,s.env,new Date(Date.now()+60000));
 assert.equal((await s.store.one('SELECT delivery FROM alerts WHERE personId=? AND uid=?',person,'a')).delivery,'revoked');
});
test('S-A3 offline late taken acknowledgement cancels queued unconfirmed delivery and preserves history',async()=>{
 const s=setup();await seed(s);s.env.PUSH_MODE='live';await s.req('POST',`/people/${person}/plans`,schedule);const at='2026-11-09T08:00:00.000Z';await tick(s.store,s.env,new Date('2026-11-09T08:15:00Z'));
 assert.equal((await s.store.one("SELECT delivery FROM alerts WHERE kind='unconfirmed'")).delivery,'pending');
 await s.req('POST',`/people/${person}/events`,{id:crypto.randomUUID(),type:'dose_ack',occurredAt:at,payload:{doseId:plan+':'+at,status:'taken'}});
 let row=await s.store.one("SELECT * FROM alerts WHERE kind='unconfirmed'");assert.equal(row.delivery,'cancelled');assert.equal(JSON.parse(row.data).acknowledgedAt,at);
 // Revalidate immediately before sending too, even if a previously queued row was not reconciled.
 await s.store.run("UPDATE alerts SET delivery='retry' WHERE kind='unconfirmed'");
 await s.req('POST','/subscriptions',{token:'fictitious-fcm-token',preferredLocale:'en-US'});
 const {deliver}=await import('../src/scheduler.mjs');await deliver(s.store,{...s.env,PUSH_MODE:'demo'},new Date(Date.now()+60000));
 row=await s.store.one("SELECT * FROM alerts WHERE kind='unconfirmed'");assert.equal(row.delivery,'cancelled');assert.equal(row.attempts,0);
});
test('S-A4 geofence without location is explicit, idempotent, no fabricated map and does not block following battery',async()=>{
 const s=setup();await seed(s);const event={id:crypto.randomUUID(),type:'geofence_exit',occurredAt:new Date().toISOString(),payload:{locationUnavailable:true}};
 assert.equal((await s.req('POST',`/people/${person}/events`,event)).status,200);assert.equal((await s.req('POST',`/people/${person}/events`,event)).data.duplicate,true);
 assert.equal((await s.req('POST',`/people/${person}/events`,{...event,id:crypto.randomUUID(),payload:{}})).status,400);
 assert.equal((await s.req('POST',`/people/${person}/events`,{...event,id:crypto.randomUUID(),payload:{locationUnavailable:true,latitude:0}})).status,400);
 assert.equal((await s.req('POST',`/people/${person}/events`,{id:crypto.randomUUID(),type:'battery',occurredAt:event.occurredAt,payload:{level:15,charging:false}})).status,200);
 const alerts=(await s.req('GET',`/people/${person}/alerts`)).data.alerts;const exit=alerts.find(a=>a.kind==='geofence_exit');assert.equal(exit.data.locationUnavailable,true);assert.equal('latitude' in exit.data,false);assert.ok(alerts.some(a=>a.kind==='battery'));
});

test('S-B1 read-only caregivers cannot impersonate assisted-device telemetry',async()=>{
 const s=setup();await seed(s);await invite(s,'read-only','en-US',['read']);const at=new Date().toISOString();
 assert.equal((await s.req('POST',`/people/${person}/heartbeat`,{occurredAt:at},'read-only')).status,403);
 for(const [type,payload] of [['battery',{level:15,charging:false}],['help',{}],['connectivity',{}]])assert.equal((await s.req('POST',`/people/${person}/events`,{id:crypto.randomUUID(),type,occurredAt:at,payload},'read-only')).status,403);
 assert.equal((await s.store.all('SELECT * FROM events')).length,0);
 assert.equal((await s.store.one('SELECT heartbeat FROM people WHERE id=?',person)).heartbeat,null);
});
test('S-B2 latest human correction controls confirmation and preserves all audit events',async()=>{
 const s=setup();await seed(s);s.env.PUSH_MODE='live';await s.req('POST',`/people/${person}/plans`,schedule);const at='2026-11-09T08:00:00.000Z',doseId=plan+':'+at;
 await tick(s.store,s.env,new Date('2026-11-09T08:15:00Z'));
 const ack=async(status,occurredAt)=>s.req('POST',`/people/${person}/events`,{id:crypto.randomUUID(),type:'dose_ack',occurredAt,payload:{doseId,status}});
 await ack('taken','2026-11-09T08:01:00.000Z');assert.equal((await s.store.one("SELECT delivery FROM alerts WHERE kind='unconfirmed'")).delivery,'cancelled');
 await ack('not_yet','2026-11-09T08:02:00.000Z');await tick(s.store,s.env,new Date('2026-11-09T08:16:00Z'));
 let alert=await s.store.one("SELECT * FROM alerts WHERE kind='unconfirmed'");assert.equal(alert.delivery,'pending');assert.equal(JSON.parse(alert.data).acknowledgedAt,undefined);
 await ack('taken','2026-11-09T08:00:30.000Z');alert=await s.store.one("SELECT * FROM alerts WHERE kind='unconfirmed'");assert.equal(alert.delivery,'pending');
 assert.equal((await s.store.all("SELECT * FROM events WHERE type='dose_ack'")).length,3);
});
test('S-B3 sync cursor traverses more than 500 events with identical received timestamps',async()=>{
 const s=setup();await seed(s);const at='2026-11-09T08:00:00.000Z';
 for(let i=0;i<1003;i++)await s.store.run('INSERT INTO events VALUES(?,?,?,?,?,?,?)',crypto.randomUUID(),person,'help',at,at,'{}','owner');
 const ids=new Set();let cursor=null,pages=0;
 do{const page=await s.req('GET',`/people/${person}/sync`+(cursor?'?cursor='+encodeURIComponent(cursor):''));assert.equal(page.status,200);for(const e of page.data.events){assert.equal(ids.has(e.id),false);ids.add(e.id);}cursor=page.data.nextCursor;pages++;if(!page.data.hasMore)break;}while(pages<10);
 assert.equal(ids.size,1003);assert.equal(pages,3);
 assert.equal((await s.req('GET',`/people/${person}/sync?cursor=bad`)).status,400);
});
test('S-B4 malformed appointment cannot poison Android sync',async()=>{
 const s=setup();await seed(s);const event={id:crypto.randomUUID(),type:'appointment',occurredAt:new Date().toISOString(),payload:{}};
 assert.equal((await s.req('POST',`/people/${person}/events`,event)).status,400);
 assert.equal((await s.req('POST',`/people/${person}/events`,{...event,payload:{id:crypto.randomUUID(),title:'Consulta fictícia',at:'2026-11-11T12:00:00.000Z',place:''}})).status,200);
 assert.equal((await s.req('GET',`/people/${person}/sync`)).data.events.length,1);
});
