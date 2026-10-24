import { DocumentaryScene } from './scene.js';
import { CHAPTERS, DURATION, chapterAt } from './timeline.js';
import { SEGMENTS } from './narration.js';
import { showChapter, captionFor } from './typography.js';
// Build timed caption cues: each narration segment's sentences share its chapter window proportionally to length.
const cues=[]; { let prevEnd=0;
  for(const s of SEGMENTS){ const a=s.start, b=s.start+(s.duration||55); const parts=s.text.match(/[^.!?]+[.!?]+/g)||[s.text]; const total=parts.reduce((n,p)=>n+p.length,0); let cur=a;
    for(const p of parts){ const dur=(b-a)*0.94*(p.length/total); cues.push({start:cur,end:cur+dur,text:p.trim()}); cur+=dur; } }
}
const canvas=document.getElementById('gl'); const doc=new DocumentaryScene(canvas);
const els={box:document.getElementById('chapter'),kicker:document.getElementById('kicker'),title:document.getElementById('title'),cap:document.getElementById('caption'),time:document.getElementById('timecode'),bar:document.getElementById('bar')};
function fit(){ const w=innerWidth, h=innerHeight; doc.resize(w,h); } addEventListener('resize',fit); fit();
let lastChapter='';
function renderFrame(t){
  t=Math.max(0,Math.min(DURATION-0.001,t));
  const info=doc.renderFrame(t); const ch=chapterAt(t);
  showChapter(els,ch,(t-ch.start)/(ch.end-ch.start));
  const cue=captionFor(cues,t); els.cap.textContent=cue?cue.text:''; els.cap.style.opacity=cue?'1':'0';
  const mm=String(Math.floor(t/60)).padStart(2,'0'), ss=String(Math.floor(t%60)).padStart(2,'0');
  els.time.textContent=`${mm}:${ss} / 10:00 — ${ch.kicker}`; els.bar.style.width=`${t/DURATION*100}%`;
  if(ch.id!==lastChapter){ lastChapter=ch.id; document.querySelectorAll('.seg-btn').forEach(b=>b.classList.toggle('active',b.dataset.id===ch.id)); }
  return info;
}
window.renderFrame=renderFrame; window.__DURATION__=DURATION; window.__CHAPTERS__=CHAPTERS; window.__READY__=true;
// Preview playback (real-time clock is used ONLY for preview; rendering calls renderFrame(frame/fps) deterministically)
const slider=document.getElementById('seek'); let playing=false, startStamp=0, startT=0;
document.getElementById('play').onclick=()=>{ playing=!playing; document.getElementById('play').textContent=playing?'❚❚ Pause':'▶ Play'; startStamp=performance.now(); startT=parseFloat(slider.value); };
slider.oninput=()=>{ renderFrame(parseFloat(slider.value)); };
const list=document.getElementById('chapters');
CHAPTERS.forEach(c=>{ const b=document.createElement('button'); b.className='seg-btn'; b.dataset.id=c.id; b.textContent=`${String(Math.floor(c.start/60)).padStart(2,'0')}:${String(c.start%60).padStart(2,'0')} ${c.kicker}`; b.onclick=()=>{ slider.value=c.start; renderFrame(c.start); }; list.appendChild(b); });
for(const s of SEGMENTS){ const a=document.createElement('audio'); a.src=`assets/voiceover/${s.id}.mp3`; a.preload='auto'; a.dataset.seg=s.id; document.getElementById('audio-bank').appendChild(a); }
function loop(now){ if(playing){ const t=startT+(now-startStamp)/1000; if(t>=DURATION){playing=false;} else { slider.value=t; renderFrame(t); syncAudio(t);} } requestAnimationFrame(loop); }
function syncAudio(t){ document.querySelectorAll('#audio-bank audio').forEach(a=>{ const s=SEGMENTS.find(x=>x.id===a.dataset.seg); const local=t-s.start; const next=SEGMENTS[SEGMENTS.indexOf(s)+1]; const end=next?next.start:DURATION;
  if(local>=0 && t<end){ if(a.paused) a.play().catch(()=>{}); if(Math.abs(a.currentTime-local)>1.5) a.currentTime=Math.max(0,local); } else if(!a.paused) a.pause(); }); }
renderFrame(0); requestAnimationFrame(loop);
