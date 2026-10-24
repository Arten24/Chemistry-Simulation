import fs from 'node:fs'; import path from 'node:path';
const data=JSON.parse(fs.readFileSync(path.resolve('src/narration-data.json'),'utf8'));

const fmt=t=>{const h=Math.floor(t/3600),m=Math.floor(t%3600/60),s=Math.floor(t%60),ms=Math.floor((t%1)*1000);return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')},${String(ms).padStart(3,'0')}`;};
let n=0,out=[];
for(const seg of data){ const a=seg.start, b=seg.start+(seg.duration||55); const parts=seg.text.match(/[^.!?]+[.!?]+/g)||[seg.text]; const total=parts.reduce((x,p)=>x+p.length,0); let cur=a;
 for(const p of parts){ const d=(b-a)*0.94*(p.length/total); out.push(`${++n}\n${fmt(cur)} --> ${fmt(cur+d)}\n${p.trim()}\n`); cur+=d; } }
fs.mkdirSync('subtitles',{recursive:true}); fs.writeFileSync(path.join('subtitles','subtitles.srt'), out.join('\n'));
console.log(`Wrote subtitles/subtitles.srt with ${n} cues covering 0-600s`);
