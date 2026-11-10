import {createServer} from 'node:http';
import {localDB} from './local-db.mjs';
import {handle} from './worker.mjs';
import {tick} from './scheduler.mjs';
import {Store} from './store.mjs';
const env={DB:localDB(process.env.DEMO_DB||'demo.sqlite'),PUSH_MODE:'demo'};
const port=Number(process.env.PORT||8787);
createServer(async(req,res)=>{const chunks=[];let size=0;for await(const chunk of req){size+=chunk.length;if(size>65536){res.writeHead(413);res.end();return;}chunks.push(chunk);}const request=new Request('http://127.0.0.1:'+port+req.url,{method:req.method,headers:req.headers,...(['GET','HEAD'].includes(req.method)?{}:{body:Buffer.concat(chunks)})});const uid=request.headers.get('x-demo-user');const result=await handle(request,env,async()=>typeof uid==='string'&&/^[a-z0-9-]{1,64}$/i.test(uid)?uid:null);res.writeHead(result.status,Object.fromEntries(result.headers));res.end(await result.text());}).listen(port,'127.0.0.1',()=>console.log(`DEMONSTRAÇÃO LOCAL: http://127.0.0.1:${port}; x-demo-user; push simulado; nunca usar dados reais.`));
setInterval(()=>tick(new Store(env.DB),env).catch(()=>console.error('demo_tick_failed')),60000).unref();
