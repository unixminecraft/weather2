package extendedrenderer.shader;


import java.util.HashMap;

import CoroUtil.forge.CoroUtil;
import CoroUtil.util.CoroUtilFile;
import net.minecraft.util.ResourceLocation;

public class Renderer {

    private HashMap<String, ShaderProgram> lookupNameToProgram = new HashMap<>();

    //might be worth relocating
    public Transformation transformation;

    public Renderer() {
        transformation = new Transformation();
    }

    public void init() throws Exception {
        ShaderProgram shaderProgram = new ShaderProgramParticle("particle");

        String vertex = CoroUtilFile.getContentsFromResourceLocation(new ResourceLocation(CoroUtil.modID, "shaders/particle.vs"));
        String fragment = CoroUtilFile.getContentsFromResourceLocation(new ResourceLocation(CoroUtil.modID, "shaders/particle.fs"));

        shaderProgram.createVertexShader(vertex);
        shaderProgram.createFragmentShader(fragment);
        shaderProgram.link();
        shaderProgram.createUniform("modelViewMatrixCamera");
        shaderProgram.createUniform("texture_sampler");
        shaderProgram.createUniform("fogmode");

        lookupNameToProgram.put(shaderProgram.getName(), shaderProgram);

        shaderProgram = new ShaderProgramFoliage("foliage");

        vertex = CoroUtilFile.getContentsFromResourceLocation(new ResourceLocation(CoroUtil.modID, "shaders/foliage.vs"));
        fragment = CoroUtilFile.getContentsFromResourceLocation(new ResourceLocation(CoroUtil.modID, "shaders/foliage.fs"));

        shaderProgram.createVertexShader(vertex);
        shaderProgram.createFragmentShader(fragment);
        shaderProgram.link();
        shaderProgram.createUniform("modelViewMatrixCamera");
        shaderProgram.createUniform("texture_sampler");
        shaderProgram.createUniform("time");
        shaderProgram.createUniform("partialTick");
        shaderProgram.createUniform("windDir");
        shaderProgram.createUniform("windSpeed");
        shaderProgram.createUniform("fogmode");

        lookupNameToProgram.put(shaderProgram.getName(), shaderProgram);
    }

    public void cleanup() {
        for (ShaderProgram shaderProgram : lookupNameToProgram.values()) {
            shaderProgram.cleanup();
        }
        lookupNameToProgram.clear();
    }

    public ShaderProgram getShaderProgram(String name) {
        return lookupNameToProgram.get(name);
    }
}
