import * as THREE from 'three';
import { EffectComposer } from 'three/addons/postprocessing/EffectComposer.js';
import { RenderPass } from 'three/addons/postprocessing/RenderPass.js';
import { UnrealBloomPass } from 'three/addons/postprocessing/UnrealBloomPass.js';
import { createBrain, setRegionHighlight } from './brain.js';
import { createNeurons } from './neurons.js';
import { createParticles, createEye, createSilhouette } from './particles.js';
import { createLights } from './lighting.js';
import { cameraPose } from './camera.js';
import CHAPTER_CFG from './chapters/index.js';
import { chapterAt, localT } from './timeline.js';
import { fadeAt } from './transitions.js';
import { REGION_MATS } from './materials.js';

export class DocumentaryScene {
  constructor(canvas){
    this.renderer=new THREE.WebGLRenderer({canvas, antialias:true, preserveDrawingBuffer:true});
    this.renderer.setPixelRatio(1);
    this.scene=new THREE.Scene(); this.scene.background=new THREE.Color(0x020409);
    this.scene.fog=new THREE.FogExp2(0x020409, 0.055);
    this.camera=new THREE.PerspectiveCamera(42, 16/9, 0.1, 100);
    this.lights=createLights(this.scene);
    this.brain=createBrain(); this.scene.add(this.brain);
    this.neurons=createNeurons(); this.scene.add(this.neurons);
    this.particles=createParticles(); this.scene.add(this.particles);
    this.eye=createEye(); this.eye.position.set(-2.4,0.1,0); this.scene.add(this.eye);
    this.silhouette=createSilhouette(); this.scene.add(this.silhouette);
    this.composer=new EffectComposer(this.renderer);
    this.composer.addPass(new RenderPass(this.scene,this.camera));
    this.bloom=new UnrealBloomPass(new THREE.Vector2(1920,1080), 0.85, 0.7, 0.72);
    this.composer.addPass(this.bloom);
  }
  resize(w,h){ this.renderer.setSize(w,h,false); this.composer.setSize(w,h); this.camera.aspect=w/h; this.camera.updateProjectionMatrix(); }
  // DETERMINISTIC: visual state is a pure function of timeInSeconds.
  renderFrame(t){
    const ch=chapterAt(t), lt=localT(t), cfg=CHAPTER_CFG[ch.id];
    const fade=fadeAt(lt);
    this.brain.rotation.y = t*0.07; this.brain.rotation.x = Math.sin(t*0.09)*0.12;
    this.neurons.rotation.y = -t*0.045;
    this.neurons.userData.pulses.material.uniforms.uTime.value = t * (ch.id==='sleep'?0.35: ch.id==='energy'?1.8: 1);
    this.particles.material.uniforms.uTime.value=t;
    this.particles.material.uniforms.uColor.value.set(ch.id==='stress'?0xff5a4d: ch.id==='sleep'?0x4d6fff: ch.id==='energy'?0xffa63d:0x3d9bff);
    this.particles.rotation.y=t*0.01;
    this.eye.visible = ch.id==='perception'; if(this.eye.visible){ this.eye.lookAt(this.camera.position); this.eye.rotation.z=Math.sin(t*0.7)*0.08; }
    this.silhouette.visible = ch.id==='sleep';
    setRegionHighlight(this.brain, cfg.region, 0.5+0.5*Math.sin(t*2.2));
    // emotions chapter alternates amygdala / prefrontal emphasis
    if(ch.id==='emotions'){ const s=Math.sin(t*0.8)>0?'amygdala':'prefrontal'; setRegionHighlight(this.brain,s,0.9); }
    this.bloom.strength = (ch.id==='intro'||ch.id==='outro') ? 1.15 : ch.id==='sleep'?0.55:0.85;
    this.lights.pt.intensity = 14 + Math.sin(t*3)*5 * fade;
    const pos=cameraPose(cfg.mode, lt, t); this.camera.position.copy(pos); this.camera.lookAt(0,0,0);
    this.renderer.toneMappingExposure = 0.35+0.65*fade;
    this.composer.render();
    return {chapter:ch.id, lt};
  }
}
