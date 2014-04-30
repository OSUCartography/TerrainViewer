varying vec3 N;
varying vec3 v;

uniform sampler2D tex;
uniform sampler2D colorMap;
varying vec3 L;

void main (void) {

    //vec3 L = normalize(gl_LightSource[0].position.xyz);
    // L = normalize(vec3(gl_LightSource[0].position));

    vec2 Lxy = normalize(vec2(L));
    vec2 Nxy = normalize(vec2(N));
    float Idiff = (dot(Lxy, Nxy) * 0.5 + 0.5); //  * gl_FrontLightProduct[0].diffuse;

    vec4 Iamb = gl_FrontLightProduct[0].ambient;
    /* vec4 Idiff = gl_FrontLightProduct[0].diffuse * max(dot(N,L), 0.0); */

    // vec4 Idiff = gl_FrontLightProduct[0].diffuse * abs(dot(N,L));

    /*vec4 Idiff = gl_FrontLightProduct[0].diffuse * dot(N,L) * 0.5 + 0.5;*/
    /* vec4 Ispec = gl_FrontLightProduct[0].specular * pow(max(dot(R,E),0.0), gl_FrontMaterial.shininess); */


    /* gl_FragColor = gl_FrontLightModelProduct.sceneColor + Iamb + Idiff + Ispec; */
    gl_FragColor = gl_FrontLightModelProduct.sceneColor + Iamb + Idiff;

}
