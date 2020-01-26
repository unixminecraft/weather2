package extendedrenderer.foliage;

import java.nio.FloatBuffer;
import java.util.Random;

import javax.vecmath.Vector3f;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector4f;

import CoroUtil.util.CoroUtilBlockLightCache;
import extendedrenderer.shader.IShaderRenderedEntity;
import extendedrenderer.shader.InstancedMeshFoliage;
import extendedrenderer.shader.Matrix4fe;
import extendedrenderer.shader.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.NoiseGeneratorPerlin;

public class Foliage implements IShaderRenderedEntity {

    public double posX;
    public double posY;
    public double posZ;
    public double prevPosX;
    public double prevPosY;
    public double prevPosZ;

    public float particleScale = 1F;

    /** The red amount of color. Used as a percentage, 1.0 = 255 and 0.0 = 0. */
    public float particleRed = 1F;
    /** The green amount of color. Used as a percentage, 1.0 = 255 and 0.0 = 0. */
    public float particleGreen = 1F;
    /** The blue amount of color. Used as a percentage, 1.0 = 255 and 0.0 = 0. */
    public float particleBlue = 1F;

    /** Particle alpha */
    public float particleAlpha = 1F;
    public TextureAtlasSprite particleTexture;

    public float rotationYaw;
    private float rotationPitch;

    private Quaternion rotation = new Quaternion();

    private boolean rotateOrderXY = false;

    public float brightnessCache = 0.5F;

    private int animationID = 0;
    public int heightIndex = 0;
    public float looseness = 1;

    private static final Random rand = new Random(439875L);

    private static final NoiseGeneratorPerlin delayNoise = new NoiseGeneratorPerlin(rand, 3);

    public Foliage(TextureAtlasSprite sprite) {
        particleTexture = sprite;
    }

    public void setPosition(BlockPos pos) {
        posX = pos.getX();
        posY = pos.getY();
        posZ = pos.getZ();
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
    }

    @Override
    public Vector3f getPosition() {
        return new Vector3f((float)posX, (float)posY, (float)posZ);
    }

    @Override
    public Quaternion getQuaternion() {
        return rotation;
    }

    //TODO: implement prev quat
    @Override
    public Quaternion getQuaternionPrev() {
        return null;
    }

    @Override
    public float getScale() {
        return particleScale;
    }

    public void updateQuaternion(Entity camera) {

        Quaternion qY = new Quaternion();
        Quaternion qX = new Quaternion();
        qY.setFromAxisAngle(new Vector4f(0, 1, 0, (float)Math.toRadians(-this.rotationYaw - 180F)));
        qX.setFromAxisAngle(new Vector4f(1, 0, 0, (float)Math.toRadians(-this.rotationPitch)));
        if (this.rotateOrderXY) {
            Quaternion.mul(qX, qY, this.rotation);
        } else {
            Quaternion.mul(qY, qX, this.rotation);
        }
    }

    public void renderForShaderVBO1(InstancedMeshFoliage mesh, Transformation transformation, Matrix4fe viewMatrix, Entity entityIn,
                                        float partialTicks) {

        if (mesh.curBufferPosVBO1 >= mesh.numInstances) {
            return;
        }

        mesh.instanceDataBufferVBO1.put(InstancedMeshFoliage.INSTANCE_SIZE_FLOATS * (mesh.curBufferPosVBO1), particleAlpha);
        float brightness;
        brightness = CoroUtilBlockLightCache.getBrightnessCached(Minecraft.getMinecraft().world, (float)this.posX, (float)this.posY, (float)this.posZ);
        mesh.instanceDataBufferVBO1.put(InstancedMeshFoliage.INSTANCE_SIZE_FLOATS * (mesh.curBufferPosVBO1) + 1, brightness);

        mesh.curBufferPosVBO1++;

    }

    public void renderForShaderVBO2(InstancedMeshFoliage mesh, Transformation transformation, Matrix4fe viewMatrix, Entity entityIn,
                                            float partialTicks) {

        boolean autoGrowBuffer = false;
        if (mesh.curBufferPosVBO2 >= mesh.numInstances) {

            //cant quite get this to work correctly without lots of missing renders ingame until next thread update, why?

            if (autoGrowBuffer) {
                mesh.numInstances *= 2;
                System.out.println("hit max mesh count, doubling in size to " + mesh.numInstances);
                FloatBuffer newBuffer = BufferUtils.createFloatBuffer(mesh.numInstances * InstancedMeshFoliage.INSTANCE_SIZE_FLOATS_SELDOM);
                mesh.instanceDataBufferVBO2.rewind();
                newBuffer.put(mesh.instanceDataBufferVBO2);
                mesh.instanceDataBufferVBO2.rewind();
                newBuffer.flip();
                mesh.instanceDataBufferVBO2 = newBuffer;
                mesh.instanceDataBufferVBO2.position(mesh.curBufferPosVBO2 * InstancedMeshFoliage.INSTANCE_SIZE_FLOATS_SELDOM);
                newBuffer = BufferUtils.createFloatBuffer(mesh.numInstances * InstancedMeshFoliage.INSTANCE_SIZE_FLOATS);
                newBuffer.clear();
                mesh.instanceDataBufferVBO1 = newBuffer;
            } else {
                return;
            }
        }

        //camera relative positions, for world position, remove the interpPos values
        float posX = (float) (this.prevPosX + (this.posX - this.prevPosX) * (double) partialTicks - mesh.interpPosXThread);
        float posY = (float) (this.prevPosY + (this.posY - this.prevPosY) * (double) partialTicks - mesh.interpPosYThread);
        float posZ = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * (double) partialTicks - mesh.interpPosZThread);
        Vector3f pos = new Vector3f(posX, posY, posZ);

        Matrix4fe modelMatrix = transformation.buildModelMatrix(this, pos, partialTicks);

        //adjust to perspective and camera
        //Matrix4fe modelViewMatrix = transformation.buildModelViewMatrix(modelMatrix, viewMatrix);
        //upload to buffer
        modelMatrix.get(InstancedMeshFoliage.INSTANCE_SIZE_FLOATS_SELDOM * (mesh.curBufferPosVBO2), mesh.instanceDataBufferVBO2);

        int floatIndex = 0;
        mesh.instanceDataBufferVBO2.put(InstancedMeshFoliage.INSTANCE_SIZE_FLOATS_SELDOM * (mesh.curBufferPosVBO2) + InstancedMeshFoliage.MATRIX_SIZE_FLOATS
                + (floatIndex++), this.particleRed);
        mesh.instanceDataBufferVBO2.put(InstancedMeshFoliage.INSTANCE_SIZE_FLOATS_SELDOM * (mesh.curBufferPosVBO2) + InstancedMeshFoliage.MATRIX_SIZE_FLOATS
                + (floatIndex++), this.particleGreen);
        mesh.instanceDataBufferVBO2.put(InstancedMeshFoliage.INSTANCE_SIZE_FLOATS_SELDOM * (mesh.curBufferPosVBO2) + InstancedMeshFoliage.MATRIX_SIZE_FLOATS
                + (floatIndex++), this.particleBlue);
        //using yaw here instead, alpha in other VBO
        mesh.instanceDataBufferVBO2.put(InstancedMeshFoliage.INSTANCE_SIZE_FLOATS_SELDOM * (mesh.curBufferPosVBO2) + InstancedMeshFoliage.MATRIX_SIZE_FLOATS
                + (floatIndex++), this.rotationYaw);

        //index, aka buffer pos?
        mesh.instanceDataBufferVBO2.put(InstancedMeshFoliage.INSTANCE_SIZE_FLOATS_SELDOM * (mesh.curBufferPosVBO2) + InstancedMeshFoliage.MATRIX_SIZE_FLOATS
                + (floatIndex++), (float)delayNoise.getValue(this.posX, this.posZ));


        mesh.instanceDataBufferVBO2.put(InstancedMeshFoliage.INSTANCE_SIZE_FLOATS_SELDOM * (mesh.curBufferPosVBO2) + InstancedMeshFoliage.MATRIX_SIZE_FLOATS
                + (floatIndex++), animationID);

        mesh.instanceDataBufferVBO2.put(InstancedMeshFoliage.INSTANCE_SIZE_FLOATS_SELDOM * (mesh.curBufferPosVBO2) + InstancedMeshFoliage.MATRIX_SIZE_FLOATS
                + (floatIndex++), heightIndex);

        mesh.instanceDataBufferVBO2.put(InstancedMeshFoliage.INSTANCE_SIZE_FLOATS_SELDOM * (mesh.curBufferPosVBO2) + InstancedMeshFoliage.MATRIX_SIZE_FLOATS
                + (floatIndex++), looseness);

        mesh.curBufferPosVBO2++;
    }
}
