package extendedrenderer.particle.entity;

import javax.vecmath.Vector3f;

import extendedrenderer.shader.InstancedMeshParticle;
import extendedrenderer.shader.Matrix4fe;
import extendedrenderer.shader.Transformation;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/**
 * Meant for interpolating rotation around a central point that isnt itself
 * - requires setting up the matrix correctly therefor a full custom render call seemed the cleanest
 */
public class ParticleCustomMatrix extends ParticleTexFX {

	private float angleX;
	private float angleY;
	private float yy;

    public ParticleCustomMatrix(World worldIn, double posXIn, double posYIn, double posZIn, double mX, double mY, double mZ, TextureAtlasSprite par8Item) {
        super(worldIn, posXIn, posYIn, posZIn, mX, mY, mZ, par8Item);
    }

    @Override
    public void renderParticleForShader(InstancedMeshParticle mesh, Transformation transformation, Matrix4fe viewMatrix, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        if (mesh.curBufferPos >= mesh.numInstances) return;

        //camera relative positions, for world position, remove the interpPos values
        float posX = (float) (this.prevPosX + (this.posX - this.prevPosX) * (double) partialTicks - Particle.interpPosX);
        float posY = (float) (this.prevPosY + (this.posY - this.prevPosY) * (double) partialTicks - Particle.interpPosY);
        float posZ = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * (double) partialTicks - Particle.interpPosZ);
        Vector3f pos = new Vector3f(posX, posY, posZ);

        Matrix4fe matrixFunnel = new Matrix4fe();
        matrixFunnel.rotateY(angleY);
        matrixFunnel.rotateX(angleX);
        matrixFunnel.translate(new Vector3f(0, yy, 0));

        //fix interpolation flicker when it wraps from 360 back to 0
        //TODO: support rotation in other direction
        if (rotationAroundCenter < rotationAroundCenterPrev) {
            rotationAroundCenterPrev -= 360;
        }
        float deltaRot = rotationAroundCenterPrev + (rotationAroundCenter - rotationAroundCenterPrev) * partialTicks;
        matrixFunnel.translate(new Vector3f((float)Math.sin(Math.toRadians(deltaRot)) * rotationDistAroundCenter,
                0,
                (float)Math.cos(Math.toRadians(deltaRot)) * rotationDistAroundCenter));
        Vector3f posExtraRot = matrixFunnel.getTranslation();

        pos.x += posExtraRot.x;
        pos.y += posExtraRot.y;
        pos.z += posExtraRot.z;

        //adjust to perspective and camera
        Matrix4fe modelMatrix = transformation.buildModelMatrix(this, pos, partialTicks);

        //upload to buffer
        modelMatrix.get(InstancedMeshParticle.INSTANCE_SIZE_FLOATS * (mesh.curBufferPos), mesh.instanceDataBuffer);

        //brightness
        float brightness;
        brightness = brightnessCache;
        mesh.instanceDataBuffer.put(InstancedMeshParticle.INSTANCE_SIZE_FLOATS * (mesh.curBufferPos) + InstancedMeshParticle.MATRIX_SIZE_FLOATS, brightness);

        int rgbaIndex = 0;
        mesh.instanceDataBuffer.put(InstancedMeshParticle.INSTANCE_SIZE_FLOATS * (mesh.curBufferPos)
                + InstancedMeshParticle.MATRIX_SIZE_FLOATS + 1 + (rgbaIndex++), this.getRedColorF());
        mesh.instanceDataBuffer.put(InstancedMeshParticle.INSTANCE_SIZE_FLOATS * (mesh.curBufferPos)
                + InstancedMeshParticle.MATRIX_SIZE_FLOATS + 1 + (rgbaIndex++), this.getGreenColorF());
        mesh.instanceDataBuffer.put(InstancedMeshParticle.INSTANCE_SIZE_FLOATS * (mesh.curBufferPos)
                + InstancedMeshParticle.MATRIX_SIZE_FLOATS + 1 + (rgbaIndex++), this.getBlueColorF());
        mesh.instanceDataBuffer.put(InstancedMeshParticle.INSTANCE_SIZE_FLOATS * (mesh.curBufferPos)
                + InstancedMeshParticle.MATRIX_SIZE_FLOATS + 1 + (rgbaIndex++), this.getAlphaF());

        mesh.curBufferPos++;
    }
}
