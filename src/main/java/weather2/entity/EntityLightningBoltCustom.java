package weather2.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import CoroUtil.util.CoroUtilBlock;
import net.minecraft.block.BlockFire;
import net.minecraft.entity.effect.EntityWeatherEffect;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import weather2.config.ConfigStorm;

public class EntityLightningBoltCustom extends EntityWeatherEffect
{
    /**
     * Declares which state the lightning bolt is in. Whether it's in the air, hit the ground, etc.
     */
    private int lightningState;

    /**
     * Determines the time before the EntityLightningBolt is destroyed. It is a random integer decremented over time.
     */
    private int boltLivingTime;

    private int fireLifeTime = ConfigStorm.Lightning_lifetimeOfFire;
    private int fireChance = ConfigStorm.Lightning_OddsTo1OfFire;
    
    public List<Vec3d> listVec = new ArrayList<Vec3d>();
    
    private double curYaw = 0;

    public EntityLightningBoltCustom(World par1World, double par2, double par4, double par6)
    {
        super(par1World);
        this.setLocationAndAngles(par2, par4, par6, 0.0F, 0.0F);
        this.lightningState = 2;
        this.boltLivingTime = this.rand.nextInt(3) + 1;

        Random rand = new Random();
        
        //create lightning bolt parts
        double spread = 5;
        double spreadAngle = 15;
        
        //init
        if (listVec.size() == 0) {
        	curYaw = rand.nextDouble();
        	listVec.add(new Vec3d(this.posX, this.posY, this.posZ));
        } else {
        	curYaw += rand.nextDouble() * spreadAngle;
        	if (curYaw > 360) curYaw -= 360;
        	if (curYaw < 0) curYaw += 360; 
        }
        for (int i = 0; i < 1; i++) {
        	listVec.add(new Vec3d(rand.nextDouble() * spread - rand.nextDouble() * spread, rand.nextDouble() * spread - rand.nextDouble() * spread, rand.nextDouble() * spread - rand.nextDouble() * spread));
        }

        if (ConfigStorm.Lightning_StartsFires) {
            if (!par1World.isRemote && par1World.getGameRules().getBoolean("doFireTick") && (par1World.getDifficulty() == EnumDifficulty.NORMAL || par1World.getDifficulty() == EnumDifficulty.HARD) && par1World.isAreaLoaded(new BlockPos(MathHelper.floor(par2), MathHelper.floor(par4), MathHelper.floor(par6)), 10)) {
                int i = MathHelper.floor(par2);
                int j = MathHelper.floor(par4);
                int k = MathHelper.floor(par6);

                if (CoroUtilBlock.isAir(par1World.getBlockState(new BlockPos(i, j, k)).getBlock()) && Blocks.FIRE.canPlaceBlockAt(par1World, new BlockPos(i, j, k))) {
                    par1World.setBlockState(new BlockPos(i, j, k), Blocks.FIRE.getDefaultState().withProperty(BlockFire.AGE, fireLifeTime));
                }

                for (i = 0; i < 4; ++i) {
                    j = MathHelper.floor(par2) + this.rand.nextInt(3) - 1;
                    k = MathHelper.floor(par4) + this.rand.nextInt(3) - 1;
                    int l = MathHelper.floor(par6) + this.rand.nextInt(3) - 1;

                    if (CoroUtilBlock.isAir(par1World.getBlockState(new BlockPos(j, k, l)).getBlock()) && Blocks.FIRE.canPlaceBlockAt(par1World, new BlockPos(j, k, l))) {
                        par1World.setBlockState(new BlockPos(i, j, k), Blocks.FIRE.getDefaultState().withProperty(BlockFire.AGE, fireLifeTime));
                    }
                }
            }
        }
    }

    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate()
    {
        super.onUpdate();
        if (world.isRemote) {
	        if (this.lightningState == 2)
	        {
	            this.world.playSound(this.posX, this.posY, this.posZ, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.WEATHER, 64.0F, 0.8F + this.rand.nextFloat() * 0.2F, false);
	            this.world.playSound(this.posX, this.posY, this.posZ, SoundEvents.ENTITY_LIGHTNING_IMPACT, SoundCategory.WEATHER, 2.0F, 0.5F + this.rand.nextFloat() * 0.2F, false);
	        }
        }

        --this.lightningState;

        if (this.lightningState < 0)
        {
            if (this.boltLivingTime == 0)
            {
                this.setDead();
            }
            else if (this.lightningState < -this.rand.nextInt(10))
            {
                --this.boltLivingTime;
                this.lightningState = 1;

                if (!this.world.isRemote && rand.nextInt(fireChance) == 0 && this.world.getGameRules().getBoolean("doFireTick") && this.world.isAreaLoaded(new BlockPos(MathHelper.floor(this.posX), MathHelper.floor(this.posY), MathHelper.floor(this.posZ)), 10))
                {
                    int i = MathHelper.floor(this.posX);
                    int j = MathHelper.floor(this.posY);
                    int k = MathHelper.floor(this.posZ);

                    if (ConfigStorm.Lightning_StartsFires) {
                        if (CoroUtilBlock.isAir(world.getBlockState(new BlockPos(i, j, k)).getBlock()) && Blocks.FIRE.canPlaceBlockAt(world, new BlockPos(i, j, k))) {
                            world.setBlockState(new BlockPos(i, j, k), Blocks.FIRE.getDefaultState().withProperty(BlockFire.AGE, fireLifeTime), 3);
                        }
                    }
                }
            }
        }
    }

    protected void entityInit() {}

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {}

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {}
}
