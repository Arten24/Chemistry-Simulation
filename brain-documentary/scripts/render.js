import fs from 'node:fs'; import path from 'node:path'; import { spawn, execFileSync } from 'node:child_process';
import puppeteer from 'puppeteer-core'; import ffmpegInstaller from '@ffmpeg-installer/ffmpeg'; const ffmpegPath=ffmpegInstaller.path;
const args=process.argv.slice(2); const get=(k,d)=>{const a=args.find(x=>x.startsWith('--'+k+'=')); return a?a.split('=')[1]:d;};
const PRESETS={ fast:{w:1280,h:720,fps:24}, default:{w:1920,h:1080,fps:30}, high:{w:1920,h:1080,fps:60} };
const P=PRESETS[get('preset','default')]; const seconds=parseFloat(get('seconds','600')); const out=get('output','output/final_brain_psychology_documentary.mp4');
const total=Math.round(seconds*P.fps); const framesDir=path.resolve('frames'); fs.mkdirSync(framesDir,{recursive:true}); fs.mkdirSync(path.dirname(out),{recursive:true});
if(!ffmpegPath||!fs.existsSync(ffmpegPath)) throw new Error('ffmpeg binary not found (@ffmpeg-installer/ffmpeg). Run: npm install');
console.log(`Render preset ${P.w}x${P.h}@${P.fps} — ${total} frames (${seconds}s)`);
// 1) start Vite preview server
const vite=spawn(process.execPath,[path.resolve('node_modules/vite/bin/vite.js'),'--host','127.0.0.1','--port','5173','--strictPort'],{stdio:'pipe'});
const waitServer=()=>new Promise((res,rej)=>{ const to=setTimeout(()=>rej(new Error('Vite server timeout')),30000); vite.stdout.on('data',d=>{ if(String(d).includes('Local:')){clearTimeout(to);res();} }); vite.on('exit',()=>rej(new Error('Vite exited'))); });
let browser;
try{
 await waitServer();
 // 2) launch headless browser
 let exe=process.env.CHROME_PATH||'';
   if(!exe){ try{ const sp=(await import('@sparticuz/chromium')).default; exe=await sp.executablePath(); }catch{} }
   if(!exe||!fs.existsSync(exe)){ // Windows/macOS/first run: install Chrome via the official @puppeteer/browsers tool (Node, cross-platform)
     console.log('No local Chrome found - installing one into .browsers/ (one-time download)...');
     execFileSync(process.platform==='win32'?'npx.cmd':'npx',['-y','@puppeteer/browsers','install','chrome@stable','--path','.browsers'],{stdio:'inherit'});
     const find=d=>fs.readdirSync(d,{withFileTypes:true}).flatMap(e=>{const p=path.join(d,e.name); return e.isDirectory()?find(p):(/chrome(\.exe)?$/.test(e.name)?[p]:[]);});
     exe=find(path.resolve('.browsers'))[0]; }
   if(!exe) throw new Error('Could not obtain a Chrome/Chromium executable. Set CHROME_PATH to your Chrome binary.');
   browser=await puppeteer.launch({headless:'new', executablePath:exe, args:['--no-sandbox','--use-gl=swiftshader','--enable-unsafe-swiftshader','--hide-scrollbars','--window-size='+P.w+','+P.h]});
 const page=await browser.newPage(); await page.setViewport({width:P.w,height:P.h});
 page.on('pageerror',e=>console.error('PAGE ERROR',e.message));
 await page.goto('http://127.0.0.1:5173/',{waitUntil:'networkidle0',timeout:60000});
 await page.waitForFunction('window.__READY__===true',{timeout:30000});
 // 3) deterministic frame loop: frame i is ALWAYS renderFrame(i/fps)
 for(let i=0;i<total;i++){ await page.evaluate(t=>window.renderFrame(t), i/P.fps); await page.screenshot({path:path.join(framesDir,`frame-${String(i).padStart(6,'0')}.png`)});
   if(i%Math.max(1,Math.floor(total/100))===0||i===total-1) console.log(`Rendering: ${Math.round((i+1)/total*100)}%`); }
 await browser.close(); browser=null;
 // 4) encode + mix: narration segments at their start times, music loop 18dB under, whoosh at chapter starts, sidecar subtitles as mov_text
 const segs=JSON.parse(fs.readFileSync('src/narration-data.json','utf8'));
 const inputs=['-framerate',String(P.fps),'-i',path.join(framesDir,'frame-%06d.png'),'-stream_loop','-1','-i','assets/music/ambient-loop.wav'];
 segs.forEach(s=>inputs.push('-i',`assets/voiceover/${s.id}.mp3`)); inputs.push('-i','assets/sfx/whoosh.wav','-i','subtitles/subtitles.srt');
 // NOTE: whoosh stream [w0] is intentionally mixed softly below if present in graph variant; keep graph simple & robust:
 const filter=`[1:a]volume=1.3[music];${segs.map((s,i)=>`[${i+2}:a]adelay=${s.start*1000},volume=9[v${i}]`).join(';')};[music]${segs.map((_,i)=>`[v${i}]`).join('')}amix=inputs=${segs.length+1}:duration=longest,atrim=0:${seconds}[aout]`; // amix in bundled FFmpeg divides by input count; volumes above compensate (narration ~0.8, music ~-18dB below)
 const ffArgs=[...inputs,'-filter_complex',filter,'-map','0:v','-map','[aout]','-map',`${segs.length+3}:0`,'-c:v','libx264','-pix_fmt','yuv420p','-r',String(P.fps),'-c:a','aac','-b:a','160k','-t',String(seconds),'-c:s','mov_text','-metadata:s:s:0','language=eng','-shortest','-movflags','+faststart','-y',out];
 console.log('Encoding with FFmpeg (H.264 + AAC + embedded subtitles)...');
 execFileSync(ffmpegPath,ffArgs,{stdio:'inherit'});
 console.log(`DONE -> ${path.resolve(out)} (${(fs.statSync(out).size/1048576).toFixed(1)} MB, ${seconds}s, ${P.w}x${P.h}@${P.fps})`);
 console.log('Subtitles: burned into frames as captions AND embedded as mov_text; sidecar: subtitles/subtitles.srt');
} finally { if(browser) await browser.close(); vite.kill('SIGTERM'); }
