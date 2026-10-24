import * as THREE from 'three';
import { mulberry32 } from './rng.js';
export function createParticles(count=2600){
  const rand=mulberry32(7); const pos=new Float32Array(count*3), phase=new Float32Array(count);
  for(let i=0;i<count;i++){ const r=2.2+rand()*5, th=rand()*Math.PI*2, ph=Math.acos(2*rand()-1);
    pos[i*3]=r*Math.sin(ph)*Math.cos(th); pos[i*3+1]=r*Math.cos(ph)*0.7; pos[i*3+2]=r*Math.sin(ph)*Math.sin(th); phase[i]=rand(); }
  const g=new THREE.BufferGeometry(); g.setAttribute('position', new THREE.BufferAttribute(pos,3)); g.setAttribute('aPhase', new THREE.BufferAttribute(phase,1));
  const m=new THREE.ShaderMaterial({transparent:true, depthWrite:false, blending:THREE.AdditiveBlending, uniforms:{uTime:{value:0},uColor:{value:new THREE.Color(0x3d9bff)}},
   vertexShader:`attribute float aPhase; uniform float uTime; varying float vP; void main(){ vP=aPhase; vec3 p=position; p.y+=sin(uTime*0.3+aPhase*40.0)*0.15; vec4 mv=modelViewMatrix*vec4(p,1.0); gl_PointSize=(1.5+aPhase*3.0)*(120.0/-mv.z); gl_Position=projectionMatrix*mv; }`,
   fragmentShader:`uniform vec3 uColor; varying float vP; void main(){ vec2 c=gl_PointCoord-0.5; if(dot(c,c)>0.25) discard; gl_FragColor=vec4(uColor,0.25+vP*0.6); }`});
  const pts=new THREE.Points(g,m); pts.name='particles'; return pts;
}
export function createEye(){
  const g=new THREE.Group();
  const eye=new THREE.Mesh(new THREE.SphereGeometry(0.42,40,28), new THREE.MeshPhysicalMaterial({color:0xf2f4f6,roughness:0.15,clearcoat:1}));
  const iris=new THREE.Mesh(new THREE.CircleGeometry(0.19,32), new THREE.MeshBasicMaterial({color:0x2f7fd0})); iris.position.set(0,0,0.415);
  const pupil=new THREE.Mesh(new THREE.CircleGeometry(0.09,32), new THREE.MeshBasicMaterial({color:0x05070a})); pupil.position.set(0,0,0.42);
  g.add(eye,iris,pupil); g.visible=false; g.name='eye'; return g;
}
export function createSilhouette(){
  // simple elegant sleeping-figure curve (line) used in the sleep chapter
  const pts=[]; for(let i=0;i<=60;i++){ const x=-1.6+3.2*i/60; pts.push(new THREE.Vector3(x, -0.9+Math.sin(i/60*Math.PI)*0.25 + Math.sin(i/60*Math.PI*3)*0.05, -0.4)); }
  const line=new THREE.Line(new THREE.BufferGeometry().setFromPoints(pts), new THREE.LineBasicMaterial({color:0x6fb7ff,transparent:true,opacity:0.8}));
  line.visible=false; line.name='silhouette'; return line;
}
