package extendedrenderer.particle.behavior;

import CoroUtil.util.Vec3;
import extendedrenderer.particle.entity.EntityRotFX;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

public class ParticleBehaviorSandstorm extends ParticleBehaviors {

	//Externally updated variables, adjusting how templated behavior works
	public int curTick = 0;
	public int ticksMax = 1;
	
	public ParticleBehaviorSandstorm(Vec3 source) {
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
				//random rotation yaw adjustment
				if (particle.getEntityId() % 2 == 0) {
					particle.rotationYaw -= 0.1;
				} else {
					particle.rotationYaw += 0.1;
				}
				
				float ticksFadeInMax = 10;
				float ticksFadeOutMax = 10;
				
				//fade in and fade out near age edges
				if (particle.getAge() < ticksFadeInMax) {
					particle.setAlphaF(Math.min(1F, particle.getAge() / ticksFadeInMax));
				} else if (particle.getAge() > particle.getMaxAge() - ticksFadeOutMax) {
					float count = particle.getAge() - (particle.getMaxAge() - ticksFadeOutMax);
					float val = (ticksFadeOutMax - (count)) / ticksFadeOutMax;
					particle.setAlphaF(val);
				}
				
				//get pos a bit under particle
				BlockPos pos = new BlockPos(particle.getPosX(), particle.getPosY() - particle.aboveGroundHeight, particle.getPosZ());
				IBlockState state = particle.getWorld().getBlockState(pos);
				//if particle is near ground, push it up to keep from landing
				if (!state.getBlock().isAir(state, particle.world, pos)) {
					if (particle.motionY < particle.bounceSpeedMax) {
						particle.motionY += particle.bounceSpeed;
					}
				//check ahead for better flowing over large cliffs
				} else {
					double aheadMultiplier = 20D;
					BlockPos posAhead = new BlockPos(particle.getPosX() + (particle.getMotionX() * aheadMultiplier), particle.getPosY() - particle.aboveGroundHeight, particle.getPosZ() + (particle.getMotionZ() * aheadMultiplier));
					IBlockState stateAhead = particle.getWorld().getBlockState(posAhead);
					if (!stateAhead.getBlock().isAir(stateAhead, particle.world, posAhead)) {
						if (particle.motionY < particle.bounceSpeedMaxAhead) {
							particle.motionY += particle.bounceSpeedAhead;
						}
					}
				}
				double moveSpeedRand = 0.005D;
				
				particle.setMotionX(particle.getMotionX() + (rand.nextDouble() * moveSpeedRand - rand.nextDouble() * moveSpeedRand));
				particle.setMotionZ(particle.getMotionZ() + (rand.nextDouble() * moveSpeedRand - rand.nextDouble() * moveSpeedRand));
				if (particle.spawnY != -1) {
					particle.setPosition(particle.getPosX(), particle.spawnY, particle.getPosZ());
				}
			}
	}
}
