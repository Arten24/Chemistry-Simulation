void main(){ vec3 p=position + normal * sin(position.y*14.0+position.x*9.0)*0.008; gl_Position=projectionMatrix*modelViewMatrix*vec4(p,1.0); }
