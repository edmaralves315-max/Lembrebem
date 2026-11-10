import {DatabaseSync} from 'node:sqlite';
import {readFileSync} from 'node:fs';
export function localDB(path=':memory:'){
 const db=new DatabaseSync(path);db.exec(readFileSync(new URL('../migrations/0001_initial.sql',import.meta.url),'utf8').replaceAll('CREATE TABLE ','CREATE TABLE IF NOT EXISTS ').replaceAll('CREATE INDEX ','CREATE INDEX IF NOT EXISTS '));
 return {prepare(sql){return {bind(...args){const q=db.prepare(sql);return {async all(){return {results:q.all(...args)};},async first(){return q.get(...args)||null;},async run(){const r=q.run(...args);return {meta:{changes:Number(r.changes)}};}};}};},close(){db.close();}};
}
