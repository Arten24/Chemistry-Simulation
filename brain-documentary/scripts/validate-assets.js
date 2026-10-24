import fs from 'node:fs';
const need=['index.html','src/main.js','src/scene.js','src/timeline.js','assets/music/ambient-loop.wav','assets/sfx/pulse.wav','assets/sfx/whoosh.wav','assets/sfx/chime.wav','subtitles/subtitles.srt',...Array.from({length:10},(_,i)=>`assets/voiceover/seg${String(i+1).padStart(2,'0')}.mp3`),...['intro','neuroplasticity','perception','memory','energy','sleep','emotions','attention','habits','stress','networks','outro'].map(c=>`src/chapters/${c}.js`)];
let bad=0; for(const f of need){ const ok=fs.existsSync(f)&&fs.statSync(f).size>0; console.log(ok?'OK  ':'MISS',f); if(!ok)bad++; }
import { CHAPTERS, DURATION } from '../src/timeline.js';
if(DURATION!==600||CHAPTERS.length!==12||CHAPTERS[11].end!==600){ console.error('Timeline must be 12 chapters / 600s'); bad++; }
console.log(bad?`FAIL ${bad} problems`:'All assets valid. Timeline: 12 chapters, 600s, 18000 frames @30fps.');
process.exit(bad?1:0);
