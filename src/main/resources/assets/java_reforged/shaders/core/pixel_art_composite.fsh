#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;
in float pixelBias;

out vec4 fragColor;

void main() {
    vec2 textureDimensions = vec2(textureSize(Sampler0, 0));
    vec2 texelPosition = texCoord0 * textureDimensions - 0.5;
    vec2 texelBase = floor(texelPosition);
    vec2 linearWeight = fract(texelPosition);
    float bias = clamp(pixelBias, 0.0, 1.0);
    float curveExponent = mix(1.0, 12.0, bias);
    vec2 lowerWeight = pow(linearWeight, vec2(curveExponent));
    vec2 upperWeight = pow(1.0 - linearWeight, vec2(curveExponent));
    vec2 adjustedWeight = lowerWeight / max(lowerWeight + upperWeight, vec2(0.00001));
    vec2 adjustedUv = (texelBase + adjustedWeight + 0.5) / textureDimensions;

    vec4 color = texture(Sampler0, adjustedUv) * vertexColor * ColorModulator;
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color;
}
