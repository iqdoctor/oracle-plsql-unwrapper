'use strict';
const http = require('node:http');
const { spawn } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
const { TextDecoder } = require('node:util');
const ROOT = path.resolve(__dirname, '..');
const cfg = {
  host: process.env.HOST || '127.0.0.1', port: Number(process.env.PORT || 8080), maxBytes: Number(process.env.MAX_REQUEST_BYTES || 2*1024*1024),
  maxOutput: Number(process.env.MAX_OUTPUT_BYTES || 8*1024*1024), timeout: Number(process.env.CHILD_TIMEOUT_MS || 15000),
  concurrency: Number(process.env.MAX_CONCURRENCY || 2), rate: Number(process.env.RATE_LIMIT_PER_MINUTE || 10),
  heap: process.env.JAVA_MAX_HEAP || '128m', trusted: (process.env.TRUSTED_PROXY_IPS || '').split(',').map(x=>x.trim()).filter(Boolean),
  java: process.env.JAVA_BIN || 'java', bridge: path.join(__dirname,'build','ToolBridge.class'),
  cp: process.env.BRIDGE_CLASSPATH || [path.join(__dirname,'build'),path.resolve(ROOT,'..','oracle-plsql-unwrap-spi','target','classes'),path.resolve(ROOT,'..','oracle-plsql-unwrap-v2','target','classes')].join(path.delimiter)
};
const contacts = { email:'ashindarev@gmail.com', linkedin:'https://www.linkedin.com/in/ashindarev/' };
let active=0; const buckets=new Map();
function clientIp(req) { const peer=req.socket.remoteAddress || ''; if(cfg.trusted.includes(peer) || cfg.trusted.includes(peer.replace(/^::ffff:/,''))) { const forwarded=(req.headers['x-forwarded-for']||'').trim(); return forwarded && !forwarded.includes(',') ? forwarded : peer; } return peer; }
function allowRate(ip, now=Date.now()) { let b=buckets.get(ip); if(!b||now-b.at>=60000)b={at:now,n:0}; b.n++; buckets.set(ip,b); if(buckets.size>10000){ for(const [k,v] of buckets)if(now-v.at>60000)buckets.delete(k); while(buckets.size>10000)buckets.delete(buckets.keys().next().value); } return b.n<=cfg.rate; }
function json(res,status,obj) { const body=Buffer.from(JSON.stringify(obj)); res.writeHead(status,{'content-type':'application/json; charset=utf-8','content-length':body.length,'cache-control':'no-store, no-cache, must-revalidate','pragma':'no-cache','x-content-type-options':'nosniff'});res.end(body); }
function readBody(req) { return new Promise((resolve,reject)=>{const chunks=[];let n=0,oversized=false;req.on('data',c=>{if(oversized)return;n+=c.length;if(n>Math.ceil(cfg.maxBytes*4/3)+16384){oversized=true;chunks.length=0;reject(Object.assign(Error('large'),{status:413}));req.resume();return;}chunks.push(c);});req.on('end',()=>{if(oversized)return;try{resolve(JSON.parse(Buffer.concat(chunks).toString('utf8')))}catch{reject(Object.assign(Error('bad'),{status:400}))}});req.on('error',reject);}); }
function runBridge(bytes, charset, signal) { return new Promise((resolve,reject)=>{
  if(signal?.aborted)return reject(Object.assign(Error('aborted'),{status:499}));
  if(active>=cfg.concurrency)return reject(Object.assign(Error('busy'),{status:429})); active++;
  const env={LANG:'C',LC_ALL:'C',PATH:'/usr/bin:/bin'};
  const args=[`-Xmx${cfg.heap}`,'-XX:-HeapDumpOnOutOfMemoryError','-XX:ErrorFile=/dev/null','-cp',cfg.cp,'ToolBridge',charset];
  const child=spawn(cfg.java,args,{shell:false,env,stdio:['pipe','pipe','pipe']}); let out=Buffer.alloc(0),settled=false,pendingError=null,released=false;
  const timer=setTimeout(()=>{pendingError=Object.assign(Error('timeout'),{status:504});child.kill('SIGKILL');},cfg.timeout);
  const abort=()=>{pendingError=Object.assign(Error('aborted'),{status:499});child.kill('SIGKILL');};
  const release=(err,val)=>{if(released)return;released=true;clearTimeout(timer);signal?.removeEventListener('abort',abort);active--;if(settled)return;settled=true;if(err)reject(err);else resolve(val);};
  signal?.addEventListener('abort',abort,{once:true});
  child.stdout.on('data',c=>{if(out.length+c.length>cfg.maxOutput){pendingError=Object.assign(Error('output'),{status:502});child.kill('SIGKILL');}else out=Buffer.concat([out,c]);});
  child.stderr.resume(); child.on('error',()=>{pendingError=Object.assign(Error('bridge'),{status:502});});
  child.on('close',code=>{if(pendingError)return release(pendingError);if(code!==0)return release(Object.assign(Error('bridge'),{status:502})); try{const parts=out.toString('utf8').replace(/\r?\n$/, '').split('\t'); if(parts.length!==4)throw Error(); const dec=x=>Buffer.from(x,'base64').toString('utf8'); release(null,{status:parts[0],type:dec(parts[1]),name:dec(parts[2]),source:dec(parts[3])});}catch{release(Object.assign(Error('protocol'),{status:502}));}});
  child.stdin.on('error',()=>{}); child.stdin.end(bytes);
 }); }
async function handle(req,res,signal){ if(req.method==='GET' && (req.url==='/'||req.url.startsWith('/static/'))){const name=req.url==='/'?'index.html':req.url.slice(8);const file=path.join(ROOT,'frontend',name);if(path.relative(path.join(ROOT,'frontend'),file).startsWith('..'))return json(res,404,{error:'Not found'});try{const data=fs.readFileSync(file);res.writeHead(200,{'content-type':name.endsWith('.js')?'text/javascript; charset=utf-8':name.endsWith('.css')?'text/css; charset=utf-8':'text/html; charset=utf-8','cache-control':'no-store','x-content-type-options':'nosniff','content-security-policy':"default-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'"});return res.end(data)}catch{return json(res,404,{error:'Not found'})}}
 if(req.method!=='POST'||req.url!=='/api/unwrap')return json(res,404,{error:'Not found'});
 if(!allowRate(clientIp(req)))return json(res,429,{status:'RATE_LIMITED',message:'Please wait before submitting another request.'});
 let data;try{data=await readBody(req)}catch(e){return json(res,e.status||400,{error:e.status===413?'Request too large':'Invalid request.'})}
 let bytes; try{if(data.rightsConfirmed!==true) return json(res,400,{error:'Confirm that you have lawful rights to this code before submitting.'}); if(!['paste','file'].includes(data.kind))throw Error(); if(typeof data.payloadBase64!=='string')throw Error(); bytes=Buffer.from(data.payloadBase64, 'base64'); if(bytes.toString('base64')!==data.payloadBase64)throw Error(); if(bytes.length===0)return json(res,400,{error:'Provide one wrapped object.'}); if(bytes.length>cfg.maxBytes)return json(res,413,{error:'Request too large.'}); if(data.kind==='file'&&!/\.(sql|plb)$/i.test(data.fileName||''))return json(res,400,{error:'Upload one .sql or .plb file.'});  }catch{return json(res,400,{error:'Invalid request.'})}
 if(active>=cfg.concurrency)return json(res,429,{status:'BUSY',message:'The service is busy. Try again shortly.'});
 let charset='UTF-8'; try{new TextDecoder('utf-8',{fatal:true}).decode(bytes)}catch(e){if(data.kind==='paste')return json(res,400,{error:'Pasted text must be valid UTF-8.'});charset='ISO-8859-1';}
 try{const r=await runBridge(bytes,charset,signal); if(r.status==='OK')return json(res,200,{status:'RESTORED',format:'V2',objectType:r.type,objectName:r.name,source:r.source}); if(r.status==='V1')return json(res,200,{status:'V1_ROUTED',format:'V1',message:'The public tool recognizes V1 but does not restore it. For qualified recovery inquiries, contact Andrey Shindarev.',contacts}); if(r.status==='MULTI')return json(res,200,{status:'MULTI_OBJECT',message:'This web tool accepts one object per request. Use the CLI for scripts containing multiple objects.',cli:'https://github.com/sftwnd/oracle-plsql-unwrapper',contacts}); if(r.status==='NOT_WRAPPED')return json(res,200,{status:'NOT_WRAPPED',message:'No supported wrapped object was found.'});return json(res,422,{status:'UNRECOGNIZED',message:'The input could not be safely classified.'});}catch(e){return json(res,e.status||502,{status:e.status===504?'TIMEOUT':e.status===429?'BUSY':'PROCESSING_ERROR',message:e.status===504?'Processing timed out.':e.status===429?'The service is busy. Try again shortly.':'Processing failed. No source content was retained.'});}finally{bytes.fill(0);data=null;}
}
const server=http.createServer((req,res)=>{const controller=new AbortController();req.on('aborted',()=>controller.abort());res.on('close',()=>{if(!res.writableEnded)controller.abort();});handle(req,res,controller.signal).catch(()=>{if(!res.headersSent)json(res,500,{error:'Request failed.'});else res.destroy();});});
if(require.main===module)server.listen(cfg.port,cfg.host,()=>{});
module.exports={server,handle,runBridge,clientIp,allowRate,cfg,resetRateLimit:()=>buckets.clear(),getActiveCount:()=>active};
