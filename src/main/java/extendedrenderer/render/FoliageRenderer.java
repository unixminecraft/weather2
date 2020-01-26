package extendedrenderer.render;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import CoroUtil.util.CoroUtilBlockLightCache;
import extendedrenderer.foliage.Foliage;
import extendedrenderer.particle.ParticleRegistry;
import extendedrenderer.particle.ShaderManager;
import extendedrenderer.shader.InstancedMeshFoliage;
import extendedrenderer.shader.Matrix4fe;
import extendedrenderer.shader.MeshBufferManagerFoliage;
import extendedrenderer.shader.ShaderEngine;
import extendedrenderer.shader.ShaderProgram;
import extendedrenderer.shader.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;

public class FoliageRenderer {


    public static FloatBuffer projectionMatrixBuffer = BufferUtils.createFloatBuffer(16);
    public static FloatBuffer viewMatrixBuffer = BufferUtils.createFloatBuffer(16);

    public Transformation transformation;

    /**
     * ordered mesh list -> renderables should be enough for no translucency
     *
     * if in future i need translucency, keep above list and make a new one for translucency with:
     *
     * render order list to help lack of use of depth mask
     * - TextureAtlasSprite to Foliage
     *
     *
     * possible solution to alpha sorting for translucency layer?
     * "batch all physobj renders into a single BufferBuilder draw call, call sortVertexData before draw()
     * remember vanilla is regularly sorting all translucent quads in the worldl
     * and all FastTESR quads are sorted"
     *
     * vanilla model -> mesh:
     *
     * instead of doing different sway by singling out certain vertex ids, use:
     * - sway amount = height index + vertex height
     * - will make meshes work more generically for more complex models
     *
     * get the more complex model via:
     * - ModelBakeEvent
     * - instead of json editing models and losing their model
     * -- leave json alone
     * -- hook into event, steal model for my shader, override vanilla model/render with blank
     * -- BufferBuilder.addVertexData eg
     *
     *
     *
     *
     */

    public ConcurrentHashMap<TextureAtlasSprite, List<Foliage>> foliage = new ConcurrentHashMap<>();

    public float windDir = 0;
    public float windSpeedSmooth = 0;
    public Lock lockVBO2 = new ReentrantLock();

    public static int radialRange = 40;
    public static boolean testStaticLimit = false;

    public static long windTime = 0;

    public FoliageRenderer(TextureManager rendererIn) {
        transformation = new Transformation();
    }

    public List<Foliage> getFoliageForSprite(TextureAtlasSprite sprite) {
        List<Foliage> list;
        if (!foliage.containsKey(sprite)) {
            list = new ArrayList<>();
            foliage.put(sprite, list);
        }
        return foliage.get(sprite);
    }

    public void render(Entity entityIn, float partialTicks)
    {

        if (RotatingParticleManager.useShaders) {

            Minecraft mc = Minecraft.getMinecraft();
            GlStateManager.depthMask(true);
            mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            Matrix4fe projectionMatrix = new Matrix4fe();
            FloatBuffer buf = BufferUtils.createFloatBuffer(16);
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, buf);
            buf.rewind();
            Matrix4fe.get(projectionMatrix, 0, buf);

            //modify far distance, 4x as far
            //dont use for now, see RotatingParticleManager notes
            Matrix4fe viewMatrix = new Matrix4fe();
            FloatBuffer buf2 = BufferUtils.createFloatBuffer(16);
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buf2);
            buf2.rewind();
            Matrix4fe.get(viewMatrix, 0, buf2);
            ShaderProgram shaderProgram = ShaderEngine.renderer.getShaderProgram("foliage");
            shaderProgram.bind();
            shaderProgram.setUniform("texture_sampler", 0);
            int glFogMode = GL11.glGetInteger(GL11.GL_FOG_MODE);
            int modeIndex = 0;
            if (glFogMode == GL11.GL_LINEAR) {
                modeIndex = 0;
            } else if (glFogMode == GL11.GL_EXP) {
                modeIndex = 1;
            } else if (glFogMode == GL11.GL_EXP2) {
                modeIndex = 2;
            }
            shaderProgram.setUniform("fogmode", modeIndex);
            shaderProgram.setUniform("partialTick", partialTicks);
            shaderProgram.setUniform("windDir", windDir - 135);
            shaderProgram.setUniform("windSpeed", windSpeedSmooth);

            try {
                shaderProgram.setUniform("time", (int) windTime);
            } catch (Exception ex) {
                //ignore optimization in testing
            }
            MeshBufferManagerFoliage.setupMeshIfMissing(ParticleRegistry.potato);
            MeshBufferManagerFoliage.setupMeshIfMissing(ParticleRegistry.chicken);
            for (int i = 0; i < ParticleRegistry.listFish.size(); i++) {
                MeshBufferManagerFoliage.setupMeshIfMissing(ParticleRegistry.listFish.get(i));
            }
            for (int i = 0; i < ParticleRegistry.listSeaweed.size(); i++) {
                MeshBufferManagerFoliage.setupMeshIfMissing(ParticleRegistry.listSeaweed.get(i));
            }

            for (Map.Entry<TextureAtlasSprite, List<Foliage>> entry : foliage.entrySet()) {
                InstancedMeshFoliage mesh = MeshBufferManagerFoliage.getMesh(entry.getKey());
                if (mesh == null) {
                    System.out.println("NULL MESH FOR: " + entry.getKey().toString());
                    continue;
                }

                mesh.initRender();
                mesh.initRenderVBO1();
                mesh.initRenderVBO2();

                if (lockVBO2.tryLock()) {
                    try {
                        if (mesh.dirtyVBO2Flag) {

                            mesh.interpPosX = mesh.interpPosXThread;
                            mesh.interpPosY = mesh.interpPosYThread;
                            mesh.interpPosZ = mesh.interpPosZThread;
                            OpenGlHelper.glBindBuffer(GL_ARRAY_BUFFER, mesh.instanceDataVBO2);
                            ShaderManager.glBufferData(GL_ARRAY_BUFFER, mesh.instanceDataBufferVBO2, GL_DYNAMIC_DRAW);

                            mesh.dirtyVBO2Flag = false;

                            mesh.curBufferPosVBO2Thread = mesh.curBufferPosVBO2;
                        }

                            List<Foliage> listFoliage = entry.getValue();

                            mesh.instanceDataBufferVBO1.clear();
                            mesh.curBufferPosVBO1 = 0;
                            try {
                                for (int i = 0; i < listFoliage.size(); i++) {
                                    Foliage foliage = listFoliage.get(i);
                                    foliage.particleAlpha = 1F;

                                    foliage.brightnessCache = CoroUtilBlockLightCache.brightnessPlayer + 0.0F;

                                    //update vbo1
                                    foliage.renderForShaderVBO1(mesh, transformation, viewMatrix, entityIn, partialTicks);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            if (testStaticLimit) {
                                mesh.instanceDataBufferVBO1.limit(30000 * InstancedMeshFoliage.INSTANCE_SIZE_FLOATS);
                            } else {
                                mesh.instanceDataBufferVBO1.limit(mesh.curBufferPosVBO1 * InstancedMeshFoliage.INSTANCE_SIZE_FLOATS);
                            }

                            OpenGlHelper.glBindBuffer(GL_ARRAY_BUFFER, mesh.instanceDataVBO1);

                            ShaderManager.glBufferData(GL_ARRAY_BUFFER, mesh.instanceDataBufferVBO1, GL_DYNAMIC_DRAW);
                    } finally {
                        lockVBO2.unlock();
                    }

                }

                float interpX = (float)((entityIn.prevPosX + (entityIn.posX - entityIn.prevPosX) * partialTicks) - mesh.interpPosX);
                float interpY = (float)((entityIn.prevPosY + (entityIn.posY - entityIn.prevPosY) * partialTicks) - mesh.interpPosY);
                float interpZ = (float)((entityIn.prevPosZ + (entityIn.posZ - entityIn.prevPosZ) * partialTicks) - mesh.interpPosZ);

                Matrix4fe matrixFix = new Matrix4fe();
                matrixFix = matrixFix.translationRotateScale(
                        -interpX, -interpY, -interpZ,
                        0, 0, 0, 1,
                        1, 1, 1);

                projectionMatrix = new Matrix4fe();
                buf = BufferUtils.createFloatBuffer(16);
                GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, buf);
                buf.rewind();
                Matrix4fe.get(projectionMatrix, 0, buf);

                Matrix4fe modelViewMatrix = projectionMatrix.mul(viewMatrix);
                matrixFix = modelViewMatrix.mul(matrixFix);

                shaderProgram.setUniformEfficient("modelViewMatrixCamera", matrixFix, viewMatrixBuffer);

                if (mesh.curBufferPosVBO2Thread > 0) {
                    ShaderManager.glDrawElementsInstanced(GL_TRIANGLES, mesh.getVertexCount(), GL_UNSIGNED_INT, 0, mesh.curBufferPosVBO2Thread);
                }

                OpenGlHelper.glBindBuffer(GL_ARRAY_BUFFER, 0);

                mesh.endRenderVBO1();
                mesh.endRenderVBO2();
                mesh.endRender();
            }
        	ShaderEngine.renderer.getShaderProgram("foliage").unbind();
        }
    }
}
