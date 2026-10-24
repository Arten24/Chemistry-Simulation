import * as THREE from 'three';
import { mulberry32 } from './rng.js';
import { MAT, REGION_MATS } from './materials.js';
// Procedural cortical-fold brain: displaced, elongated hemispheres (no external model required).
// Gyri/sulci from layered deterministic trigonometric noise - visible folds, not plain spheres.
function hemisphere(side, seed){
  const geo = new THREE.SphereGeometry(1, 110, 78);
  const pos = geo.attributes.position; const rand = mulberry32(seed); const ph=[...Array(8)].map(()=>rand()*Math.PI*2);
  const v=new THREE.Vector3();
  for(let i=0;i<pos.count;i++){
    v.fromBufferAttribute(pos,i);
    const n = Math.sin(v.x*11+ph[0])*Math.sin(v.y*13+ph[1])*0.5 + Math.sin(v.z*17+ph[2])*Math.sin(v.x*7+ph[3])*0.35 + Math.sin((v.y+v.z)*23+ph[4])*0.18;
    const fold = 1 + n*0.075 + Math.pow(Math.abs(Math.sin(v.x*6+v.y*4+ph[5])),3)*0.03;
    v.multiplyScalar(fold);
    v.x = v.x*0.72 + side*0.34; v.y = v.y*0.82 - Math.max(0,-v.y)*0.12; v.z = v.z*1.08; // elongate front-back, flatten base
    if(v.y<-0.55) v.y=-0.55-(v.y+0.55)*0.3;
    pos.setXYZ(i,v.x,v.y,v.z);
  }
  geo.computeVertexNormals();
  const mesh=new THREE.Mesh(geo, MAT.cortex); mesh.name='hemisphere'+side; return mesh;
}
export function createBrain(){
  const g=new THREE.Group(); g.name='brain';
  g.add(hemisphere(-1,1234), hemisphere(1,9876));
  // cerebellum + brain stem (also fold-displaced, squashed)
  const cere = hemisphere(0,555); cere.scale.set(0.55,0.32,0.4); cere.position.set(0,-0.62,-0.78); cere.rotation.x=0.4; g.add(cere);
  const stem=new THREE.Mesh(new THREE.CylinderGeometry(0.13,0.2,0.7,20), MAT.cortex); stem.position.set(0,-0.85,-0.25); g.add(stem);
  // anatomical region highlights (positions approximate, labelled in typography overlay)
  const regions={ hippocampus:[0.38,-0.28,0.05], amygdala:[0.3,-0.22,0.42], prefrontal:[0,-0.02,1.02], visual:[0,-0.05,-1.05], basal:[0,-0.1,0.1], cortex:[0,0.85,0] };
  g.userData.regions={};
  for(const [k,p] of Object.entries(regions)){ const m=new THREE.Mesh(new THREE.SphereGeometry(k==='cortex'?0.5:0.17,24,18), REGION_MATS[k]); m.position.set(...p); m.name='region-'+k; g.add(m); g.userData.regions[k]=m; }
  return g;
}
export function setRegionHighlight(brain, name, intensity){
  for(const [k,m] of Object.entries(brain.userData.regions)) m.material.opacity = (k===name? 0.38+0.3*intensity : 0);
}
