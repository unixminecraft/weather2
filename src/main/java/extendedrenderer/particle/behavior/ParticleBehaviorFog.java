package extendedrenderer.particle.behavior;

import CoroUtil.config.ConfigCoroUtil;
import CoroUtil.util.Vec3;
import extendedrenderer.particle.entity.EntityRotFX;

public class ParticleBehaviorFog extends ParticleBehaviors {

	//Externally updated variables, adjusting how templated behavior works
	public int curTick = 0;
	public int ticksMax = 1;
	
	public ParticleBehaviorFog(Vec3 source) {
		super(source);
	}
	
	public EntityRotFX initParticle(EntityRotFX particle) {
		super.initParticle(particle);
		
		//fog
		particle.rotationYaw = rand.nextInt(360);
		particle.rotationPitch = rand.nextInt(50)-rand.nextInt(50);
		
		//cloud
		particle.rotationYaw = rand.nextInt(360);
		particle.rotationPitch = -90+rand.nextInt(50)-rand.nextInt(50);


		
		particle.setMaxAge(650+rand.nextInt(10));
		particle.setGravity(0.01F);
		float randFloat = (rand.nextFloat() * 0.6F);
		float baseBright = 0.7F;
		float finalBright = Math.min(1F, baseBright+randFloat);
		particle.setRBGColorF(finalBright, finalBright, finalBright);
		
		//location based color shift
		particle.setScale(0.25F + 0.2F * rand.nextFloat());
		particle.brightness = 1F;
		particle.setAlphaF(0);
		
		float sizeBase = (float) (500+(rand.nextDouble()*40));

		//TEST
		if (ConfigCoroUtil.optimizedCloudRendering) {
			sizeBase += 500;
			particle.rotationPitch = -90 + rand.nextInt(5) - rand.nextInt(5);
			particle.setTicksFadeInMax(20);
			particle.setTicksFadeOutMax(20);
		}

		particle.setScale(sizeBase);
		particle.setCanCollide(true);
		
		particle.renderRange = 2048;
		
		return particle;
	}

	@Override
	public void tickUpdateAct(EntityRotFX particle) {
			if (!particle.isAlive()) {
				particles.remove(particle);
			} else {
				if (particle.getEntityId() % 2 == 0) {
					particle.rotationYaw -= 0.02;
				} else {
					particle.rotationYaw += 0.02;
				}
				
				float ticksFadeInMax = 50;
				float ticksFadeOutMax = 50;
				
				if (particle.getAge() < ticksFadeInMax) {
					particle.setAlphaF(particle.getAge() / ticksFadeInMax);
				} else if (particle.getAge() > particle.getMaxAge() - ticksFadeOutMax) {
					float count = particle.getAge() - (particle.getMaxAge() - ticksFadeOutMax);
					float val = (ticksFadeOutMax - (count)) / ticksFadeOutMax;
					particle.setAlphaF(val);
				} else {
				}
				double moveSpeed = 0.001D;
				if (particle.isCollided()) {
					particle.rotationYaw += 0.1;
				}
				
				particle.setMotionX(particle.getMotionX() - Math.sin(Math.toRadians((particle.rotationYaw + particle.getEntityId()) % 360)) * moveSpeed);
				particle.setMotionZ(particle.getMotionZ() + Math.cos(Math.toRadians((particle.rotationYaw + particle.getEntityId()) % 360)) * moveSpeed);
				
				double moveSpeedRand = 0.005D;
				
				particle.setMotionX(particle.getMotionX() + (rand.nextDouble() * moveSpeedRand - rand.nextDouble() * moveSpeedRand));
				particle.setMotionZ(particle.getMotionZ() + (rand.nextDouble() * moveSpeedRand - rand.nextDouble() * moveSpeedRand));
				
				particle.setScale(particle.getScale() - 0.1F);
				
				if (particle.spawnY != -1) {
					particle.setPosition(particle.getPosX(), particle.spawnY, particle.getPosZ());
				}
			}
	}
}
