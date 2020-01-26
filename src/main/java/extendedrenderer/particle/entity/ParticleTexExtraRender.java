package extendedrenderer.particle.entity;

import javax.vecmath.Vector3f;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector4f;

import CoroUtil.util.CoroUtilBlockLightCache;
import CoroUtil.util.CoroUtilParticle;
import extendedrenderer.render.RotatingParticleManager;
import extendedrenderer.shader.InstancedMeshParticle;
import extendedrenderer.shader.Matrix4fe;
import extendedrenderer.shader.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ParticleTexExtraRender extends ParticleTexFX {

	private int severityOfRainRate = 2;

	private int extraParticlesBaseAmount = 5;

	public boolean noExtraParticles = false;

	public ParticleTexExtraRender(World worldIn, double posXIn, double posYIn,
			double posZIn, double mX, double mY, double mZ,
			TextureAtlasSprite par8Item) {
		super(worldIn, posXIn, posYIn, posZIn, mX, mY, mZ, par8Item);
	}

	public void setExtraParticlesBaseAmount(int extraParticlesBaseAmount) {
		this.extraParticlesBaseAmount = extraParticlesBaseAmount;
	}

	@Override
	protected void tickExtraRotations() {

		if (isSlantParticleToWind()) {
			rotationYaw = (float)Math.toDegrees(Math.atan2(motionZ, motionX)) - 90;
			double motionXZ = Math.sqrt(motionX * motionX + motionZ * motionZ);
			rotationPitch = -(float)Math.toDegrees(Math.atan2(motionXZ, Math.abs(motionY)));
		}

		if (!quatControl) {
			Entity ent = Minecraft.getMinecraft().getRenderViewEntity();
			updateQuaternion(ent);
		}
	}

	@Override
	public void renderParticle(BufferBuilder worldRendererIn, Entity entityIn,
			float partialTicks, float rotationX, float rotationZ,
			float rotationYZ, float rotationXY, float rotationXZ) {

		//override rotations
		if (!facePlayer) {
			rotationX = MathHelper.cos(this.rotationYaw * (float)Math.PI / 180.0F);
			rotationYZ = MathHelper.sin(this.rotationYaw * (float)Math.PI / 180.0F);
	        rotationXY = -rotationYZ * MathHelper.sin(this.rotationPitch * (float)Math.PI / 180.0F);
	        rotationXZ = rotationX * MathHelper.sin(this.rotationPitch * (float)Math.PI / 180.0F);
	        rotationZ = MathHelper.cos(this.rotationPitch * (float)Math.PI / 180.0F);
		} else {
			if (this.isSlantParticleToWind()) {
				rotationXZ = (float) -this.motionZ;
				rotationXY = (float) -this.motionX;
			}
		}

		
		float f = (float)this.particleTextureIndexX / 16.0F;
        float f1 = f + 0.0624375F;
        float f2 = (float)this.particleTextureIndexY / 16.0F;
        float f3 = f2 + 0.0624375F;
        float scale1 = 0.1F * this.particleScale;
		float scale2 = 0.1F * this.particleScale;
		float scale3 = 0.1F * this.particleScale;
		float scale4 = 0.1F * this.particleScale;

		float fixY = 0;

        if (this.particleTexture != null)
        {
            f = this.particleTexture.getMinU();
            f1 = this.particleTexture.getMaxU();
            f2 = this.particleTexture.getMinV();
            f3 = this.particleTexture.getMaxV();
			float part = 16F / 3F;
			float offset = 0;
			float posBottom = (float)(this.posY - 10D);

			float height = this.world.getPrecipitationHeight(new BlockPos(this.posX, this.posY, this.posZ)).getY();

			if (posBottom < height) {
				float diff = height - posBottom;
				offset = diff;
				fixY = 0;//diff * 1.0F;
				if (offset > part) offset = part;
			}
        }

		int renderAmount = 0;
		if (noExtraParticles) {
			renderAmount = 1;
		} else {
			renderAmount = Math.min(extraParticlesBaseAmount + ((Math.max(0, severityOfRainRate-1)) * 5), CoroUtilParticle.maxRainDrops);
		}

		//catch code hotload crash, doesnt help much anyways
		try {
			for (int ii = 0; ii < renderAmount/*(noExtraParticles ? 1 : Math.min(rainDrops, CoroUtilParticle.maxRainDrops))*/; ii++) {
				float f5 = (float)(this.prevPosX + (this.posX - this.prevPosX) * (double)partialTicks - interpPosX);
				float f6 = (float)(this.prevPosY + (this.posY - this.prevPosY) * (double)partialTicks - interpPosY) + fixY;
				float f7 = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * (double)partialTicks - interpPosZ);

				double xx = 0;
				double zz = 0;
				double yy = 0;
				if (ii != 0) {
					xx = CoroUtilParticle.rainPositions[ii].xCoord;
					zz = CoroUtilParticle.rainPositions[ii].zCoord;
					yy = CoroUtilParticle.rainPositions[ii].yCoord;

					f5 += xx;
					f6 += yy;
					f7 += zz;
				}

				//prevent precip under overhangs/inside for extra render
				if (this.isDontRenderUnderTopmostBlock()) {
					int height = this.world.getPrecipitationHeight(new BlockPos(this.posX + xx, this.posY, this.posZ + zz)).getY();
					if (this.posY + yy <= height) continue;
				}

				if (ii != 0) {
					RotatingParticleManager.debugParticleRenderCount++;
				}
				int i = this.getBrightnessForRender(partialTicks);
				i = 15728640;
				int j = i >> 16 & 65535;
				int k = i & 65535;

				Vec3d[] avec3d = new Vec3d[] {
						new Vec3d((double)(-rotationX * scale1 - rotationXY * scale1), (double)(-rotationZ * scale1), (double)(-rotationYZ * scale1 - rotationXZ * scale1)),
						new Vec3d((double)(-rotationX * scale2 + rotationXY * scale2), (double)(rotationZ * scale2), (double)(-rotationYZ * scale2 + rotationXZ * scale2)),
						new Vec3d((double)(rotationX * scale3 + rotationXY * scale3), (double)(rotationZ * scale3), (double)(rotationYZ * scale3 + rotationXZ * scale3)),
						new Vec3d((double)(rotationX * scale4 - rotationXY * scale4), (double)(-rotationZ * scale4), (double)(rotationYZ * scale4 - rotationXZ * scale4))};

				worldRendererIn.pos((double)f5 + avec3d[0].x, (double)f6 + avec3d[0].y, (double)f7 + avec3d[0].z).tex((double)f1, (double)f3).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j, k).endVertex();
				worldRendererIn.pos((double)f5 + avec3d[1].x, (double)f6 + avec3d[1].y, (double)f7 + avec3d[1].z).tex((double)f1, (double)f2).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j, k).endVertex();
				worldRendererIn.pos((double)f5 + avec3d[2].x, (double)f6 + avec3d[2].y, (double)f7 + avec3d[2].z).tex((double)f, (double)f2).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j, k).endVertex();
				worldRendererIn.pos((double)f5 + avec3d[3].x, (double)f6 + avec3d[3].y, (double)f7 + avec3d[3].z).tex((double)f, (double)f3).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j, k).endVertex();
			}
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
		


        
	}

	public void renderParticleForShader(InstancedMeshParticle mesh, Transformation transformation, Matrix4fe viewMatrix, Entity entityIn,
										float partialTicks, float rotationX, float rotationZ,
										float rotationYZ, float rotationXY, float rotationXZ) {

		float posX = (float) (this.prevPosX + (this.posX - this.prevPosX) * (double) partialTicks);
		float posY = (float) (this.prevPosY + (this.posY - this.prevPosY) * (double) partialTicks);
		float posZ = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * (double) partialTicks);

		int renderAmount = 0;
		if (noExtraParticles) {
			renderAmount = 1;
		} else {
			renderAmount = Math.min(extraParticlesBaseAmount + ((Math.max(0, severityOfRainRate-1)) * 5), CoroUtilParticle.maxRainDrops);
		}

		for (int iii = 0; iii < renderAmount; iii++) {

			if (mesh.curBufferPos >= mesh.numInstances) return;

			Vector3f pos;

			if (iii != 0) {
				pos = new Vector3f(posX + (float) CoroUtilParticle.rainPositions[iii].xCoord,
						posY + (float) CoroUtilParticle.rainPositions[iii].yCoord,
						posZ + (float) CoroUtilParticle.rainPositions[iii].zCoord);
			} else {
				pos = new Vector3f(posX, posY, posZ);
			}

			if (this.isDontRenderUnderTopmostBlock()) {
				int height = this.world.getPrecipitationHeight(new BlockPos(pos.x, this.posY, pos.z)).getY();
				if (pos.y <= height) continue;
			}

			//adjust to relative to camera positions finally
			pos.x -= interpPosX;
			pos.y -= interpPosY;
			pos.z -= interpPosZ;

			Matrix4fe modelMatrix = transformation.buildModelMatrix(this, pos, partialTicks);

			//adjust to perspective and camera
			//upload to buffer
			modelMatrix.get(InstancedMeshParticle.INSTANCE_SIZE_FLOATS * (mesh.curBufferPos), mesh.instanceDataBuffer);

			//brightness
			float brightness;
			if (fastLight) {
				brightness = CoroUtilBlockLightCache.brightnessPlayer;
			} else {
				brightness = CoroUtilBlockLightCache.getBrightnessCached(world, (float)this.posX, (float)this.posY, (float)this.posZ);
			}

			//brightness to buffer
			mesh.instanceDataBuffer.put(InstancedMeshParticle.INSTANCE_SIZE_FLOATS * (mesh.curBufferPos) + InstancedMeshParticle.MATRIX_SIZE_FLOATS, brightness);

			//rgba to buffer
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

	@Override
	public void updateQuaternion(Entity camera) {

		if (camera != null) {
			if (this.facePlayer) {
				this.rotationYaw = camera.rotationYaw;
				this.rotationPitch = camera.rotationPitch;
			} else if (facePlayerYaw) {
				this.rotationYaw = camera.rotationYaw;
			}
		}

		Quaternion qY = new Quaternion();
		Quaternion qX = new Quaternion();
		qY.setFromAxisAngle(new Vector4f(0, 1, 0, (float)Math.toRadians(-this.rotationYaw - 180F)));
		qX.setFromAxisAngle(new Vector4f(1, 0, 0, (float)Math.toRadians(-this.rotationPitch)));
		if (this.rotateOrderXY) {
			Quaternion.mul(qX, qY, this.rotation);
		} else {
			Quaternion.mul(qY, qX, this.rotation);

			if (extraYRotation != 0) {
				//float rot = (new Random()).nextFloat() * 360F;
				qY = new Quaternion();
				qY.setFromAxisAngle(new Vector4f(0, 1, 0, extraYRotation));
				Quaternion.mul(this.rotation, qY, this.rotation);
			}
		}
	}
}
