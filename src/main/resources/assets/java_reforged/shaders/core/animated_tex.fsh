#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec2 nextTexCoordPixels;
in vec4 vertexColor;
in float interpolationProgress;

out vec4 fragColor;

void main() {
    vec2 nextTexCoord = nextTexCoordPixels / vec2(textureSize(Sampler0, 0));
    vec4 currentColor = texture(Sampler0, texCoord0);
    vec4 nextColor = texture(Sampler0, nextTexCoord);
    vec4 color = mix(currentColor, nextColor, interpolationProgress) * vertexColor;
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
