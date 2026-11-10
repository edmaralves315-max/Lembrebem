const encoder=new TextEncoder();
export const b64url=b=>btoa(String.fromCharCode(...new Uint8Array(b))).replaceAll('+','-').replaceAll('/','_').replaceAll('=','');
export const decode=s=>Uint8Array.from(atob(s.replaceAll('-','+').replaceAll('_','/')),c=>c.charCodeAt(0));
let keysCache;
export async function verifyFirebase(token,project,now=Date.now(),fetcher=fetch){
 if(!project||!token||token.length>8192)throw Error('invalid_token');
 const parts=token.split('.');if(parts.length!==3)throw Error('invalid_token');
 const head=JSON.parse(new TextDecoder().decode(decode(parts[0]))),body=JSON.parse(new TextDecoder().decode(decode(parts[1])));
 if(head.alg!=='RS256'||!head.kid||body.aud!==project||body.iss!==`https://securetoken.google.com/${project}`||typeof body.sub!=='string'||!body.sub||body.sub.length>128||!Number.isFinite(body.exp)||body.exp*1000<=now||!Number.isFinite(body.iat)||body.iat*1000>now+30000||!Number.isFinite(body.auth_time)||body.auth_time*1000>now+30000)throw Error('invalid_token');
 if(!keysCache||keysCache.until<now){const r=await fetcher('https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com');if(!r.ok)throw Error('keys_unavailable');keysCache={keys:(await r.json()).keys,until:now+300000};}
 const jwk=keysCache.keys.find(k=>k.kid===head.kid);if(!jwk)throw Error('invalid_token');
 const key=await crypto.subtle.importKey('jwk',jwk,{name:'RSASSA-PKCS1-v1_5',hash:'SHA-256'},false,['verify']);
 if(!await crypto.subtle.verify('RSASSA-PKCS1-v1_5',key,decode(parts[2]),encoder.encode(parts[0]+'.'+parts[1])))throw Error('invalid_token');
 return body.sub;
}
export async function sha256(s){return b64url(await crypto.subtle.digest('SHA-256',encoder.encode(s)));}
let oauth;
export async function serviceAccessToken(env,now=Date.now()){
 if(oauth&&oauth.until>now)return oauth.token;
 const account=JSON.parse(env.FCM_SERVICE_ACCOUNT_JSON||'{}');if(!account.private_key||!account.client_email)throw Error('push_not_configured');
 const seconds=Math.floor(now/1000),h=b64url(encoder.encode(JSON.stringify({alg:'RS256',typ:'JWT'}))),p=b64url(encoder.encode(JSON.stringify({iss:account.client_email,scope:'https://www.googleapis.com/auth/firebase.messaging',aud:'https://oauth2.googleapis.com/token',iat:seconds,exp:seconds+3600})));
 const key=await crypto.subtle.importKey('pkcs8',decode(account.private_key.replace(/-----[^-]+-----|\s/g,'')),{name:'RSASSA-PKCS1-v1_5',hash:'SHA-256'},false,['sign']);
 const assertion=h+'.'+p+'.'+b64url(await crypto.subtle.sign('RSASSA-PKCS1-v1_5',key,encoder.encode(h+'.'+p)));
 const r=await fetch('https://oauth2.googleapis.com/token',{method:'POST',headers:{'content-type':'application/x-www-form-urlencoded'},body:new URLSearchParams({grant_type:'urn:ietf:params:oauth:grant-type:jwt-bearer',assertion})});
 if(!r.ok)throw Error('push_auth_failed');const data=await r.json();oauth={token:data.access_token,until:now+Math.min(data.expires_in,3500)*1000};return oauth.token;
}
