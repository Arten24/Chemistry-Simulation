varying float vP; void main(){ vec2 c=gl_PointCoord-0.5; if(dot(c,c)>0.25) discard; gl_FragColor=vec4(mix(vec3(0.2,0.6,1.0),vec3(1.0,0.75,0.3),vP),1.0); }
