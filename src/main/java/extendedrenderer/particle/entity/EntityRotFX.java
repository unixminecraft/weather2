package extendedrenderer.particle.entity;

import java.util.List;

import javax.vecmath.Vector3f;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector4f;

import CoroUtil.api.weather.IWindHandler;
import CoroUtil.util.CoroUtilBlockLightCache;
import CoroUtil.util.Vec3;
import extendedrenderer.ExtendedRenderer;
import extendedrenderer.particle.behavior.ParticleBehaviors;
import extendedrenderer.shader.IShaderRenderedEntity;
import extendedrenderer.shader.InstancedMeshParticle;
import extendedrenderer.shader.Matrix4fe;
import extendedrenderer.shader.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EntityRotFX extends Particle implements IWindHandler, IShaderRenderedEntity
{
	public float spawnY = -1;
    
    //this field and 2 methods below are for backwards compatibility with old particle system from the new icon based system
    private int particleTextureIndexInt = 0;
    
    public float brightness = 0.7F;
    
    public ParticleBehaviors pb = null; //designed to be a reference to the central objects particle behavior
    
    public boolean callUpdatePB = true;
    
    public float renderRange = 128F;
    
    //used in RotatingEffectRenderer to assist in solving some transparency ordering issues, eg, tornado funnel before clouds
    public int renderOrder = 0;
    
    //not a real entity ID now, just used for making rendering of entities slightly unique
    private int entityID = 0;
    
    public float rotationYaw;
    public float rotationPitch;
    
    public float windWeight = 5;
    private int particleDecayExtra = 0;
    public boolean isTransparent = true;
    
    private boolean killOnCollide = false;
	
	protected boolean facePlayer = false;

	//facePlayer will override this
    public boolean facePlayerYaw = false;
	
    private boolean vanillaMotionDampen = true;

    //for particle behaviors
    public double aboveGroundHeight = 4.5D;
    public boolean collisionSpeedDampen = true;

    public double bounceSpeed = 0.05D;
    public double bounceSpeedMax = 0.15D;
    public double bounceSpeedAhead = 0.35D;
    public double bounceSpeedMaxAhead = 0.25D;

    public boolean spinFast = false;

    private float ticksFadeInMax = 0;
    private float ticksFadeOutMax = 0;

    private boolean dontRenderUnderTopmostBlock = false;

    private boolean killWhenUnderTopmostBlock = false;
    private int killWhenUnderTopmostBlock_ScanAheadRange = 0;

    public int killWhenUnderCameraAtLeast = 0;

    public int killWhenFarFromCameraAtLeast = 0;

    private float ticksFadeOutMaxOnDeath = -1;
    private float ticksFadeOutCurOnDeath = 0;
	private boolean fadingOut = false;

    public float avoidTerrainAngle = 0;

    public float rotationAroundCenter = 0;
    public float rotationAroundCenterPrev = 0;
    public float rotationSpeedAroundCenter = 0;
    public float rotationDistAroundCenter = 0;

    private boolean slantParticleToWind = false;

    protected Quaternion rotation;
    private Quaternion rotationPrev;

    //set to true for direct quaternion control, not EULER conversion helper
    protected boolean quatControl = false;

    public boolean fastLight = false;

    protected float brightnessCache = 0.5F;

    protected boolean rotateOrderXY = false;

	public float extraYRotation = 0;

    protected boolean isCollidedVerticallyDownwards = false;
    
    public EntityRotFX(World par1World, double par2, double par4, double par6, double par8, double par10, double par12)
    {
        super(par1World, par2, par4, par6, par8, par10, par12);
        setSize(0.3F, 0.3F);
        this.entityID = par1World.rand.nextInt(100000);

        rotation = new Quaternion();

        brightnessCache = CoroUtilBlockLightCache.getBrightnessCached(world, (float)posX, (float)posY, (float)posZ);
    }

    protected boolean isSlantParticleToWind() {
        return slantParticleToWind;
    }

    public void setSlantParticleToWind(boolean slantParticleToWind) {
        this.slantParticleToWind = slantParticleToWind;
    }

    public void setTicksFadeOutMaxOnDeath(float ticksFadeOutMaxOnDeath) {
        this.ticksFadeOutMaxOnDeath = ticksFadeOutMaxOnDeath;
    }

    public void setKillWhenUnderTopmostBlock(boolean killWhenUnderTopmostBlock) {
        this.killWhenUnderTopmostBlock = killWhenUnderTopmostBlock;
    }

    protected boolean isDontRenderUnderTopmostBlock() {
        return dontRenderUnderTopmostBlock;
    }

    public void setDontRenderUnderTopmostBlock(boolean dontRenderUnderTopmostBlock) {
        this.dontRenderUnderTopmostBlock = dontRenderUnderTopmostBlock;
    }

    public void setTicksFadeInMax(float ticksFadeInMax) {
        this.ticksFadeInMax = ticksFadeInMax;
    }

    public void setTicksFadeOutMax(float ticksFadeOutMax) {
        this.ticksFadeOutMax = ticksFadeOutMax;
    }
    
    protected int getParticleTextureIndex()
    {
        return this.particleTextureIndexInt;
    }
    
    public void setMaxAge(int par) {
    	particleMaxAge = par;
    }
    
    public float getAlphaF()
    {
        return this.particleAlpha;
    }
    
    @Override
    public void setExpired() {
    	if (pb != null) pb.particles.remove(this);
    	super.setExpired();
    }
    
    @Override
    public void onUpdate() {
    	super.onUpdate();

        Entity ent = Minecraft.getMinecraft().getRenderViewEntity();
    	if (!isVanillaMotionDampen()) {
    		//cancel motion dampening (which is basically air resistance)
    		//keep this up to date with the inverse of whatever Particle.onUpdate uses
        	this.motionX /= 0.9800000190734863D;
            this.motionY /= 0.9800000190734863D;
            this.motionZ /= 0.9800000190734863D;
    	}

    	if (!this.isExpired && !fadingOut) {
            if (killOnCollide) {
                if (this.isCollided()) {
                    startDeath();
                }

            }

            if (killWhenUnderTopmostBlock) {
                int height = this.world.getPrecipitationHeight(new BlockPos(this.posX, this.posY, this.posZ)).getY();
                if (this.posY - killWhenUnderTopmostBlock_ScanAheadRange <= height) {
                    startDeath();
                }
            }

            //case: when on high pillar and rain is falling far below you, start killing it / fading it out
            if (killWhenUnderCameraAtLeast != 0) {
                if (this.posY < ent.posY - killWhenUnderCameraAtLeast) {
                    startDeath();
                }
            }

            if (killWhenFarFromCameraAtLeast != 0) {
                if (getAge() > 20 && getAge() % 5 == 0) {

                    if (ent.getDistance(this.posX, this.posY, this.posZ) > killWhenFarFromCameraAtLeast) {
                        startDeath();
                    }
                }
            }
        }

    	if (!collisionSpeedDampen) {
            if (this.onGround) {
                this.motionX /= 0.699999988079071D;
                this.motionZ /= 0.699999988079071D;
            }
        }

        if (spinFast) {
            this.rotationPitch += this.entityID % 2 == 0 ? 10 : -10;
            this.rotationYaw += this.entityID % 2 == 0 ? -10 : 10;
        }

        if (!fadingOut) {
            if (ticksFadeInMax > 0 && this.getAge() < ticksFadeInMax) {
                this.setAlphaF((float)this.getAge() / ticksFadeInMax);
            } else if (ticksFadeOutMax > 0 && this.getAge() > this.getMaxAge() - ticksFadeOutMax) {
                float count = this.getAge() - (this.getMaxAge() - ticksFadeOutMax);
                float val = (ticksFadeOutMax - (count)) / ticksFadeOutMax;
                this.setAlphaF(val);
            } else if (ticksFadeInMax > 0 || ticksFadeOutMax > 0) {
                this.setAlphaF(1F);
            }
        } else {
    	    if (ticksFadeOutCurOnDeath < ticksFadeOutMaxOnDeath) {
                ticksFadeOutCurOnDeath++;
            } else {
    	        this.setExpired();
            }
            float val = 1F - (ticksFadeOutCurOnDeath / ticksFadeOutMaxOnDeath);
            this.setAlphaF(val);
        }

        if (world.getTotalWorldTime() % 5 == 0) {
            brightnessCache = CoroUtilBlockLightCache.getBrightnessCached(world, (float)posX, (float)posY, (float)posZ);
        }

        rotationAroundCenter += rotationSpeedAroundCenter;
        rotationAroundCenter %= 360;
        tickExtraRotations();
    }

    protected void tickExtraRotations() {
        if (slantParticleToWind) {
            double motionXZ = Math.sqrt(motionX * motionX + motionZ * motionZ);
            rotationPitch = (float)Math.atan2(motionY, motionXZ);
        }

        if (!quatControl) {
            rotationPrev = new Quaternion(rotation);
            Entity ent = Minecraft.getMinecraft().getRenderViewEntity();
            updateQuaternion(ent);
        }
    }

    private void startDeath() {
        if (ticksFadeOutMaxOnDeath > 0) {
            ticksFadeOutCurOnDeath = 0;//ticksFadeOutMaxOnDeath;
            fadingOut = true;
        } else {
            this.setExpired();
        }
    }
    
    public void setParticleTextureIndex(int par1)
    {
        this.particleTextureIndexInt = par1;
        if (this.getFXLayer() == 0) super.setParticleTextureIndex(par1);
    }

    @Override
    public int getFXLayer()
    {
        return 5;
    }

    public void spawnAsWeatherEffect()
    {
        ExtendedRenderer.rotEffRenderer.addEffect(this);
    }

    public int getAge()
    {
        return particleAge;
    }

    public int getMaxAge()
    {
        return particleMaxAge;
    }

    public void setSize(float par1, float par2)
    {
        super.setSize(par1, par2);
        this.setPosition(posX, posY, posZ);
    }
    
    public void setGravity(float par) {
    	particleGravity = par;
    }

    public void setScale(float parScale) {
    	particleScale = parScale;
    }

    @Override
    public Vector3f getPosition() {
        return new Vector3f((float)posX, (float)posY, (float)posZ);
    }

    @Override
    public Quaternion getQuaternion() {
        return this.rotation;
    }

    @Override
    public Quaternion getQuaternionPrev() {
        return this.rotationPrev;
    }

    @Override
    public float getScale() {
    	return particleScale;
    }

    public Vec3 getPos() {
        return new Vec3(posX, posY, posZ);
    }

	public double getPosX() {
		return posX;
	}

	public double getPosY() {
		return posY;
	}

	public double getPosZ() {
		return posZ;
	}

	public double getMotionX() {
		return motionX;
	}

	public void setMotionX(double motionX) {
		this.motionX = motionX;
	}

	public double getMotionY() {
		return motionY;
	}

	public void setMotionY(double motionY) {
		this.motionY = motionY;
	}

	public double getMotionZ() {
		return motionZ;
	}

	public void setMotionZ(double motionZ) {
		this.motionZ = motionZ;
	}

	public void setPrevPosX(double prevPosX) {
		this.prevPosX = prevPosX;
	}

	public void setPrevPosY(double prevPosY) {
		this.prevPosY = prevPosY;
	}

	public void setPrevPosZ(double prevPosZ) {
		this.prevPosZ = prevPosZ;
	}

	public int getEntityId() {
		return entityID;
	}
	
	public World getWorld() {
		return this.world;
	}
	
	public void setCanCollide(boolean val) {
		this.canCollide = val;
	}
	
	public boolean isCollided() {
		return this.onGround;
	}
	
	public double getDistance(double x, double y, double z)
    {
        double d0 = this.posX - x;
        double d1 = this.posY - y;
        double d2 = this.posZ - z;
        return (double)MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
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
		}
		
		super.renderParticle(worldRendererIn, entityIn, partialTicks, rotationX,
				rotationZ, rotationYZ, rotationXY, rotationXZ);
	}

	public void renderParticleForShader(InstancedMeshParticle mesh, Transformation transformation, Matrix4fe viewMatrix, Entity entityIn,
                                        float partialTicks, float rotationX, float rotationZ,
                                        float rotationYZ, float rotationXY, float rotationXZ) {

        if (mesh.curBufferPos >= mesh.numInstances) return;

        //camera relative positions, for world position, remove the interpPos values
        float posX = (float) (this.prevPosX + (this.posX - this.prevPosX) * (double) partialTicks - Particle.interpPosX);
        float posY = (float) (this.prevPosY + (this.posY - this.prevPosY) * (double) partialTicks - Particle.interpPosY);
        float posZ = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * (double) partialTicks - Particle.interpPosZ);
        Vector3f pos = new Vector3f(posX, posY, posZ);

        Matrix4fe modelMatrix = transformation.buildModelMatrix(this, pos, partialTicks);

        //adjust to perspective and camera
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

	@Override
	public float getWindWeight() {
		return windWeight;
	}

	@Override
	public int getParticleDecayExtra() {
		return particleDecayExtra;
	}
    
    @Override
    public boolean shouldDisableDepth() {
    	return isTransparent;
    }
    
    public void setKillOnCollide(boolean val) {
    	this.killOnCollide = val;
    }
    
    //override to fix isCollided check
    @Override
    public void move(double x, double y, double z)
    {
        double yy = y;
        double xx = x;
        double zz = z;

        if (this.canCollide)
        {
            List<AxisAlignedBB> list = this.world.getCollisionBoxes((Entity)null, this.getBoundingBox().expand(x, y, z));

            for (AxisAlignedBB axisalignedbb : list)
            {
                y = axisalignedbb.calculateYOffset(this.getBoundingBox(), y);
            }

            this.setBoundingBox(this.getBoundingBox().offset(0.0D, y, 0.0D));

            for (AxisAlignedBB axisalignedbb1 : list)
            {
                x = axisalignedbb1.calculateXOffset(this.getBoundingBox(), x);
            }

            this.setBoundingBox(this.getBoundingBox().offset(x, 0.0D, 0.0D));

            for (AxisAlignedBB axisalignedbb2 : list)
            {
                z = axisalignedbb2.calculateZOffset(this.getBoundingBox(), z);
            }

            this.setBoundingBox(this.getBoundingBox().offset(0.0D, 0.0D, z));
        }
        else
        {
            this.setBoundingBox(this.getBoundingBox().offset(x, y, z));
        }

        this.resetPositionToBB();
        this.onGround = yy != y || xx != x || zz != z;
        this.isCollidedVerticallyDownwards = yy < y;
        if (xx != x)
        {
            this.motionX = 0.0D;
        }

        if (zz != z)
        {
            this.motionZ = 0.0D;
        }
    }
    
    public void setFacePlayer(boolean val) {
    	this.facePlayer = val;
    }
    
    public TextureAtlasSprite getParticleTexture() {
    	return this.particleTexture;
    }
    
    private boolean isVanillaMotionDampen() {
		return vanillaMotionDampen;
	}

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
        }
    }

    public void setKillWhenUnderTopmostBlock_ScanAheadRange(int killWhenUnderTopmostBlock_ScanAheadRange) {
        this.killWhenUnderTopmostBlock_ScanAheadRange = killWhenUnderTopmostBlock_ScanAheadRange;
    }
}
