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
in vec2 sampleFootprint;

out vec4 fragColor;

const int MAX_FILTER_TAPS = 16;
const float MAX_FILTER_SCALE = 4.0;
const float PI = 3.14159265358979323846;

vec4 fetchClamped(ivec2 position, ivec2 maximumPosition) {
    return texelFetch(Sampler0, clamp(position, ivec2(0), maximumPosition), 0);
}

vec4 sampleBiased(vec2 textureDimensions) {
    vec2 texelPosition = texCoord0 * textureDimensions - 0.5;
    ivec2 texelBase = ivec2(floor(texelPosition));
    vec2 linearWeight = fract(texelPosition);
    float bias = clamp(pixelBias, 0.0, 1.0);
    float curveExponent = mix(1.0, 12.0, bias);
    vec2 lowerWeight = pow(linearWeight, vec2(curveExponent));
    vec2 upperWeight = pow(1.0 - linearWeight, vec2(curveExponent));
    vec2 adjustedWeight = lowerWeight / max(lowerWeight + upperWeight, vec2(0.00001));
    ivec2 maximumPosition = ivec2(textureDimensions) - ivec2(1);
    vec4 topLeft = fetchClamped(texelBase, maximumPosition);
    vec4 topRight = fetchClamped(texelBase + ivec2(1, 0), maximumPosition);
    vec4 bottomLeft = fetchClamped(texelBase + ivec2(0, 1), maximumPosition);
    vec4 bottomRight = fetchClamped(texelBase + ivec2(1, 1), maximumPosition);
    return mix(
        mix(topLeft, topRight, adjustedWeight.x),
        mix(bottomLeft, bottomRight, adjustedWeight.x),
        adjustedWeight.y
    );
}

float sinc(float value) {
    if (abs(value) < 0.00001) {
        return 1.0;
    }
    float angle = PI * value;
    return sin(angle) / angle;
}

float lanczos2(float distance, float filterScale) {
    float normalizedDistance = distance / filterScale;
    if (abs(normalizedDistance) >= 2.0) {
        return 0.0;
    }
    return sinc(normalizedDistance) * sinc(normalizedDistance * 0.5);
}

vec4 sampleReduced(vec2 textureDimensions, vec2 footprint) {
    float bias = clamp(pixelBias, 0.0, 1.0);
    vec2 filterScale = max(vec2(1.0), footprint * (1.0 - bias * 0.15));
    vec2 sourceCenter = texCoord0 * textureDimensions - 0.5;
    vec2 support = filterScale * 2.0;
    ivec2 firstTexel = ivec2(ceil(sourceCenter - support));
    ivec2 maximumPosition = ivec2(textureDimensions) - ivec2(1);

    vec4 accumulated = vec4(0.0);
    float accumulatedWeight = 0.0;
    for (int y = 0; y < MAX_FILTER_TAPS; y++) {
        int sourceY = firstTexel.y + y;
        float distanceY = sourceCenter.y - float(sourceY);
        if (distanceY < -support.y) {
            break;
        }
        float weightY = lanczos2(distanceY, filterScale.y);
        for (int x = 0; x < MAX_FILTER_TAPS; x++) {
            int sourceX = firstTexel.x + x;
            float distanceX = sourceCenter.x - float(sourceX);
            if (distanceX < -support.x) {
                break;
            }
            float weightX = lanczos2(distanceX, filterScale.x);
            float weight = weightX * weightY;
            accumulated += fetchClamped(ivec2(sourceX, sourceY), maximumPosition) * weight;
            accumulatedWeight += weight;
        }
    }
    vec4 color = accumulated / max(accumulatedWeight, 0.00001);
    color = clamp(color, 0.0, 1.0);
    color.rgb = min(color.rgb, vec3(color.a));
    return color;
}

void main() {
    vec2 textureDimensions = vec2(textureSize(Sampler0, 0));
    vec2 footprint = clamp(sampleFootprint, vec2(1.0), vec2(MAX_FILTER_SCALE));
    vec4 sampled = any(greaterThan(footprint, vec2(1.0001)))
        ? sampleReduced(textureDimensions, footprint)
        : sampleBiased(textureDimensions);

    vec4 color = sampled * vertexColor * ColorModulator;
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color;
}
