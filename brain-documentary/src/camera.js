import * as THREE from 'three'; import { smooth } from './rng.js';
export function cameraPose(mode, lt, t){
  const a = t*0.05;
  const poses={
    orbit:[Math.sin(a)*4.2, 1.1+Math.sin(t*0.11)*0.4, Math.cos(a)*4.2],
    plasticity:[Math.sin(a*1.4)*3.0,0.7,Math.cos(a*1.4)*3.0],
    eye:[-1.8,0.5,3.4], hippocampus:[2.6,-0.6,2.8], energy:[0,1.6,4.6], sleep:[0,0.8,5.2],
    amygdala:[2.2,-0.4,3.0], attention:[0,2.4,3.6], habit:[Math.sin(a*2)*2.6,0.4,3.2], stress:[2.8,0.8,2.6], network:[0,0.4,5.6],
    pullback:[0,0.8, 4.5+lt*7],
  };
  const p=poses[mode]||poses.orbit; const dolly = mode==='pullback'?0: Math.sin(lt*Math.PI)*0.5;
  return new THREE.Vector3(p[0],p[1],p[2]+ (mode==='pullback'?0:dolly*0.3));
}
