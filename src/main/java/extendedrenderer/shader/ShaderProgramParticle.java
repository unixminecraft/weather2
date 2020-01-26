package extendedrenderer.shader;

import extendedrenderer.particle.ShaderManager;

public class ShaderProgramParticle extends ShaderProgram {

    private int vertexShaderAttributeIndexPosition = 0;
    private int vertexShaderAttributeTexCoord = 1;
    private int vertexShaderAttributeModelMatrix = InstancedMeshParticle.vboSizeMesh;//5;
    private int vertexShaderAttributeBrightness = InstancedMeshParticle.vboSizeMesh + 4;//9;
    private int vertexShaderAttributeRGBATest = InstancedMeshParticle.vboSizeMesh + 5;//10;

    public ShaderProgramParticle(String name) throws Exception {
        super(name);
    }

    @Override
    public void setupAttribLocations() {
        int offset = 0;
        ShaderManager.glBindAttribLocation(getProgramId(), vertexShaderAttributeIndexPosition+offset, "position");
        ShaderManager.glBindAttribLocation(getProgramId(), vertexShaderAttributeTexCoord+offset, "texCoord");
        ShaderManager.glBindAttribLocation(getProgramId(), vertexShaderAttributeModelMatrix+offset, "modelMatrix");
        ShaderManager.glBindAttribLocation(getProgramId(), vertexShaderAttributeBrightness+offset, "brightness");
        ShaderManager.glBindAttribLocation(getProgramId(), vertexShaderAttributeRGBATest+offset, "rgba");
    }
}
