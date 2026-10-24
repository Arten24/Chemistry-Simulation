import * as THREE from 'three';
import { mulberry32 } from './rng.js';
export function createNeurons(count=420){
  const rand=mulberry32(42); const pts=[];
  for(let i=0;i<count;i++){ // inside brain-ish ellipsoid
    const u=rand()*Math.PI*2, w=Math.acos(2*rand()-1), r=Math.cbrt(rand());
    pts.push(new THREE.Vector3(Math.sin(w)*Math.cos(u)*r*0.95, Math.cos(w)*r*0.72, Math.sin(w)*Math.sin(u)*r*1.0));
  }
  const inst=new THREE.InstancedMesh(new THREE.SphereGeometry(0.016,8,6), new THREE.MeshBasicMaterial({color:0x7fd4ff}), count);
  const d=new THREE.Object3D(); pts.forEach((p,i)=>{d.position.copy(p); d.updateMatrix(); inst.setMatrixAt(i,d.matrix);});
  const linePos=[]; const pairs=[];
  pts.forEach((p,i)=>{ const near=pts.map((q,j)=>[p.distanceTo(q),j]).filter(x=>x[1]!==i).sort((a,b)=>a[0]-b[0]).slice(0,2); near.forEach(([dist,j])=>{ if(dist<0.55){ linePos.push(p.x,p.y,p.z,pts[j].x,pts[j].y,pts[j].z); pairs.push([i,j]); } }); });
  const lg=new THREE.BufferGeometry(); lg.setAttribute('position', new THREE.Float32BufferAttribute(linePos,3));
  const lines=new THREE.LineSegments(lg, new THREE.LineBasicMaterial({color:0x2f8fff, transparent:true, opacity:0.35, blending:THREE.AdditiveBlending}));
  // pulses: points travelling along connections, position computed from uTime in onBeforeRender via CPU-free shader trick using pair index attr
  const pg=new THREE.BufferGeometry(); const pp=[], pa=[];
  pairs.forEach(([a,b],i)=>{ for(let k=0;k<2;k++){ pp.push(pts[a].x,pts[a].y,pts[a].z, pts[b].x,pts[b].y,pts[b].z); pa.push(i*0.37+k*0.5);} });
  // store endpoints as attributes for shader
  const start=[], end=[], phase=[];
  pairs.forEach(([a,b],i)=>{ for(let k=0;k<2;k++){ start.push(pts[a].x,pts[a].y,pts[a].z); end.push(pts[b].x,pts[b].y,pts[b].z); phase.push((i*0.37+k*0.5)%1);} });
  pg.setAttribute('position', new THREE.Float32BufferAttribute(start,3));
  pg.setAttribute('aEnd', new THREE.Float32BufferAttribute(end,3));
  pg.setAttribute('aPhase', new THREE.Float32BufferAttribute(phase,1));
  const pm=new THREE.ShaderMaterial({transparent:true, depthWrite:false, blending:THREE.AdditiveBlending, uniforms:{uTime:{value:0}},
    vertexShader:`attribute vec3 aEnd; attribute float aPhase; uniform float uTime; varying float vA; void main(){ float f=fract(uTime*0.25+aPhase); vec3 p=mix(position,aEnd,f); vA=sin(f*3.14159); vec4 mv=modelViewMatrix*vec4(p,1.0); gl_PointSize=(3.0+vA*6.0)*(140.0/-mv.z); gl_Position=projectionMatrix*mv; }`,
    fragmentShader:`varying float vA; void main(){ vec2 c=gl_PointCoord-0.5; if(dot(c,c)>0.25) discard; gl_FragColor=vec4(1.0,0.8,0.35,vA); }`});
  const pulses=new THREE.Points(pg,pm); pulses.name='pulses';
  const group=new THREE.Group(); group.add(inst,lines,pulses); group.userData={pulses,count};
  return group;
}
