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

const int MAX_FILTER_TAPS = 10;

vec4 fetchClamped(ivec2 position, ivec2 maximumPosition) {
    return texelFetch(Sampler0, clamp(position, ivec2(0), maximumPosition), 0);
}

vec4 sampleArea(vec2 textureDimensions) {
    vec2 footprint = clamp(sampleFootprint, vec2(1.0), vec2(8.0));
    vec2 sourceCenter = texCoord0 * textureDimensions;
    vec2 areaMinimum = sourceCenter - footprint * 0.5;
    vec2 areaMaximum = sourceCenter + footprint * 0.5;
    ivec2 firstTexel = ivec2(floor(areaMinimum));
    ivec2 maximumPosition = ivec2(textureDimensions) - ivec2(1);

    vec4 accumulated = vec4(0.0);
    float accumulatedWeight = 0.0;
    for (int y = 0; y < MAX_FILTER_TAPS; y++) {
        int sourceY = firstTexel.y + y;
        float weightY = max(
            0.0,
            min(areaMaximum.y, float(sourceY + 1))
                - max(areaMinimum.y, float(sourceY))
        );
        if (weightY <= 0.0) {
            continue;
        }
        for (int x = 0; x < MAX_FILTER_TAPS; x++) {
            int sourceX = firstTexel.x + x;
            float weightX = max(
                0.0,
                min(areaMaximum.x, float(sourceX + 1))
                    - max(areaMinimum.x, float(sourceX))
            );
            if (weightX <= 0.0) {
                continue;
            }
            float weight = weightX * weightY;
            accumulated += fetchClamped(
                ivec2(sourceX, sourceY),
                maximumPosition
            ) * weight;
            accumulatedWeight += weight;
        }
    }
    return accumulated / max(accumulatedWeight, 0.00001);
}

void main() {
    vec2 textureDimensions = vec2(textureSize(Sampler0, 0));
    vec4 sampled = sampleArea(textureDimensions);

    vec4 color = sampled * vertexColor * ColorModulator;
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color;
}
