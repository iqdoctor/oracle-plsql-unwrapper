'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { server, cfg, resetRateLimit, getActiveCount } = require('../server');
const fixture = fs.readFileSync(path.join(__dirname, 'fixtures/single-v2.sql'));
let base;
test.before(async () => {
  cfg.rate = 1000; cfg.maxBytes = 2 * 1024 * 1024; cfg.timeout = 15000; resetRateLimit();
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  base = `http://127.0.0.1:${server.address().port}`;
});
test.after(async () => { await new Promise(resolve => server.close(resolve)); });
async function send(body, pathName='/api/unwrap') {
  return fetch(base + pathName, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(body), cache: 'no-store' });
}
const paste = (buf, rightsConfirmed=true) => ({ rightsConfirmed, kind:'paste', payloadBase64:Buffer.from(buf).toString('base64') });
const upload = (buf, fileName='one.sql', rightsConfirmed=true) => ({ rightsConfirmed, kind:'file', fileName, payloadBase64:Buffer.from(buf).toString('base64') });

test('server binds loopback by default and requires backend rights attestation without spawning', async () => {
  assert.equal(cfg.host, '127.0.0.1');
  const old = cfg.java; cfg.java = '/no/such/java';
  try {
    for (const flag of [false, null]) {
      const r = await send(paste(fixture, flag)); assert.equal(r.status, 400);
      assert.match((await r.json()).error, /lawful rights/);
    }
  } finally { cfg.java = old; }
});

test('HTTP V2 restore, V1 routing, and multi-object route include CTAs and CLI guidance', async () => {
  let r = await send(paste(fixture)); let j = await r.json();
  assert.equal(j.status, 'RESTORED'); assert.equal(j.format, 'V2'); assert.ok(j.source.includes('create or replace procedure'));
  r = await send(paste('PACKAGE BODY old_pkg wrapped\n0\nabcd\n'));
  j = await r.json(); assert.equal(j.status, 'V1_ROUTED'); assert.equal(j.contacts.email, 'ashindarev@gmail.com');
  r = await send(paste(Buffer.concat([fixture, Buffer.from('\n'), fixture])));
  j = await r.json(); assert.equal(j.status, 'MULTI_OBJECT'); assert.match(j.cli, /github.com/);
  assert.equal(j.contacts.email, 'ashindarev@gmail.com'); assert.match(j.contacts.linkedin, /linkedin.com/);
  const html = await (await fetch(base + '/')).text(); const app = await (await fetch(base + '/static/app.js')).text();
  assert.match(html, /ashindarev@gmail.com/); assert.match(app, /For bulk recovery or other complex cases/);
});

test('uploads preserve UTF-8 and Latin-1; extension and pasted UTF-8 are validated', async () => {
  let r = await send(upload(fixture, 'sample.plb')); assert.equal((await r.json()).status, 'RESTORED');
  const latin = Buffer.from(fixture); const name = Buffer.from('stray_cr'); const at = latin.indexOf(name); assert.notEqual(at, -1);
  const latinBytes = Buffer.concat([latin.subarray(0, at), Buffer.from([0x73,0x74,0x72,0x61,0x79,0x5f,0xe9]), latin.subarray(at+name.length)]);
  r = await send(upload(latinBytes, 'latin.sql')); const result = await r.json(); assert.equal(result.status, 'RESTORED'); assert.match(result.objectName, /É/i);
  r = await send(upload(fixture, 'bad.txt')); assert.equal(r.status, 400);
  r = await send(paste(Buffer.from([0xff,0xfe]))); assert.equal(r.status, 400); assert.match((await r.json()).error, /UTF-8/);
});

test('backend enforces decoded byte size and request rate limits', async () => {
  const oldSize=cfg.maxBytes; cfg.maxBytes=8;
  try { const r=await send(paste(Buffer.alloc(20,65))); assert.equal(r.status,413); }
  finally { cfg.maxBytes=oldSize; }
  resetRateLimit(); cfg.rate=1;
  try { assert.equal((await send(paste('x'))).status,200); assert.equal((await send(paste('x'))).status,429); }
  finally { cfg.rate=1000; resetRateLimit(); }
});

test('timeouts, nonzero exits and client aborts kill/reap children and release capacity', async () => {
  const dir=fs.mkdtempSync(path.join(os.tmpdir(),'unwrapper-child-'));
  const oldJava=cfg.java, oldTimeout=cfg.timeout;
  const fake=path.join(dir,'fake-java');
  try {
    fs.writeFileSync(fake,'#!/bin/sh\nexec /bin/sleep 5\n',{mode:0o755}); cfg.java=fake; cfg.timeout=80;
    let start=Date.now(); let r=await send(paste('not important')); assert.equal(r.status,504); assert.ok(Date.now()-start<2000); assert.equal(getActiveCount(),0);
    cfg.java='/usr/bin/false'; cfg.timeout=1000;
    r=await send(paste('not important')); assert.equal(r.status,502); assert.equal(getActiveCount(),0);
    fs.writeFileSync(fake,'#!/bin/sh\nexec /bin/sleep 5\n',{mode:0o755});
    const abort=new AbortController(); const pending=fetch(base+'/api/unwrap',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify(paste('aborted-secret')),signal:abort.signal}).catch(()=>{});
    await new Promise(resolve=>setTimeout(resolve,80)); abort.abort(); await pending;
    for(let i=0;i<30&&getActiveCount()!==0;i++) await new Promise(resolve=>setTimeout(resolve,20));
    assert.equal(getActiveCount(),0);
  } finally { cfg.java=oldJava; cfg.timeout=oldTimeout; fs.rmSync(dir,{recursive:true,force:true}); }
});

test('untrusted X-Forwarded-For cannot evade rate limits; responses are no-store and source is not logged or persisted', async () => {
  const dir=fs.mkdtempSync(path.join(os.tmpdir(),'unwrapper-no-store-')); const old=process.cwd();
  const logs=[]; const log=console.log, error=console.error; console.log=(...a)=>logs.push(a.join(' ')); console.error=(...a)=>logs.push(a.join(' '));
  resetRateLimit(); cfg.rate=1000;
  try {
    process.chdir(dir); const secret='private-source-sentinel';
    const r=await fetch(base+'/api/unwrap',{method:'POST',headers:{'content-type':'application/json','x-forwarded-for':'198.51.100.3'},body:JSON.stringify(paste(secret))});
    assert.equal(r.headers.get('cache-control'),'no-store, no-cache, must-revalidate');
    await r.text(); assert.deepEqual(fs.readdirSync(dir),[]); assert.equal(logs.some(x=>x.includes(secret)),false);
    resetRateLimit(); cfg.rate=1;
    await send(paste('x')); const limited=await fetch(base+'/api/unwrap',{method:'POST',headers:{'content-type':'application/json','x-forwarded-for':'203.0.113.8'},body:JSON.stringify(paste('x'))}); assert.equal(limited.status,429);
  } finally { process.chdir(old); console.log=log; console.error=error; cfg.rate=1000; resetRateLimit(); fs.rmSync(dir,{recursive:true,force:true}); }
});
