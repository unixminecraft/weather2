package extendedrenderer.particle.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import CoroUtil.util.Vec3;
import extendedrenderer.particle.entity.EntityRotFX;
import extendedrenderer.particle.entity.ParticleTexFX;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ParticleBehaviors {

	public List<EntityRotFX> particles = new ArrayList<EntityRotFX>();
	public Vec3 coordSource;
	public Entity sourceEntity = null;
	public Random rand = new Random();
	
	//Visual tweaks
	public float rateDarken = 0.025F;
	public float rateBrighten = 0.010F;
	public float rateBrightenSlower = 0.003F;
	public float rateAlpha = 0.002F;
	public float rateScale = 0.1F;
	public int tickSmokifyTrigger = 40;
	
	public ParticleBehaviors(Vec3 source) {
		coordSource = source;
	}
	
	public void tickUpdateList() { //shouldnt be used, particles tick their own method, who removes it though?
		for (int i = 0; i < particles.size(); i++) {
			EntityRotFX particle = particles.get(i);
			
			if (!particle.isAlive()) {
				particles.remove(particle);
			} else {
				tickUpdate(particle);
			}
		}
	}
	
	private void tickUpdate(EntityRotFX particle) {
		
		if (sourceEntity != null) {
			coordSource = new Vec3(sourceEntity.posX, sourceEntity.posY, sourceEntity.posZ);
		}
		
		tickUpdateAct(particle);
	}
	
	//default is smoke effect, override for custom
	protected void tickUpdateAct(EntityRotFX particle) {
		
			
		double centerX = particle.getPosX();
		double centerZ = particle.getPosZ();
		
		if (coordSource != null) {
			centerX = coordSource.xCoord/* + 0.5D*/;
			centerZ = coordSource.zCoord/* + 0.5D*/;
		}
		
		double vecX = centerX - particle.getPosX();
		double vecZ = centerZ - particle.getPosZ();
		double distToCenter = Math.sqrt(vecX * vecX + vecZ * vecZ);
		double rotYaw = (float)(Math.atan2(vecZ, vecX) * 180.0D / Math.PI);
		double adjYaw = Math.min(360, 45+particle.getAge());
		
		rotYaw -= adjYaw;
		double speed = 0.1D;
		if (particle.getAge() < 25 && distToCenter > 0.05D) {
			particle.setMotionX(Math.cos(rotYaw * 0.017453D) * speed);
			particle.setMotionZ(Math.sin(rotYaw * 0.017453D) * speed);
		} else {
			double speed2 = 0.008D;
			
			double pSpeed = Math.sqrt(particle.getMotionX() * particle.getMotionX() + particle.getMotionZ() * particle.getMotionZ());
			
			//cheap air search code
			if (pSpeed < 0.2 && particle.getMotionY() < 0.01) {
				speed2 = 0.08D;
			}
			
			if (pSpeed < 0.002 && Math.abs(particle.getMotionY()) < 0.02) {
				particle.setMotionY(particle.getMotionY() - 0.15D);
			}
			
			particle.setMotionX(particle.getMotionX() + (rand.nextDouble() - rand.nextDouble()) * speed2);
			particle.setMotionZ(particle.getMotionZ() + (rand.nextDouble() - rand.nextDouble()) * speed2);
			
		}
		
		float brightnessShiftRate = rateDarken;
		
		int stateChangeTick = tickSmokifyTrigger;
		
		if (particle.getAge() < stateChangeTick) {
			particle.setGravity(-0.2F);
			particle.setRBGColorF(particle.getRedColorF() - brightnessShiftRate, particle.getGreenColorF() - brightnessShiftRate, particle.getBlueColorF() - brightnessShiftRate);
		} else if (particle.getAge() == stateChangeTick) {
			particle.setRBGColorF(0,0,0);
		} else {
			brightnessShiftRate = rateBrighten;
			particle.setGravity(-0.05F);
			if (particle.getRedColorF() < 0.3F) {
				
			} else {
				brightnessShiftRate = rateBrightenSlower;
			}
			
			particle.setRBGColorF(particle.getRedColorF() + brightnessShiftRate, particle.getGreenColorF() + brightnessShiftRate, particle.getBlueColorF() + brightnessShiftRate);
			
			if (particle.getAlphaF() > 0) {
				particle.setAlphaF(particle.getAlphaF() - rateAlpha);
			} else {
				particle.setExpired();
			}
		}
		
		if (particle.getScale() < 8F) particle.setScale(particle.getScale() + rateScale);
	}
	
	public EntityRotFX spawnNewParticleIconFX(World world, TextureAtlasSprite icon, double x, double y, double z, double vecX, double vecY, double vecZ) {
		return spawnNewParticleIconFX(world, icon, x, y, z, vecX, vecY, vecZ, 0);
	}
	
	public EntityRotFX spawnNewParticleIconFX(World world, TextureAtlasSprite icon, double x, double y, double z, double vecX, double vecY, double vecZ, int renderOrder) {
		EntityRotFX entityfx = new ParticleTexFX(world, x, y, z, vecX, vecY, vecZ, icon);
		entityfx.pb = this;
		entityfx.renderOrder = renderOrder;
		return entityfx;
	}
	
	public EntityRotFX initParticle(EntityRotFX particle) {
		
		particle.setPrevPosX(particle.getPosX());
		particle.setPrevPosY(particle.getPosY());
		particle.setPrevPosZ(particle.getPosZ());
		
		//keep AABB small, very important to performance
		particle.setSize(0.01F, 0.01F);
		
		return particle;
	}
	
	public static EntityRotFX setParticleRandoms(EntityRotFX particle, boolean yaw, boolean pitch) {
		Random rand = new Random();
		if (yaw) particle.rotationYaw = rand.nextInt(360);
		if (pitch) particle.rotationPitch = rand.nextInt(360);
		return particle;
	}
	
	public static EntityRotFX setParticleFire(EntityRotFX particle) {
		Random rand = new Random();
		particle.setRBGColorF(0.6F + (rand.nextFloat() * 0.4F), 0.2F + (rand.nextFloat() * 0.2F), 0);
		particle.setScale(0.25F + 0.2F * rand.nextFloat());
		particle.brightness = 1F;
		particle.setSize(0.1F, 0.1F);
		particle.setAlphaF(0.6F);
		return particle;
	}
}
