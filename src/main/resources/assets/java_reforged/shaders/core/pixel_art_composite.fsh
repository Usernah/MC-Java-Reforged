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

const int MAX_FILTER_TAPS = 8;

vec4 sampleBiased(vec2 textureDimensions) {
    vec2 texelPosition = texCoord0 * textureDimensions - 0.5;
    vec2 texelBase = floor(texelPosition);
    vec2 linearWeight = fract(texelPosition);
    float bias = clamp(pixelBias, 0.0, 1.0);
    float curveExponent = mix(1.0, 12.0, bias);
    vec2 lowerWeight = pow(linearWeight, vec2(curveExponent));
    vec2 upperWeight = pow(1.0 - linearWeight, vec2(curveExponent));
    vec2 adjustedWeight = lowerWeight / max(lowerWeight + upperWeight, vec2(0.00001));
    vec2 adjustedUv = (texelBase + adjustedWeight + 0.5) / textureDimensions;
    return texture(Sampler0, adjustedUv);
}

vec4 sampleArea(vec2 textureDimensions, vec2 footprint) {
    vec2 filterSize = clamp(footprint, vec2(1.0), vec2(float(MAX_FILTER_TAPS)));
    vec2 sourceCenter = texCoord0 * textureDimensions;
    vec2 areaMin = sourceCenter - filterSize * 0.5;
    vec2 areaMax = sourceCenter + filterSize * 0.5;
    ivec2 firstTexel = ivec2(floor(areaMin));
    ivec2 lastTextureTexel = ivec2(textureDimensions) - ivec2(1);

    vec4 accumulated = vec4(0.0);
    float accumulatedWeight = 0.0;
    for (int y = 0; y < MAX_FILTER_TAPS; y++) {
        int sourceY = firstTexel.y + y;
        if (float(sourceY) >= areaMax.y) {
            break;
        }
        float weightY = max(
            0.0,
            min(areaMax.y, float(sourceY + 1)) - max(areaMin.y, float(sourceY))
        );
        for (int x = 0; x < MAX_FILTER_TAPS; x++) {
            int sourceX = firstTexel.x + x;
            if (float(sourceX) >= areaMax.x) {
                break;
            }
            float weightX = max(
                0.0,
                min(areaMax.x, float(sourceX + 1)) - max(areaMin.x, float(sourceX))
            );
            float weight = weightX * weightY;
            ivec2 texel = clamp(ivec2(sourceX, sourceY), ivec2(0), lastTextureTexel);
            accumulated += texelFetch(Sampler0, texel, 0) * weight;
            accumulatedWeight += weight;
        }
    }
    return accumulated / max(accumulatedWeight, 0.00001);
}

void main() {
    vec2 textureDimensions = vec2(textureSize(Sampler0, 0));
    vec2 footprint = fwidth(texCoord0) * textureDimensions;
    bool minifying = any(greaterThan(footprint, vec2(1.0001)));
    vec4 sampled = minifying
        ? sampleArea(textureDimensions, footprint)
        : sampleBiased(textureDimensions);
    vec4 color = sampled * vertexColor * ColorModulator;
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color;
}
