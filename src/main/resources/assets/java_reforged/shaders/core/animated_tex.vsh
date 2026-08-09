#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in ivec2 UV1;
in float LineWidth;

out vec2 texCoord0;
out vec2 nextTexCoordPixels;
out vec4 vertexColor;
out float interpolationProgress;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    texCoord0 = UV0;
    nextTexCoordPixels = vec2(UV1);
    vertexColor = Color;
    interpolationProgress = LineWidth;
}
