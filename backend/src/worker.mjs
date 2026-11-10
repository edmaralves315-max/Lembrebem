import {verifyFirebase} from './auth.mjs';
import {route,ApiError} from './api.mjs';
import {Store} from './store.mjs';
import {tick} from './scheduler.mjs';
export function response(data,status=200){return Response.json(data,{status,headers:{'cache-control':'no-store','x-content-type-options':'nosniff','referrer-policy':'no-referrer'}});}
export async function handle(request,env,authenticate){try{if(new URL(request.url).pathname==='/health')return response({service:'LembreBem',pushMode:env.PUSH_MODE==='demo'?'demo':'fcm',time:new Date().toISOString()});const token=request.headers.get('authorization')?.match(/^Bearer (.+)$/)?.[1];let uid;try{uid=await authenticate(token);}catch{return response({error:'unauthorized'},401);}if(!uid)return response({error:'unauthorized'},401);return response(await route(request,env,uid));}catch(e){if(e instanceof ApiError)return response({error:e.message},e.status);if(String(e.message).includes('UNIQUE constraint'))return response({error:'conflict'},409);return response({error:'service_unavailable'},503);}}
export default {fetch(request,env){return handle(request,env,token=>verifyFirebase(token,env.FIREBASE_PROJECT_ID));},async scheduled(_event,env,ctx){ctx.waitUntil(tick(new Store(env.DB),env));}};
