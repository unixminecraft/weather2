package weather2.entity;

import java.util.List;

import CoroUtil.api.weather.IWindHandler;
import CoroUtil.entity.EntityThrowableUsefull;
import CoroUtil.util.Vec3;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EntityIceBall extends EntityThrowableUsefull implements IWindHandler
{
	private int ticksInAir;
	
	@SideOnly(Side.CLIENT)
	private boolean hasDeathTicked;

	public EntityIceBall(World world)
	{
		super(world);
	}
	
	@Override
	public void onUpdate()
    {
		super.onUpdate();
		
		//gravity
		this.motionY -= 0.1F;
		
		if (this.motionY <= -3) {
			this.motionY = -3;
		}
		
		if (!this.world.isRemote)
        {
			
			ticksInAir++;
			
			if (this.isCollided) {
				setDead();
			}
			
			if (ticksInAir > 120) {
				setDead();
			}
			
			if (this.world.getClosestPlayer(this.posX, 50, this.posZ, 80, false) == null) {
				setDead();
			}
			
			if (isInWater()) {
				setDead();
			}
        }
    }
	
	@Override
	protected float getGravityVelocity() {
		return 0F;
	}
	
	@Override
	public RayTraceResult tickEntityCollision(Vec3 vec3, Vec3 vec31) {
		RayTraceResult movingobjectposition = null;
		
        Entity entity = null;
        List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox().grow(this.motionX, this.motionY, this.motionZ).grow(0.5D, 1D, 0.5D));
        EntityLivingBase entityliving = this.getThrower();

        for (int j = 0; j < list.size(); ++j)
        {
            Entity entity1 = (Entity)list.get(j);

            if (entity1.canBeCollidedWith() && (entity1 != entityliving && this.ticksInAir >= 4))
            {
                entity = entity1;
                break;
            }
        }

        if (entity != null)
        {
            movingobjectposition = new RayTraceResult(entity);
        }
        return movingobjectposition;
	}

	@Override
	protected void onImpact(RayTraceResult movingobjectposition)
	{
		
		if (movingobjectposition.entityHit != null)
		{
			if (!world.isRemote)
			{
				
				byte damage = 5;
				
				movingobjectposition.entityHit.attackEntityFrom(DamageSource.FALLING_BLOCK, damage);

				if (!world.isRemote) {
					setDead();
				}

			}
		}
		
		
		
		if (!world.isRemote) {
			world.playSound(null, new BlockPos(posX, posY, posZ), SoundEvents.BLOCK_STONE_STEP, SoundCategory.AMBIENT, 3F, 5F);//0.2F + world.rand.nextFloat() * 0.1F);
			setDead();
		} else {
			tickDeath();
		}
		
	}
	
	@Override
	public void setDead() {
		if (world.isRemote) tickDeath();
		super.setDead();
	}
	
	@SideOnly(Side.CLIENT)
	private void tickDeath() {
		if (!hasDeathTicked) {
			hasDeathTicked = true;
		}
	}

	@Override
	public float getWindWeight() {
		return 4;
	}

	@Override
	public int getParticleDecayExtra() {
		return 0;
	}
}
