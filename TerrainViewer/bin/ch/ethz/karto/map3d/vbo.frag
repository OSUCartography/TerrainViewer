// texture that is draped onto the terrain
uniform sampler2D imageTexture;

// hypsometric 1D texture
uniform sampler1D hypsoTexture;

uniform sampler2D hypsoLookUpTexture;

// 0: no texture
// 1: 1D hypsometric texture
// 2: 2D texture
uniform int textureType;

// if true, shading is computed, otherwise only texture mapping is applied
uniform bool applyShading;

// if true, fog is added
uniform bool applyFog;

// the position to sample imageTexture
varying vec3 texCoord;

// the fog weight
varying float fog;

void main (void) {

    if (textureType == 0) {
        gl_FragColor = vec4(1.);
    } else if (textureType == 1) {
        gl_FragColor = texture1D(hypsoTexture, texCoord.z);
    } else if (textureType == 2) {
        gl_FragColor = texture2D(imageTexture, texCoord.xy);
    } else { // textureType -1
        float z = texture2D(hypsoLookUpTexture, texCoord.xy).x;
        gl_FragColor = texture1D(hypsoTexture, z);
    }

    if (applyShading) {
        gl_FragColor *= gl_Color;
    }

    if (applyFog) {
        gl_FragColor = mix(gl_Fog.color, gl_FragColor, fog);
    }

}