package extendedrenderer.particle.behavior;

import CoroUtil.util.Vec3;
import extendedrenderer.particle.entity.EntityRotFX;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

public class ParticleBehaviorFogGround extends ParticleBehaviors {

	//Externally updated variables, adjusting how templated behavior works
	public int curTick = 0;
	public int ticksMax = 1;
	
	public ParticleBehaviorFogGround(Vec3 source) {
		super(source);
	}
	
	public EntityRotFX initParticle(EntityRotFX particle) {
		super.initParticle(particle);
		
		//fog
		particle.rotationYaw = rand.nextInt(360);
		particle.rotationPitch = rand.nextInt(50)-rand.nextInt(50);
		
		//cloud
		particle.setMaxAge(450+rand.nextInt(10));
		float randFloat = (rand.nextFloat() * 0.6F);
		float baseBright = 0.7F;
		float finalBright = Math.min(1F, baseBright+randFloat);
		particle.setRBGColorF(finalBright, finalBright, finalBright);
		
		//location based color shift
		particle.brightness = 1F;
		particle.setAlphaF(1F);
		
		float sizeBase = (float) (30+(rand.nextDouble()*4));
		
		particle.setScale(sizeBase);
		particle.setCanCollide(true);
		
		particle.renderRange = 2048;
		
		particle.setFacePlayer(true);
		particle.setGravity(0.03F);
		
		return particle;
	}

	@Override
	public void tickUpdateAct(EntityRotFX particle) {
			
			if (!particle.isAlive()) {
				particles.remove(particle);
			} else {
				if (particle.getEntityId() % 2 == 0) {
					particle.rotationYaw -= 0.1;
				} else {
					particle.rotationYaw += 0.1;
				}
				
				float ticksFadeInMax = 50;
				float ticksFadeOutMax = 50;
				
				if (particle.getAge() < ticksFadeInMax) {
					particle.setAlphaF(particle.getAge() / ticksFadeInMax);
				} else if (particle.getAge() > particle.getMaxAge() - ticksFadeOutMax) {
					float count = particle.getAge() - (particle.getMaxAge() - ticksFadeOutMax);
					float val = (ticksFadeOutMax - (count)) / ticksFadeOutMax;
					particle.setAlphaF(val);
				}
				double moveSpeed = 0.001D;
				BlockPos pos = new BlockPos(particle.getPosX(), particle.getPosY() - 0.5D, particle.getPosZ());
				IBlockState state = particle.getWorld().getBlockState(pos);
				if (!state.getBlock().isAir(state, particle.world, pos)) {
					if (particle.motionY < 0.09D) {
						particle.motionY += 0.02D;
					}
				}
				
				if (particle.isCollided()) {
					particle.rotationYaw += 0.1;
				}
				
				particle.setMotionX(particle.getMotionX() - Math.sin(Math.toRadians((particle.rotationYaw + particle.getEntityId()) % 360)) * moveSpeed);
				particle.setMotionZ(particle.getMotionZ() + Math.cos(Math.toRadians((particle.rotationYaw + particle.getEntityId()) % 360)) * moveSpeed);
				
				double moveSpeedRand = 0.005D;
				
				particle.setMotionX(particle.getMotionX() + (rand.nextDouble() * moveSpeedRand - rand.nextDouble() * moveSpeedRand));
				particle.setMotionZ(particle.getMotionZ() + (rand.nextDouble() * moveSpeedRand - rand.nextDouble() * moveSpeedRand));
				
				//TEMPOFF
				particle.setScale(particle.getScale() - 0.1F);
				
				if (particle.spawnY != -1) {
					particle.setPosition(particle.getPosX(), particle.spawnY, particle.getPosZ());
				}
			}
	}
}
