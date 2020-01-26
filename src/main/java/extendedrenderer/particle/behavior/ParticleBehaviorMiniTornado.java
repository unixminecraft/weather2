package extendedrenderer.particle.behavior;

import CoroUtil.util.Vec3;
import extendedrenderer.particle.entity.EntityRotFX;

public class ParticleBehaviorMiniTornado extends ParticleBehaviors {

	//Externally updated variables, adjusting how templated behavior works
	public int curTick = 0;
	public int ticksMax = 1;
	
	public ParticleBehaviorMiniTornado(Vec3 source) {
		super(source);
	}
	
	public EntityRotFX initParticle(EntityRotFX particle) {
		super.initParticle(particle);
		
		particle.rotationYaw = rand.nextInt(360);
		particle.setMaxAge(1+rand.nextInt(10));
		particle.setGravity(0F);
		particle.setRBGColorF(72F/255F, 239F/255F, 8F/255F);
		//red
		particle.setRBGColorF(0.6F + (rand.nextFloat() * 0.4F), 0.2F + (rand.nextFloat() * 0.7F), 0);
		//green
		float greyScale = 0.5F + (rand.nextFloat() * 0.3F);
		particle.setRBGColorF(greyScale, greyScale, greyScale);
		
		particle.setScale(0.25F + 0.2F * rand.nextFloat());
		particle.brightness = 1F;
		particle.setScale(0.5F + rand.nextFloat() * 0.5F);
		particle.spawnY = (float) particle.getPosY();
		particle.setCanCollide(false);
		particle.isTransparent = false;
		particle.setMaxAge(100);
		
		return particle;
	}

	@Override
	public void tickUpdateAct(EntityRotFX particle) {
			
			if (!particle.isAlive()) {
				particles.remove(particle);
			} else {
				particle.setMotionX(0);
				particle.setMotionY(0);
				particle.setMotionZ(0);
				
				double x = particle.getPosX();
				double z = particle.getPosZ();

				double age = particle.getAge();
				double ageOffset = age + particle.getEntityId();
				
				double yAdj = age * 0.01D;
				
				double ageScale;
				
				double distFromCenter = 0.2D + (yAdj * 0.3D);
				
				ageScale = (Math.PI / 45) * ageOffset * 3D;
				double centerX = coordSource.xCoord;
				double centerZ = coordSource.zCoord;
				
				x = centerX + (Math.sin(ageScale) * distFromCenter);
				z = centerZ + (Math.cos(ageScale) * distFromCenter);
				
				particle.setPosition(x, coordSource.yCoord + yAdj, z);
				
				double var16 = centerX - x;
                double var18 = centerZ - z;
                particle.rotationYaw = (float)Math.toDegrees(Math.atan2(var18, var16)) + 90;
			}
	}
}
