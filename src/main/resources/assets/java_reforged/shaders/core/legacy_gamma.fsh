#version 330

layout(std140) uniform LegacyGammaConfig {
    float Strength;
};

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

uint pixelHash(uvec2 position) {
    uint state = position.x * 747796405u + position.y * 2891336453u + 277803737u;
    state = (state ^ (state >> 16u)) * 2246822519u;
    state = (state ^ (state >> 13u)) * 3266489917u;
    return state ^ (state >> 16u);
}

float pixelNoise(vec2 position) {
    return float(pixelHash(uvec2(position))) / 4294967295.0;
}

void main() {
    vec4 source = texture(InSampler, texCoord);
    float strength = clamp(Strength, 0.0, 1.0);
    // Legacy's midpoint exponent is 0.8 because its source framebuffer was
    // already encoded differently. Normalize by that midpoint so 50% is an
    // exact neutral point for Java's framebuffer while preserving the curve.
    float exponent = 1.25 / (0.5 + 1.5 * strength);
    vec3 corrected = pow(max(source.rgb, vec3(0.0)), vec3(exponent));

    // The input remains floating point until this final pass. Dither only the
    // single conversion to the RGBA8 presentation target.
    const float HALF_LSB = 0.5 / 255.0;
    vec3 endpointMask = step(vec3(HALF_LSB), source.rgb)
        * step(vec3(HALF_LSB), vec3(1.0) - source.rgb);
    float noise = pixelNoise(gl_FragCoord.xy) * 2.0 - 1.0;
    corrected += noise * HALF_LSB * endpointMask;

    fragColor = vec4(clamp(corrected, 0.0, 1.0), source.a);
}
