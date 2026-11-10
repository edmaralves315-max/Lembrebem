export class Store {
 constructor(db){this.db=db;}
 async all(sql,...args){return (await this.db.prepare(sql).bind(...args).all()).results;}
 async one(sql,...args){return await this.db.prepare(sql).bind(...args).first();}
 async run(sql,...args){return await this.db.prepare(sql).bind(...args).run();}
 async audit(person,uid,action,version=null){await this.run('INSERT INTO audit VALUES(?,?,?,?,?,?)',crypto.randomUUID(),person,uid,action,new Date().toISOString(),version);}
 async access(person,uid,scope){const p=await this.one('SELECT * FROM people WHERE id=?',person);if(!p)return null;if(p.owner===uid)return p;if(['health','location'].includes(scope)&&!JSON.parse(p.consents)[scope])return null;const g=await this.one('SELECT * FROM grants WHERE personId=? AND uid=?',person,uid);if(!g||!JSON.parse(g.scopes).includes(scope))return null;return p;}
}
