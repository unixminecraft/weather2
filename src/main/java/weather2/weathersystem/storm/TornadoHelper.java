package weather2.weathersystem.storm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import CoroUtil.util.CoroUtilBlock;
import CoroUtil.util.Vec3;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.INpc;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import weather2.ClientConfigData;
import weather2.ClientTickHandler;
import weather2.config.ConfigMisc;
import weather2.config.ConfigTornado;
import weather2.entity.EntityMovingBlock;
import weather2.util.WeatherUtil;
import weather2.util.WeatherUtilBlock;
import weather2.util.WeatherUtilEntity;
import weather2.util.WeatherUtilSound;

public class TornadoHelper {
	
	private StormObject storm;
	
	//public int blockCount = 0;
	
	private long lastGrabTime = 0;
	private int tickGrabCount = 0;
	private int tryRipCount = 0;
    
	private int tornadoBaseSize = 5;
	private int grabDist = 100;
    
    /**
     * this update queue isnt perfect, created to reduce chunk updates on client, but not removing block right away messes with block rip logic:
     * - wont dig for blocks under this block until current is removed
     * - initially, entries were spam added as the block still existed, changed list to hashmap to allow for blockpos hash lookup before adding another entry
     * - entity creation relocated to queue processing to initially prevent entity spam, but with entry lookup, not needed, other issues like collision are now the reason why we still relocated entity creation to queue process
     */
	private HashMap<BlockPos, BlockUpdateSnapshot> listBlockUpdateQueue = new HashMap<BlockPos, BlockUpdateSnapshot>();
    private int queueProcessRate = 10;

    //for client player, for use of playing sounds
    private static boolean isOutsideCached = false;

	//static because its a shared list for the whole dimension
    private static HashMap<Integer, Long> flyingBlock_LastQueryTime = new HashMap<>();
    private static HashMap<Integer, Integer> flyingBlock_LastCount = new HashMap<>();

    private static GameProfile fakePlayerProfile = null;
    
    private static class BlockUpdateSnapshot {
    	private int dimID;
    	private IBlockState state;
    	private IBlockState statePrev;
		private BlockPos pos;
    	private boolean createEntityForBlockRemoval;

    	private BlockUpdateSnapshot(int dimID, IBlockState state, IBlockState statePrev, BlockPos pos, boolean createEntityForBlockRemoval) {
			this.dimID = dimID;
			this.state = state;
			this.statePrev = statePrev;
			this.pos = pos;
			this.createEntityForBlockRemoval = createEntityForBlockRemoval;
		}
    }
	
	public TornadoHelper(StormObject parStorm) {
		storm = parStorm;
	}
	
	private int getTornadoBaseSize() {
        int sizeChange = 10;
		if (storm.levelCurIntensityStage >= StormObject.STATE_STAGE5) {
        	return sizeChange * 9;
        } else if (storm.levelCurIntensityStage >= StormObject.STATE_STAGE4) {
        	return sizeChange * 7;
        } else if (storm.levelCurIntensityStage >= StormObject.STATE_STAGE3) {
        	return sizeChange * 5;
        } else if (storm.levelCurIntensityStage >= StormObject.STATE_STAGE2) {
        	return sizeChange * 4;
        } else if (storm.levelCurIntensityStage >= StormObject.STATE_STAGE1) {
        	return sizeChange * 3;
        } else if (storm.levelCurIntensityStage >= StormObject.STATE_FORMING) {
        	return sizeChange * 1;
        } else {
        	return 5;
        }
	}
	
	public void tick(World parWorld) {
		
		if (!parWorld.isRemote) {
			if (parWorld.getTotalWorldTime() % queueProcessRate == 0) {
				Iterator<BlockUpdateSnapshot> it = listBlockUpdateQueue.values().iterator();
				Random rand = new Random();
				while (it.hasNext()) {
					BlockUpdateSnapshot snapshot = it.next();
					World world = DimensionManager.getWorld(snapshot.dimID);
					if (world != null) {

						world.setBlockState(snapshot.pos, snapshot.state, 3);

						if (snapshot.createEntityForBlockRemoval) {
							EntityMovingBlock mBlock = new EntityMovingBlock(parWorld, snapshot.pos.getX(), snapshot.pos.getY(), snapshot.pos.getZ(), snapshot.statePrev, storm);
							double speed = 1D;
							mBlock.motionX += (rand.nextDouble() - rand.nextDouble()) * speed;
							mBlock.motionZ += (rand.nextDouble() - rand.nextDouble()) * speed;
							mBlock.motionY = 1D;
							parWorld.spawnEntity(mBlock);
						}
					}
				}
				listBlockUpdateQueue.clear();
			}
		}
		
		if (storm == null) return;
		
		boolean seesLight = false;
        tickGrabCount = 0;
        tryRipCount = 0;
        int tryRipMax = 300;
		int firesPerTickMax = 1;
        tornadoBaseSize = getTornadoBaseSize();
        
        if (storm.stormType == StormObject.TYPE_WATER) {
        	tornadoBaseSize *= 3;
        }
        
        forceRotate(parWorld);
        
        Random rand = new Random();
        
        int spawnYOffset = (int) storm.posBaseFormationPos.yCoord;

        if (!parWorld.isRemote && (ConfigTornado.Storm_Tornado_grabBlocks || storm.isFirenado)/*getStorm().grabsBlocks*/)
        {
            int yStart = 00;
            int yEnd = (int)storm.pos.yCoord/* + 72*/;
            int yInc = 1;
            Biome bgb = parWorld.getBiome(new BlockPos(MathHelper.floor(storm.pos.xCoord), 0, MathHelper.floor(storm.pos.zCoord)));
        	
        	if (bgb != null && (bgb.getBaseHeight()/* + bgb.getHeightVariation()*/ <= 0.7 || storm.isFirenado)) {
        		
	            for (int i = yStart; i < yEnd; i += yInc)
	            {
	                int YRand = i;
	                int ii = YRand / 4;
	
	                if (i > 20 && rand.nextInt(2) != 0)
	                {
	                    continue;
	                }
	
	                if (tryRipCount > tryRipMax)
	                {
	                    break;
	                }
	                
	                
	                int extraTry = (int) ((storm.levelCurIntensityStage+1 - StormObject.levelStormIntensityFormingStartVal) * 5);
	                int loopAmount = 5 + ii + extraTry;
	                
	                if (storm.stormType == StormObject.TYPE_WATER) {
	                	loopAmount = 1 + ii/2;
	                }
	
	                for (int k = 0; k < loopAmount; k++)
	                {
	                    if (tryRipCount > tryRipMax)
	                    {
	                        break;
	                    }
	
	                    int tryY = (int)(spawnYOffset + YRand - 1.5D);
	
	                    if (tryY > 255)
	                    {
	                        tryY = 255;
	                    }
	                    int tryX = (int)storm.pos.xCoord + rand.nextInt(tornadoBaseSize + (ii)) - ((tornadoBaseSize / 2) + (ii / 2));
	                    int tryZ = (int)storm.pos.zCoord + rand.nextInt(tornadoBaseSize + (ii)) - ((tornadoBaseSize / 2) + (ii / 2));
	
	                    double d0 = storm.pos.xCoord - tryX;
	                    double d2 = storm.pos.zCoord - tryZ;
	                    double dist = (double)MathHelper.sqrt(d0 * d0 + d2 * d2);
	                    BlockPos pos = new BlockPos(tryX, tryY, tryZ);
	                    
	                    if (dist < tornadoBaseSize/2 + ii/2 && tryRipCount < tryRipMax)
	                    {
	                    	
	                    	IBlockState state = parWorld.getBlockState(pos);
	                        Block blockID = state.getBlock();
	                        
	                        boolean performed = false;
	
	                        if (canGrab(parWorld, state, pos))
	                        {
	                            tryRipCount++;
	                            seesLight = tryRip(parWorld, tryX, tryY, tryZ);
	                            
	                            performed = seesLight;
	                        }
	                        
	                        if (!performed && ConfigTornado.Storm_Tornado_RefinedGrabRules) {
	                        	if (blockID == Blocks.GRASS) {
	                        		if (!listBlockUpdateQueue.containsKey(pos)) {
	                        			listBlockUpdateQueue.put(pos, new BlockUpdateSnapshot(parWorld.provider.getDimension(), Blocks.DIRT.getDefaultState(), state, pos, false));
	                        		}
	                        		
	                        	}
	                        }
	                    }
	                }
	            }
	                for (int k = 0; k < 10; k++)
	                {
	                	int randSize = 40;

							randSize = 10;
	                	
	                    int tryX = (int)storm.pos.xCoord + rand.nextInt(randSize) - randSize/2;
	                    int tryY = (int)spawnYOffset - 2 + rand.nextInt(8);
	                    int tryZ = (int)storm.pos.zCoord + rand.nextInt(randSize) - randSize/2;
	
	                    double d0 = storm.pos.xCoord - tryX;
	                    double d2 = storm.pos.zCoord - tryZ;
	                    double dist = (double)MathHelper.sqrt(d0 * d0 + d2 * d2);
	                    
	                    if (dist < tornadoBaseSize/2 + randSize/2 && tryRipCount < tryRipMax)
	                    {
	                    	BlockPos pos = new BlockPos(tryX, tryY, tryZ);
	                    	IBlockState state = parWorld.getBlockState(pos);
	                        state.getBlock();

							if (canGrab(parWorld, state, pos))
							{
								tryRipCount++;
								tryRip(parWorld, tryX, tryY, tryZ);
							}

	

	                    }
	                }
        	}
        }
        else
        {
            seesLight = true;
        }

        if (Math.abs((spawnYOffset - storm.pos.yCoord)) > 5)
        {
            seesLight = true;
        }

		if (!parWorld.isRemote && storm.isFirenado) {
        	if (storm.levelCurIntensityStage >= StormObject.STATE_STAGE1)
			for (int i = 0; i < firesPerTickMax; i++) {
				BlockPos posUp = new BlockPos(storm.posGround.xCoord, storm.posGround.yCoord + rand.nextInt(30), storm.posGround.zCoord);
				IBlockState state = parWorld.getBlockState(posUp);
				if (CoroUtilBlock.isAir(state.getBlock())) {
					EntityMovingBlock mBlock = new EntityMovingBlock(parWorld, posUp.getX(), posUp.getY(), posUp.getZ(), Blocks.FIRE.getDefaultState(), storm);
					mBlock.metadata = 15;
					double speed = 2D;
					mBlock.motionX += (rand.nextDouble() - rand.nextDouble()) * speed;
					mBlock.motionZ += (rand.nextDouble() - rand.nextDouble()) * speed;
					mBlock.motionY = 1D;
					mBlock.mode = 0;
					parWorld.spawnEntity(mBlock);
				}
			}


			int randSize = 10;

			int tryX = (int)storm.pos.xCoord + rand.nextInt(randSize) - randSize/2;

			int tryZ = (int)storm.pos.zCoord + rand.nextInt(randSize) - randSize/2;
			int tryY = parWorld.getHeight(tryX, tryZ) - 1;

			double d0 = storm.pos.xCoord - tryX;
			double d2 = storm.pos.zCoord - tryZ;
			double dist = (double)MathHelper.sqrt(d0 * d0 + d2 * d2);

			if (dist < tornadoBaseSize/2 + randSize/2 && tryRipCount < tryRipMax) {
				BlockPos pos = new BlockPos(tryX, tryY, tryZ);
				Block block = parWorld.getBlockState(pos).getBlock();
				BlockPos posUp = new BlockPos(tryX, tryY+1, tryZ);
				Block blockUp = parWorld.getBlockState(posUp).getBlock();

				if (!CoroUtilBlock.isAir(block) && CoroUtilBlock.isAir(blockUp))
				{
					parWorld.setBlockState(posUp, Blocks.FIRE.getDefaultState());
				}
			}
		}
	}

	private boolean tryRip(World parWorld, int tryX, int tryY, int tryZ/*, boolean notify*/)
    {
        BlockPos pos = new BlockPos(tryX, tryY, tryZ);
		if (listBlockUpdateQueue.containsKey(pos)) {
			return true;
		}
        
        if (!ConfigTornado.Storm_Tornado_grabBlocks) return true;

        boolean seesLight = false;
        IBlockState state = parWorld.getBlockState(pos);
        Block blockID = state.getBlock();
        if ((((WeatherUtilBlock.getPrecipitationHeightSafe(parWorld, new BlockPos(tryX, 0, tryZ)).getY() - 1 == tryY) ||
		WeatherUtilBlock.getPrecipitationHeightSafe(parWorld, new BlockPos(tryX + 1, 0, tryZ)).getY() - 1 < tryY ||
		WeatherUtilBlock.getPrecipitationHeightSafe(parWorld, new BlockPos(tryX, 0, tryZ + 1)).getY() - 1 < tryY ||
		WeatherUtilBlock.getPrecipitationHeightSafe(parWorld, new BlockPos(tryX - 1, 0, tryZ)).getY() - 1 < tryY ||
		WeatherUtilBlock.getPrecipitationHeightSafe(parWorld, new BlockPos(tryX, 0, tryZ - 1)).getY() - 1 < tryY))) {

        	int blockCount = getBlockCountForDim(parWorld);

			//old per storm blockCount seems glitched... lets use a global we cache count of
            if (parWorld.isBlockLoaded(new BlockPos(storm.pos.xCoord, 128, storm.pos.zCoord)) &&
				lastGrabTime < System.currentTimeMillis() &&
				tickGrabCount < ConfigTornado.Storm_Tornado_maxBlocksGrabbedPerTick) {

                lastGrabTime = System.currentTimeMillis() - 5;

                if (blockID != Blocks.SNOW)
                {
                	boolean playerClose = parWorld.getClosestPlayer(storm.posBaseFormationPos.xCoord, storm.posBaseFormationPos.yCoord, storm.posBaseFormationPos.zCoord, 140, false) != null;
                    if (playerClose) {
	                    tickGrabCount++;
	                    seesLight = true;
                    }

					if (WeatherUtil.shouldRemoveBlock(blockID))
					{
						boolean shouldEntityify = blockCount <= ConfigTornado.Storm_Tornado_maxFlyingEntityBlocks;
						listBlockUpdateQueue.put(pos, new BlockUpdateSnapshot(parWorld.provider.getDimension(), Blocks.AIR.getDefaultState(), state, pos, playerClose && shouldEntityify));
					}
                }
				if (blockID == Blocks.GLASS)
				{
					parWorld.playSound(null, new BlockPos(tryX, tryY, tryZ), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.AMBIENT, 5.0F, 1.0F);
				}
            }
        }

        return seesLight;
    }

	private boolean canGrab(World parWorld, IBlockState state, BlockPos pos)
    {
        if (!CoroUtilBlock.isAir(state.getBlock()) && state.getBlock() != Blocks.FIRE)
        {
        	return canGrabEventCheck(parWorld, state, pos);
        }

        return false;
    }

	private boolean canGrabEventCheck(World world, IBlockState state, BlockPos pos) {
    	if (!ConfigMisc.blockBreakingInvokesCancellableEvent) return true;
    	if (world instanceof WorldServer) {
			if (fakePlayerProfile == null) {
				fakePlayerProfile = new GameProfile(UUID.fromString("1396b887-2570-4948-86e9-0633d1d22946"), "weather2FakePlayer");
			}
			BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world, pos, state, FakePlayerFactory.get((WorldServer) world, fakePlayerProfile));
			MinecraftForge.EVENT_BUS.post(event);
			return !event.isCanceled();
		} else {
    		return false;
		}
	}

	private boolean canGrabEntity(Entity ent) {
		if (ent.world.isRemote) {
			return canGrabEntityClient(ent);
		} else {
			if (ent instanceof EntityPlayer) {
				if (ConfigTornado.Storm_Tornado_grabPlayer) {
					return true;
				} else {
					return false;
				}
			} else {
				if (ConfigTornado.Storm_Tornado_grabPlayersOnly) {
					return false;
				}
				if (ent instanceof INpc) {
					return ConfigTornado.Storm_Tornado_grabVillagers;
				}
				if (ent instanceof EntityItem) {
					return ConfigTornado.Storm_Tornado_grabItems;
				}
				if (ent instanceof IMob) {
					return ConfigTornado.Storm_Tornado_grabMobs;
				}
				if (ent instanceof EntityAnimal) {
					return ConfigTornado.Storm_Tornado_grabAnimals;
				}
			}
			//for moving blocks, other non livings
			return true;
		}

	}

	@SideOnly(Side.CLIENT)
	private boolean canGrabEntityClient(Entity ent) {
		ClientConfigData clientConfig = ClientTickHandler.clientConfigData;
		if (ent instanceof EntityPlayer) {
			if (clientConfig.Storm_Tornado_grabPlayer) {
				return true;
			} else {
				return false;
			}
		} else {
			if (clientConfig.Storm_Tornado_grabPlayersOnly) {
				return false;
			}
			if (ent instanceof INpc) {
				return clientConfig.Storm_Tornado_grabVillagers;
			}
			if (ent instanceof EntityItem) {
				return clientConfig.Storm_Tornado_grabItems;
			}
			if (ent instanceof IMob) {
				return clientConfig.Storm_Tornado_grabMobs;
			}
			if (ent instanceof EntityAnimal) {
				return clientConfig.Storm_Tornado_grabAnimals;
			}
		}
		//for moving blocks, other non livings
		return true;
	}
	
	private boolean forceRotate(World parWorld/*Entity entity*/)
    {
        double dist = grabDist;
        AxisAlignedBB aabb = new AxisAlignedBB(storm.pos.xCoord, storm.currentTopYBlock, storm.pos.zCoord, storm.pos.xCoord, storm.currentTopYBlock, storm.pos.zCoord);
        aabb = aabb.grow(dist, this.storm.maxHeight * 3, dist);
        List<Entity> list = parWorld.getEntitiesWithinAABB(Entity.class, aabb);
        boolean foundEnt = false;
        if (list != null)
        {
            for (int i = 0; i < list.size(); i++)
            {
                Entity entity1 = (Entity)list.get(i);

                if (canGrabEntity(entity1)) {
					if (getDistanceXZ(storm.posBaseFormationPos, entity1.posX, entity1.posY, entity1.posZ) < dist)
					{
						if ((entity1 instanceof EntityMovingBlock && !((EntityMovingBlock)entity1).collideFalling)/* || canEntityBeSeen(entity, entity1)*/)
						{
							storm.spinEntity(entity1);
							foundEnt = true;
						} else {
							if (entity1 instanceof EntityPlayer) {
								if (WeatherUtilEntity.isEntityOutside(entity1)) {
									storm.spinEntity(entity1);
									foundEnt = true;
								}
							} else if ((entity1 instanceof EntityLivingBase || entity1 instanceof EntityItem) && WeatherUtilEntity.isEntityOutside(entity1, true)) {

								storm.spinEntity(entity1);
								foundEnt = true;
							}
						}
					}
				}
            }
        }

        return foundEnt;
    }
    
	private double getDistanceXZ(Vec3 parVec, double var1, double var3, double var5)
    {
        double var7 = parVec.xCoord - var1;
        double var11 = parVec.zCoord - var5;
        return (double)MathHelper.sqrt(var7 * var7/* + var9 * var9*/ + var11 * var11);
    }
    
    @SideOnly(Side.CLIENT)
    public void soundUpdates(boolean playFarSound, boolean playNearSound)
    {
    	
    	Minecraft mc = FMLClientHandler.instance().getClient();
    	
        if (mc.player == null)
        {
            return;
        }

        //close sounds
        int far = 200;
        int close = 120;
        if (storm.stormType == StormObject.TYPE_WATER) {
        	close = 200;
        }
        Vec3 plPos = new Vec3(mc.player.posX, mc.player.posY, mc.player.posZ);
        
        double distToPlayer = this.storm.posGround.distanceTo(plPos);
        
        float volScaleFar = (float) ((far - distToPlayer) / far);
        float volScaleClose = (float) ((close - distToPlayer) / close);

        if (volScaleFar < 0F)
        {
            volScaleFar = 0.0F;
        }

        if (volScaleClose < 0F)
        {
            volScaleClose = 0.0F;
        }

        if (distToPlayer < close)
        {
        }
        else
        {
        }

        if (distToPlayer < far)
        {
            if (playFarSound) {
				if (mc.world.getTotalWorldTime() % 40 == 0) {
					isOutsideCached = WeatherUtilEntity.isPosOutside(mc.world,
							new Vec3(mc.player.getPosition().getX()+0.5F, mc.player.getPosition().getY()+0.5F, mc.player.getPosition().getZ()+0.5F));
				}
				if (isOutsideCached) {
					tryPlaySound(WeatherUtilSound.snd_wind_far, 2, mc.player, volScaleFar, far);
				}
			}
            if (playNearSound) tryPlaySound(WeatherUtilSound.snd_wind_close, 1, mc.player, volScaleClose, close);

            if (storm.levelCurIntensityStage >= StormObject.STATE_FORMING && storm.stormType == StormObject.TYPE_LAND)
            {
                tryPlaySound(WeatherUtilSound.snd_tornado_dmg_close, 0, mc.player, volScaleClose, close);
            }
        }
    }

    private boolean tryPlaySound(String[] sound, int arrIndex, Entity source, float vol, float parCutOffRange)
    {
        Random rand = new Random();

        if (WeatherUtilSound.soundTimer[arrIndex] <= System.currentTimeMillis())
        {
        	WeatherUtilSound.playMovingSound(storm, new StringBuilder().append("streaming." + sound[WeatherUtilSound.snd_rand[arrIndex]]).toString(), vol, 1.0F, parCutOffRange);
            int length = (Integer)WeatherUtilSound.soundToLength.get(sound[WeatherUtilSound.snd_rand[arrIndex]]);
            WeatherUtilSound.soundTimer[arrIndex] = System.currentTimeMillis() + length - 500L;
            WeatherUtilSound.snd_rand[arrIndex] = rand.nextInt(3);
        }

        return false;
    }

	/**
	 * Will abort out of counting if it hits the min amount required as per config
	 *
	 * @param world
	 * @return
	 */
    private static int getBlockCountForDim(World world) {
    	int queryRate = 20;
    	boolean perform = false;
		int flyingBlockCount = 0;
    	int dimID = world.provider.getDimension();
    	if (!flyingBlock_LastCount.containsKey(dimID) || !flyingBlock_LastQueryTime.containsKey(dimID)) {
			perform = true;
		} else if (flyingBlock_LastQueryTime.get(dimID) + queryRate < world.getTotalWorldTime()) {
			perform = true;
		}

		if (perform) {
			List<Entity> entities = world.loadedEntityList;
    		for (int i = 0; i < entities.size(); i++) {
    			Entity ent = entities.get(i);
				if (ent instanceof EntityMovingBlock) {
					flyingBlockCount++;

					if (flyingBlockCount > ConfigTornado.Storm_Tornado_maxFlyingEntityBlocks) {
						break;
					}
				}
			}

			flyingBlock_LastQueryTime.put(dimID, world.getTotalWorldTime());
    		flyingBlock_LastCount.put(dimID, flyingBlockCount);
		}

		return flyingBlock_LastCount.get(dimID);
	}

	public void cleanup() {
		listBlockUpdateQueue.clear();
		storm = null;
	}
}
