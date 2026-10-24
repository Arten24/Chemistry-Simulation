import fs from 'node:fs'; import path from 'node:path'; import { execFileSync } from 'node:child_process';
// Narration: real pre-generated voiceover ships in assets/voiceover/seg01..seg10.mp3 and is NEVER overwritten.
// If a segment is missing, try a local system TTS (Windows SAPI / macOS say / Linux espeak), else fail loudly.
const data=JSON.parse(fs.readFileSync('src/narration-data.json','utf8'));
for(const s of data){ const f=path.join('assets','voiceover',s.id+'.mp3');
  if(fs.existsSync(f)&&fs.statSync(f).size>10000){ console.log('narration OK',f); continue; }
  console.warn('Missing narration, attempting system TTS for',s.id);
  try{ if(process.platform==='win32') execFileSync('powershell',['-NoProfile','-Command',`Add-Type -AssemblyName System.Speech; $v=New-Object System.Speech.Synthesis.SpeechSynthesizer; $v.SetOutputToWaveFile('${s.id}.wav'); $v.Speak('${s.text.replaceAll("'","''")}')`]);
       else if(process.platform==='darwin') execFileSync('say',['-o',s.id+'.aiff',s.text]); else execFileSync('espeak',['-w',s.id+'.wav',s.text]); }
  catch{ throw new Error(`No narration for ${s.id} and no local TTS found. Restore assets/voiceover/${s.id}.mp3`); } }
// Procedural music (60s seamless ambient loop) + SFX as WAV PCM - fully licensed (generated here).
function wav(file, seconds, fn, sr=22050){ if(fs.existsSync(file)) {console.log('exists',file); return;}
  const n=Math.floor(seconds*sr), buf=Buffer.alloc(44+n*2); buf.write('RIFF',0); buf.writeUInt32LE(36+n*2,4); buf.write('WAVEfmt ',8); buf.writeUInt32LE(16,16); buf.writeUInt16LE(1,20); buf.writeUInt16LE(1,22); buf.writeUInt32LE(sr,24); buf.writeUInt32LE(sr*2,28); buf.writeUInt16LE(2,32); buf.writeUInt16LE(16,34); buf.write('data',36); buf.writeUInt32LE(n*2,40);
  for(let i=0;i<n;i++){ const t=i/sr; let v=fn(t,seconds); v=Math.max(-1,Math.min(1,v)); buf.writeInt16LE(Math.round(v*32767*0.5),44+i*2); }
  fs.mkdirSync(path.dirname(file),{recursive:true}); fs.writeFileSync(file,buf); console.log('generated',file); }
wav(path.join('assets','music','ambient-loop.wav'),60,(t,d)=>{ const x=t/d*Math.PI*2; const env=0.5+0.5*Math.cos(x); // seamless loop crossfade-ish
  return (Math.sin(2*Math.PI*110*t)+0.6*Math.sin(2*Math.PI*165*t)+0.4*Math.sin(2*Math.PI*220*t)+0.25*Math.sin(2*Math.PI*330*t+Math.sin(x)*2))*0.18*(0.7+0.3*Math.cos(x)); });
wav(path.join('assets','sfx','pulse.wav'),0.7,t=>Math.sin(2*Math.PI*(880-600*t)*t)*Math.exp(-t*6)*0.8);
wav(path.join('assets','sfx','whoosh.wav'),1.2,t=> (Math.random()*2-1)*Math.sin(Math.PI*t/1.2)*0.25*Math.sin(2*Math.PI*(200+800*t)*t)*3);
wav(path.join('assets','sfx','chime.wav'),1.5,t=>(Math.sin(2*Math.PI*660*t)+0.5*Math.sin(2*Math.PI*990*t))*Math.exp(-t*2.5)*0.6);
