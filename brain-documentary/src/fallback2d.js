import { mulberry32 } from './rng.js';
import { chapterAt } from './timeline.js';
// Canvas-2D fallback renderer: same deterministic timeline, used automatically if WebGL
// is unavailable, the GPU context is lost, or the first WebGL frame comes out blank/white.
const rand = mulberry32(123);
const nodes = Array.from({length:110},()=>{ const a=rand()*Math.PI*2, r=Math.sqrt(rand());
  return {x:Math.cos(a)*r*0.92, y:Math.sin(a)*r*0.68, p:rand()*Math.PI*2}; });
const links=[]; nodes.forEach((n,i)=>{ nodes.forEach((m,j)=>{ if(j>i){ const d=Math.hypot(n.x-m.x,n.y-m.y); if(d<0.34) links.push([i,j,rand()]); }}); });
const stars = Array.from({length:220},()=>({x:rand()*2-1,y:rand()*2-1,s:rand()*1.8+0.4,p:rand()*7}));
const MODE_COLORS={intro:'#38b6ff',neuroplasticity:'#3dff9e',perception:'#7fd4ff',memory:'#c792ff',energy:'#ffb340',sleep:'#5f7dff',emotions:'#ff4d8d',attention:'#ffe95e',habits:'#3dffc8',stress:'#ff6a4d',networks:'#38e0ff',outro:'#ffb340'};
export function drawFallback2D(ctx,W,H,t){
  const ch=chapterAt(t), col=MODE_COLORS[ch.id]||'#38b6ff';
  const cx=W/2, cy=H*0.44, R=Math.min(W,H)*0.30*(ch.id==='outro'?Math.max(0.55,1-(t-580)/20*0.4):1);
  const bg=ctx.createRadialGradient(cx,cy,R*0.2,cx,cy,Math.max(W,H)*0.75);
  bg.addColorStop(0,'#071228'); bg.addColorStop(0.55,'#030716'); bg.addColorStop(1,'#010204');
  ctx.fillStyle=bg; ctx.fillRect(0,0,W,H);
  for(const s of stars){ ctx.globalAlpha=0.35+0.3*Math.sin(t*0.7+s.p); ctx.fillStyle='#9fd0ff'; ctx.fillRect(cx+s.x*W*0.48, cy+s.y*H*0.48, s.s, s.s); }
  ctx.globalAlpha=1;
  // brain glow + body (folded silhouette made of layered arcs, not a plain circle)
  const glow=ctx.createRadialGradient(cx,cy,R*0.1,cx,cy,R*1.7); glow.addColorStop(0,col+'55'); glow.addColorStop(1,'transparent');
  ctx.fillStyle=glow; ctx.fillRect(cx-R*1.8,cy-R*1.8,R*3.6,R*3.6);
  ctx.save(); ctx.translate(cx,cy); ctx.rotate(Math.sin(t*0.07)*0.05);
  const body=ctx.createRadialGradient(-R*0.3,-R*0.3,R*0.1,0,0,R*1.15); body.addColorStop(0,'#e8a0a8'); body.addColorStop(0.6,'#b95f6e'); body.addColorStop(1,'#5e2a38');
  ctx.fillStyle=body; ctx.beginPath(); ctx.ellipse(0,0,R,R*0.74,0,0,Math.PI*2); ctx.fill();
  ctx.strokeStyle='rgba(70,20,35,0.5)'; ctx.lineWidth=Math.max(1.5,R*0.012);
  for(let i=0;i<26;i++){ const yy=-R*0.62+i*R*0.05, ph=i*1.7; ctx.beginPath();
    for(let x=-R*0.95;x<=R*0.95;x+=R/18){ const edge=Math.sqrt(Math.max(0,1-(x/R)**2))*R*0.72; const y=yy*0.7+Math.sin(x*0.02/R*60+ph+t*0.15)*R*0.03; if(Math.abs(y)<edge) (x<=-R*0.9?ctx.moveTo(x,y):ctx.lineTo(x,y)); } ctx.stroke(); }
  ctx.strokeStyle='rgba(30,10,20,0.65)'; ctx.lineWidth=Math.max(2,R*0.02); ctx.beginPath(); ctx.moveTo(0,-R*0.72); ctx.bezierCurveTo(R*0.06,-R*0.2,-R*0.06,R*0.2,0,R*0.72); ctx.stroke();
  // neural links + pulses
  for(const [i,j,seed] of links){ const a=nodes[i],b=nodes[j]; ctx.strokeStyle=col; ctx.globalAlpha=0.14+0.13*Math.sin(t*1.5+seed*9); ctx.lineWidth=1;
    ctx.beginPath(); ctx.moveTo(a.x*R,a.y*R); ctx.lineTo(b.x*R,b.y*R); ctx.stroke();
    const f=(t*0.25+seed)%1, px=a.x+(b.x-a.x)*f, py=a.y+(b.y-a.y)*f; ctx.globalAlpha=Math.sin(f*Math.PI)*0.95; ctx.fillStyle='#ffd76a';
    ctx.beginPath(); ctx.arc(px*R,py*R,Math.max(1.6,R*0.012),0,Math.PI*2); ctx.fill(); }
  ctx.globalAlpha=1;
  for(const n of nodes){ const tw=0.5+0.5*Math.sin(t*2+n.p); ctx.fillStyle=tw>0.75?'#ffe9a8':'#bfe6ff'; ctx.beginPath(); ctx.arc(n.x*R,n.y*R,Math.max(1.2,R*0.008+tw*R*0.006),0,Math.PI*2); ctx.fill(); }
  ctx.restore();
  if(ch.id==='perception'){ ctx.strokeStyle='#dff4ff'; ctx.lineWidth=3; ctx.beginPath(); ctx.ellipse(cx-R*1.55,cy,R*0.34,R*0.2,0,0,Math.PI*2); ctx.stroke();
    ctx.fillStyle='#2f7fd0'; ctx.beginPath(); ctx.arc(cx-R*1.55,cy,R*0.1,0,Math.PI*2); ctx.fill(); ctx.fillStyle='#05070a'; ctx.beginPath(); ctx.arc(cx-R*1.55,cy,R*0.05,0,Math.PI*2); ctx.fill(); }
  if(ch.id==='sleep'){ ctx.strokeStyle='#6fb7ff'; ctx.globalAlpha=0.8; ctx.lineWidth=3; ctx.beginPath();
    for(let x=0;x<=W;x+=10){ const y=H*0.78+Math.sin(x*0.01+t*0.8)*H*0.02; x===0?ctx.moveTo(x,y):ctx.lineTo(x,y); } ctx.stroke(); ctx.globalAlpha=1; }
  return {chapter:ch.id, fallback:true};
}
