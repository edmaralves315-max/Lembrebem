import {localized} from './i18n.mjs';
import {serviceAccessToken} from './auth.mjs';
export function scheduleAt(plan,instant){const parts=Object.fromEntries(new Intl.DateTimeFormat('en-US',{timeZone:plan.timeZone,year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',hourCycle:'h23',weekday:'short'}).formatToParts(instant).map(p=>[p.type,p.value]));const date=`${parts.year}-${parts.month}-${parts.day}`;const day={Mon:1,Tue:2,Wed:3,Thu:4,Fri:5,Sat:6,Sun:7}[parts.weekday];return (!plan.startDate||date>=plan.startDate)&&(!plan.endDate||date<=plan.endDate)&&plan.days.includes(day)&&`${parts.hour}:${parts.minute}`===plan.time;}
// Recheck category consent and current grants both when reading and before sending.
export async function alertAllowed(store,alert){
 const person=await store.access(alert.personId,alert.uid,'alerts');if(!person)return false;
 const consent=JSON.parse(person.consents),data=typeof alert.data==='string'?JSON.parse(alert.data):alert.data||{};
 const category=['dose','unconfirmed'].includes(alert.kind)?'health':alert.kind==='geofence_exit'?'location':null;
 if(category&&(!consent[category]||!await store.access(alert.personId,alert.uid,category)))return false;
 if(person.owner!==alert.uid){const grant=await store.one('SELECT preferences FROM grants WHERE personId=? AND uid=?',alert.personId,alert.uid);if(!grant)return false;const prefs=JSON.parse(grant.preferences);if(prefs[alert.kind]===false||(data.planId&&prefs['plan:'+data.planId]===false))return false;}
 return true;
}
export async function latestAcknowledgement(store,personId,doseId){
 for(const row of await store.all("SELECT occurredAt,payload FROM events WHERE personId=? AND type='dose_ack' ORDER BY occurredAt DESC,receivedAt DESC,rowid DESC",personId)){const event=JSON.parse(row.payload);if(event.doseId===doseId)return {...event,occurredAt:row.occurredAt};}return null;
}
async function takenAcknowledgement(store,alert){const ack=await latestAcknowledgement(store,alert.personId,JSON.parse(alert.data).doseId);return ack?.status==='taken'?ack.occurredAt:null;}
export async function notify(store,personId,kind,key,occurredAt,data={}){
 const p=await store.one('SELECT * FROM people WHERE id=?',personId);if(!p)return;
 const candidates=[{uid:p.owner,preferredLocale:p.preferredLocale,preferences:'{}',scopes:'["alerts","health","location"]'},...await store.all('SELECT * FROM grants WHERE personId=?',personId)];
 for(const c of candidates){if(!await alertAllowed(store,{personId,uid:c.uid,kind,data}))continue;const scopes=JSON.parse(c.scopes),prefs=JSON.parse(c.preferences);if(!scopes.includes('alerts')||prefs[kind]===false||(data.planId&&prefs['plan:'+data.planId]===false))continue;if(['dose','unconfirmed'].includes(kind)&&!scopes.includes('health'))continue;if(kind==='geofence_exit'&&!scopes.includes('location'))continue;await store.run('INSERT OR IGNORE INTO alerts(id,personId,uid,kind,occurredAt,data,nextAttempt) VALUES(?,?,?,?,?,?,?)',`${key}:${c.uid}`,personId,c.uid,kind,occurredAt,JSON.stringify({...data,locale:c.preferredLocale}),new Date().toISOString());}
}
export async function tick(store,env,now=new Date()){
 const people=await store.all('SELECT * FROM people');
 for(const p of people){
 if(JSON.parse(p.consents).health){for(const row of await store.all('SELECT * FROM plans WHERE personId=?',p.id)){const plan=JSON.parse(row.data);for(let ago=0;ago<=30;ago++){const at=new Date(Math.floor(now.getTime()/60000)*60000-ago*60000);if(!scheduleAt(plan,at))continue;const scheduledAt=at.toISOString(),doseId=row.id+':'+scheduledAt;
 await notify(store,p.id,'dose','dose:'+doseId,scheduledAt,{doseId,planId:row.id,scheduledAt});
 if(now-at>=900000){const acknowledgement=await latestAcknowledgement(store,p.id,doseId);if(acknowledgement?.status!=='taken'){await notify(store,p.id,'unconfirmed','unconfirmed:'+doseId,scheduledAt,{doseId,planId:row.id,scheduledAt,syncMayBeDelayed:true});await store.run("UPDATE alerts SET delivery='pending',data=json_remove(data,'$.acknowledgedAt','$.reconciledAt') WHERE personId=? AND kind='unconfirmed' AND delivery='cancelled' AND json_extract(data,'$.doseId')=?",p.id,doseId);}}
 }}}
 if(p.heartbeat&&now-new Date(p.heartbeat)>p.heartbeatMinutes*60000)await notify(store,p.id,'heartbeat','heartbeat:'+p.id+':'+p.heartbeat,p.heartbeat,{lastCommunication:p.heartbeat});
 }
 await deliver(store,env,now);
 await store.run('DELETE FROM limits WHERE bucket<?',Math.floor(now.getTime()/60000)-10);
 await store.run('DELETE FROM invites WHERE expiresAt<?',now.toISOString());
 const cutoff=new Date(now.getTime()-90*86400000).toISOString();await store.run('DELETE FROM alerts WHERE occurredAt<?',cutoff);await store.run('DELETE FROM events WHERE receivedAt<?',cutoff);
}
export async function deliver(store,env,now=new Date()){
 for(const alert of await store.all("SELECT * FROM alerts WHERE delivery IN ('pending','retry') AND nextAttempt<=? LIMIT 100",now.toISOString())){
 if(!await alertAllowed(store,alert)){await store.run("UPDATE alerts SET delivery='revoked' WHERE id=?",alert.id);continue;}
 if(alert.kind==='unconfirmed'){const acknowledgedAt=await takenAcknowledgement(store,alert);if(acknowledgedAt){await store.run("UPDATE alerts SET delivery='cancelled',data=? WHERE id=?",JSON.stringify({...JSON.parse(alert.data),acknowledgedAt,reconciledAt:now.toISOString()}),alert.id);continue;}}
 const subscriptions=await store.all('SELECT * FROM subscriptions WHERE uid=?',alert.uid);if(env.PUSH_MODE==='demo'){await store.run("UPDATE alerts SET delivery='demo' WHERE id=?",alert.id);continue;}
 if(!subscriptions.length)continue;
 let failed=false;
 for(const s of subscriptions){const message=localized(alert.kind,s.preferredLocale);try{const token=await serviceAccessToken(env,now.getTime());const r=await fetch(`https://fcm.googleapis.com/v1/projects/${encodeURIComponent(env.FIREBASE_PROJECT_ID)}/messages:send`,{method:'POST',headers:{Authorization:'Bearer '+token,'content-type':'application/json'},body:JSON.stringify({message:{token:s.token,notification:{title:message.title,body:message.body},data:{alertId:alert.id,locale:message.locale},android:{priority:'high',notification:{visibility:'PRIVATE',tag:alert.id}}}})});if(r.status===404||r.status===410)await store.run('DELETE FROM subscriptions WHERE uid=? AND token=?',s.uid,s.token);else if(!r.ok)failed=true;}catch{failed=true;}}
 const attempts=alert.attempts+1;await store.run('UPDATE alerts SET delivery=?,attempts=?,nextAttempt=? WHERE id=?',failed?(attempts>=8?'failed':'retry'):'sent',attempts,new Date(now.getTime()+Math.min(3600000,30000*2**attempts)).toISOString(),alert.id);
 }
}
