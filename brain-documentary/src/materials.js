import * as THREE from 'three';
export const MAT = {
 cortex: new THREE.MeshPhysicalMaterial({color:0xc97b84, roughness:0.48, metalness:0.05, clearcoat:0.55, clearcoatRoughness:0.6, sheen:0.4, sheenColor:new THREE.Color(0xff9a7a)}),
 inner: new THREE.MeshBasicMaterial({color:0x2a6cff, transparent:true, opacity:0.16, blending:THREE.AdditiveBlending, depthWrite:false}),
 glowAmber: new THREE.MeshBasicMaterial({color:0xffb340}),
 glowBlue: new THREE.MeshBasicMaterial({color:0x38b6ff}),
 glowPink: new THREE.MeshBasicMaterial({color:0xff4d8d}),
 glowGreen: new THREE.MeshBasicMaterial({color:0x3dff9e}),
};
export const REGION_MATS = Object.fromEntries(['hippocampus','amygdala','prefrontal','visual','basal','cortex'].map(k=>[k, new THREE.MeshBasicMaterial({color:0xffcf5e, transparent:true, opacity:0})]));
