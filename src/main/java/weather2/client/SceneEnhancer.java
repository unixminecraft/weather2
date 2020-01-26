package weather2.client;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import CoroUtil.api.weather.IWindHandler;
import CoroUtil.config.ConfigCoroUtil;
import CoroUtil.forge.CULog;
import CoroUtil.util.BlockCoord;
import CoroUtil.util.CoroUtilBlock;
import CoroUtil.util.CoroUtilEntOrParticle;
import CoroUtil.util.CoroUtilPhysics;
import CoroUtil.util.Vec3;
import extendedrenderer.EventHandler;
import extendedrenderer.particle.ParticleRegistry;
import extendedrenderer.particle.behavior.ParticleBehaviorSandstorm;
import extendedrenderer.particle.behavior.ParticleBehaviors;
import extendedrenderer.particle.entity.EntityRotFX;
import extendedrenderer.particle.entity.ParticleTexExtraRender;
import extendedrenderer.particle.entity.ParticleTexFX;
import extendedrenderer.particle.entity.ParticleTexLeafColor;
import extendedrenderer.render.RotatingParticleManager;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFlame;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import weather2.ClientTickHandler;
import weather2.SoundRegistry;
import weather2.client.entity.particle.EntityWaterfallFX;
import weather2.client.entity.particle.ParticleSandstorm;
import weather2.client.foliage.FoliageEnhancerShader;
import weather2.config.ConfigMisc;
import weather2.config.ConfigParticle;
import weather2.config.ConfigStorm;
import weather2.util.WeatherUtil;
import weather2.util.WeatherUtilBlock;
import weather2.util.WeatherUtilConfig;
import weather2.util.WeatherUtilEntity;
import weather2.util.WeatherUtilParticle;
import weather2.util.WeatherUtilSound;
import weather2.util.WindReader;
import weather2.weathersystem.WeatherManagerClient;
import weather2.weathersystem.storm.StormObject;
import weather2.weathersystem.storm.WeatherObjectSandstorm;
import weather2.weathersystem.wind.WindManager;

@SideOnly(Side.CLIENT)
public class SceneEnhancer implements Runnable {
	
	//this is for the thread we make
	private World lastWorldDetected = null;

	//used for acting on fire/smoke
	private static ParticleBehaviors pm;
	
	private static List<Particle> spawnQueueNormal = new ArrayList<Particle>();
	private static List<Particle> spawnQueue = new ArrayList<Particle>();
    
	private static long threadLastWorldTickTime;
	private static int lastTickFoundBlocks;
	private static long lastTickAmbient;
	private static long lastTickAmbientThreaded;
    
    //consider caching somehow without desyncing or overflowing
    //WE USE 0 TO MARK WATER, 1 TO MARK LEAVES
	private static ArrayList<BlockCoord> soundLocations = new ArrayList<BlockCoord>();
	private static HashMap<BlockCoord, Long> soundTimeLocations = new HashMap<BlockCoord, Long>();
    
	private static Block SOUNDMARKER_WATER = Blocks.WATER;
	private static Block SOUNDMARKER_LEAVES = Blocks.LEAVES;
    
	private static float curPrecipStr = 0F;
	private static float curPrecipStrTarget = 0F;
    
	private static float curOvercastStr = 0F;
	private static float curOvercastStrTarget = 0F;
    
    //sandstorm fog state
	private static double distToStormThreshold = 100;
    private static double distToStorm = distToStormThreshold + 50;
    public static float stormFogRed = 0;
    public static float stormFogGreen = 0;
    public static float stormFogBlue = 0;
    public static float stormFogRedOrig = 0;
    public static float stormFogGreenOrig = 0;
    public static float stormFogBlueOrig = 0;
    private static float stormFogDensity = 0;
    private static float stormFogDensityOrig = 0;

    public static float stormFogStart = 0;
    public static float stormFogEnd = 0;
    private static float stormFogStartOrig = 0;
    private static float stormFogEndOrig = 0;
    
    public static float stormFogStartClouds = 0;
    public static float stormFogEndClouds = 0;
    private static float stormFogStartCloudsOrig = 0;
    private static float stormFogEndCloudsOrig = 0;
    
    private static boolean needFogState = true;
    
    private static float scaleIntensitySmooth = 0F;
    
    private static float adjustAmountTarget = 0F;
    private static float adjustAmountSmooth = 0F;

	public static float adjustAmountTargetPocketSandOverride = 0F;
    
	private static boolean isPlayerOutside = true;

	private static ParticleBehaviorSandstorm particleBehavior;

	private int rainSoundCounter;

	private static List<BlockPos> listPosRandom = new ArrayList<>();

	public static List<EntityRotFX> testParticles = new ArrayList<>();

	public SceneEnhancer() {
		pm = new ParticleBehaviors(null);

		listPosRandom.clear();
		listPosRandom.add(new BlockPos(0, -1, 0));
		listPosRandom.add(new BlockPos(1, 0, 0));
		listPosRandom.add(new BlockPos(-1, 0, 0));
		listPosRandom.add(new BlockPos(0, 0, 1));
		listPosRandom.add(new BlockPos(0, 0, -1));
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				tickClientThreaded();
				Thread.sleep(ConfigMisc.Thread_Particle_Process_Delay);
			} catch (Throwable throwable) {
                throwable.printStackTrace();
            }
		}
	}

	//run from client side _mc_ thread
	public void tickClient() {
		if (!WeatherUtil.isPaused() && !ConfigMisc.Client_PotatoPC_Mode) {
			tryParticleSpawning();
			tickRainRates();
			tickParticlePrecipitation();
			trySoundPlaying();

			Minecraft mc = FMLClientHandler.instance().getClient();

			if (mc.world != null && lastWorldDetected != mc.world) {
				lastWorldDetected = mc.world;
				reset();
			}

			tryWind(mc.world);
			tickSandstorm();

			if (particleBehavior == null) {
				particleBehavior = new ParticleBehaviorSandstorm(null);
			}
			particleBehavior.tickUpdateList();

			if (ConfigCoroUtil.foliageShaders && EventHandler.queryUseOfShaders()) {
				if (!FoliageEnhancerShader.useThread) {
					if (mc.world.getTotalWorldTime() % 40 == 0) {
						FoliageEnhancerShader.tickClientThreaded();
					}
				}

				if (mc.world.getTotalWorldTime() % 5 == 0) {
					FoliageEnhancerShader.tickClientCloseToPlayer();
				}
			}

		}
	}
	
	//run from our newly created thread
	private void tickClientThreaded() {
		Minecraft mc = FMLClientHandler.instance().getClient();
		if (mc.world != null && mc.player != null && WeatherUtilConfig.listDimensionsWindEffects.contains(mc.world.provider.getDimension())) {
			profileSurroundings();
			tryAmbientSounds();
		}
	}
	
	@SuppressWarnings("deprecation")
	private synchronized void trySoundPlaying()
    {
		try {
			if (lastTickAmbient < System.currentTimeMillis()) {
	    		lastTickAmbient = System.currentTimeMillis() + 500;
	    		
	    		Minecraft mc = FMLClientHandler.instance().getClient();
	        	
	        	World worldRef = mc.world;
	        	EntityPlayer player = mc.player;
	        	
	        	int size = 32;
	            int curX = (int)player.posX;
	            int curY = (int)player.posY;
	            int curZ = (int)player.posZ;
	            
	            Random rand = new Random();
	            
	            //trim out distant sound locations, also update last time played
	            for (int i = 0; i < soundLocations.size(); i++) {
	            	
	            	BlockCoord cCor = soundLocations.get(i);
	            	
	            	if (Math.sqrt(cCor.getDistanceSquared(curX, curY, curZ)) > size) {
	            		soundLocations.remove(i--);
	            		soundTimeLocations.remove(cCor);
	            	} else {
	
	                    Block block = getBlock(worldRef, cCor.posX, cCor.posY, cCor.posZ);
	                    
	                    if (block == null || (block.getMaterial(block.getDefaultState()) != Material.WATER && block.getMaterial(block.getDefaultState()) != Material.LEAVES)) {
	                    	soundLocations.remove(i);
	                		soundTimeLocations.remove(cCor);
	                    } else {
	                    	
		            		long lastPlayTime = 0;
		            		
		            		
		            		
		            		if (soundTimeLocations.containsKey(cCor)) {
		            			lastPlayTime = soundTimeLocations.get(cCor);
		            		}
		            		
							if (lastPlayTime < System.currentTimeMillis()) {
								if (cCor.block == SOUNDMARKER_WATER) {
									soundTimeLocations.put(cCor, System.currentTimeMillis() + 2500 + rand.nextInt(50));
									mc.world.playSound(cCor.toBlockPos(), SoundRegistry.get("env.waterfall"), SoundCategory.AMBIENT, (float)ConfigMisc.volWaterfallScale, 0.75F + (rand.nextFloat() * 0.05F), false);
								} else if (cCor.block == SOUNDMARKER_LEAVES) {
									
										
									float windSpeed = WindReader.getWindSpeed(mc.world, new Vec3(cCor.posX, cCor.posY, cCor.posZ), WindReader.WindType.EVENT);
									if (windSpeed > 0.2F) {
										soundTimeLocations.put(cCor, System.currentTimeMillis() + 12000 + rand.nextInt(50));
										mc.world.playSound(cCor.toBlockPos(), SoundRegistry.get("env.wind_calmfade"), SoundCategory.AMBIENT, (float)(windSpeed * 4F * ConfigMisc.volWindTreesScale), 0.70F + (rand.nextFloat() * 0.1F), false);
									} else {
										windSpeed = WindReader.getWindSpeed(mc.world, new Vec3(cCor.posX, cCor.posY, cCor.posZ));
										if (mc.world.rand.nextInt(15) == 0) {
											soundTimeLocations.put(cCor, System.currentTimeMillis() + 12000 + rand.nextInt(50));
											mc.world.playSound(cCor.toBlockPos(), SoundRegistry.get("env.wind_calmfade"), SoundCategory.AMBIENT, (float)(windSpeed * 2F * ConfigMisc.volWindTreesScale), 0.70F + (rand.nextFloat() * 0.1F), false);
										}
									}
										
									
								}
								
							}
	                    }
	            	}
	            }
			}


			Minecraft mc = Minecraft.getMinecraft();

			float vanillaCutoff = 0.2F;
			float precipStrength = Math.abs(getRainStrengthAndControlVisuals(mc.player, ClientTickHandler.clientConfigData.overcastMode));

			//if less than vanilla sound playing amount
			if (precipStrength <= vanillaCutoff) {

				float volAmp = 0.2F + ((precipStrength / vanillaCutoff) * 0.8F);

				Random random = new Random();

				float f = mc.world.getRainStrength(1.0F);

				if (!mc.gameSettings.fancyGraphics) {
					f /= 2.0F;
				}

				if (f != 0.0F) {
					//random.setSeed((long)this.rendererUpdateCount * 312987231L);
					Entity entity = mc.getRenderViewEntity();
					World world = mc.world;
					BlockPos blockpos = new BlockPos(entity);
					double d0 = 0.0D;
					double d1 = 0.0D;
					double d2 = 0.0D;
					int j = 0;
					int k = 3;

					if (mc.gameSettings.particleSetting == 1) {
						k >>= 1;
					} else if (mc.gameSettings.particleSetting == 2) {
						k = 0;
					}

					for (int l = 0; l < k; ++l) {
						BlockPos blockpos1 = world.getPrecipitationHeight(blockpos.add(random.nextInt(10) - random.nextInt(10), 0, random.nextInt(10) - random.nextInt(10)));
						Biome biome = world.getBiome(blockpos1);
						BlockPos blockpos2 = blockpos1.down();
						IBlockState iblockstate = world.getBlockState(blockpos2);

						if (blockpos1.getY() <= blockpos.getY() + 10 && blockpos1.getY() >= blockpos.getY() - 10 && biome.canRain() && biome.getFloatTemperature(blockpos1) >= 0.15F) {
							double d3 = random.nextDouble();
							double d4 = random.nextDouble();
							AxisAlignedBB axisalignedbb = iblockstate.getBoundingBox(world, blockpos2);

							if (iblockstate.getMaterial() != Material.LAVA && iblockstate.getBlock() != Blocks.MAGMA) {
								if (iblockstate.getMaterial() != Material.AIR) {
									++j;

									if (random.nextInt(j) == 0) {
										d0 = (double) blockpos2.getX() + d3;
										d1 = (double) ((float) blockpos2.getY() + 0.1F) + axisalignedbb.maxY - 1.0D;
										d2 = (double) blockpos2.getZ() + d4;
									}

									mc.world.spawnParticle(EnumParticleTypes.WATER_DROP, (double) blockpos2.getX() + d3, (double) ((float) blockpos2.getY() + 0.1F) + axisalignedbb.maxY, (double) blockpos2.getZ() + d4, 0.0D, 0.0D, 0.0D, new int[0]);
								}
							} else {
								mc.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, (double) blockpos1.getX() + d3, (double) ((float) blockpos1.getY() + 0.1F) - axisalignedbb.minY, (double) blockpos1.getZ() + d4, 0.0D, 0.0D, 0.0D, new int[0]);
							}
						}
					}

					if (j > 0 && random.nextInt(3) < this.rainSoundCounter++) {
						this.rainSoundCounter = 0;

						if (d1 > (double) (blockpos.getY() + 1) && world.getPrecipitationHeight(blockpos).getY() > MathHelper.floor((float) blockpos.getY())) {
							mc.world.playSound(d0, d1, d2, SoundEvents.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, 0.1F * volAmp, 0.5F, false);
						} else {
							mc.world.playSound(d0, d1, d2, SoundEvents.WEATHER_RAIN, SoundCategory.WEATHER, 0.2F * volAmp, 1.0F, false);
						}
					}
				}
			}
		} catch (Exception ex) {
    		System.out.println("Weather2: Error handling sound play queue: ");
    		ex.printStackTrace();
    	}
    }
	
	//Threaded function
    @SuppressWarnings("deprecation")
	@SideOnly(Side.CLIENT)
    private static void tryAmbientSounds()
    {
    	Minecraft mc = FMLClientHandler.instance().getClient();
    	
    	World worldRef = mc.world;
    	EntityPlayer player = mc.player;
    	
    	if (lastTickAmbientThreaded < System.currentTimeMillis()) {
    		lastTickAmbientThreaded = System.currentTimeMillis() + 500;
    		
    		int size = 32;
            int hsize = size / 2;
            int curX = (int)player.posX;
            int curY = (int)player.posY;
            int curZ = (int)player.posZ;
            
    		for (int xx = curX - hsize; xx < curX + hsize; xx++)
            {
                for (int yy = curY - (hsize / 2); yy < curY + hsize; yy++)
                {
                    for (int zz = curZ - hsize; zz < curZ + hsize; zz++)
                    {
                        Block block = getBlock(worldRef, xx, yy, zz);
                        
                        if (block != null) {
                        	
                        	//Waterfall
                        	if (ConfigParticle.Wind_Particle_waterfall && ((block.getMaterial(block.getDefaultState()) == Material.WATER))) {
                            	
                            	int meta = getBlockMetadata(worldRef, xx, yy, zz);
                            	if ((meta & 8) != 0) {
                            		
                            		int bottomY = yy;
                            		int index = 0;
                            		
                            		//this scans to bottom till not water, kinda overkill? owell lets keep it, and also add rule if index > 4 (waterfall height of 4)
                            		while (yy-index > 0) {
                            			Block id2 = getBlock(worldRef, xx, yy-index, zz);
                            			if (id2 != null && !(id2.getMaterial(id2.getDefaultState()) == Material.WATER)) {
                            				break;
                            			}
                            			index++;
                            		}
                            		
                            		bottomY = yy-index+1;
                            		
                            		//check if +10 from here is water with right meta too
                            		int meta2 = getBlockMetadata(worldRef, xx, bottomY+10, zz);
                            		Block block2 = getBlock(worldRef, xx, bottomY+10, zz);;
                            		
                        			if (index >= 4 && (block2 != null && block2.getMaterial(block2.getDefaultState()) == Material.WATER && (meta2 & 8) != 0)) {
                        				boolean proxFail = false;
                        				for (int j = 0; j < soundLocations.size(); j++) {
                                			if (Math.sqrt(soundLocations.get(j).getDistanceSquared(xx, bottomY, zz)) < 5) {
                                				proxFail = true;
                                				break;
                                			}
                                		}
                        				
                        				if (!proxFail) {
                        					soundLocations.add(new BlockCoord(xx, bottomY, zz, SOUNDMARKER_WATER, 0));
                        				}
                        			}
                            	}
                            } else if (ConfigMisc.volWindTreesScale > 0 && ((block.getMaterial(block.getDefaultState()) == Material.LEAVES))) {
                            	boolean proxFail = false;
                				for (int j = 0; j < soundLocations.size(); j++) {
                        			if (Math.sqrt(soundLocations.get(j).getDistanceSquared(xx, yy, zz)) < 15) {
                        				proxFail = true;
                        				break;
                        			}
                        		}
                				
                				if (!proxFail) {
                					soundLocations.add(new BlockCoord(xx, yy, zz, SOUNDMARKER_LEAVES, 0));
                				}
                            }
                        }
                    }
                }
            }
    	}
    }

    private void reset() {
		
		lastWorldDetected.weatherEffects.clear();
		
		if (WeatherUtilParticle.fxLayers == null) {
			WeatherUtilParticle.getFXLayers();
		}
	}
	
    private void tickParticlePrecipitation() {

		if (ConfigParticle.Particle_RainSnow) {
			EntityPlayer entP = FMLClientHandler.instance().getClient().player;
			
			if (entP.posY >= StormObject.static_YPos_layer0) return;

			WeatherManagerClient weatherMan = ClientTickHandler.weatherManager;
			if (weatherMan == null) return;
			WindManager windMan = weatherMan.getWindManager();
			if (windMan == null) return;

			float curPrecipVal = getRainStrengthAndControlVisuals(entP);
			
			float maxPrecip = 0.5F;
			
			int precipitationHeight = entP.world.getPrecipitationHeight(new BlockPos(MathHelper.floor(entP.posX), 0, MathHelper.floor(entP.posZ))).getY();
			
			Biome biomegenbase = entP.world.getBiome(new BlockPos(MathHelper.floor(entP.posX), 0, MathHelper.floor(entP.posZ)));

			World world = entP.world;
			Random rand = entP.world.rand;

			double particleAmp = 1F;
			if (RotatingParticleManager.useShaders && ConfigCoroUtil.particleShaders) {
				particleAmp = ConfigMisc.shaderParticleRateAmplifier;
			}

			//check rules same way vanilla texture precip does
            if (biomegenbase != null && (biomegenbase.canRain() || biomegenbase.getEnableSnow()))
            {

            	float temperature = biomegenbase.getFloatTemperature(entP.getPosition());
	            //now absolute it for ez math
				curPrecipVal = Math.min(maxPrecip, Math.abs(curPrecipVal));

				curPrecipVal *= 1F;


				if (curPrecipVal > 0) {

					int spawnCount;
					int spawnNeed = (int)(curPrecipVal * 40F * ConfigParticle.Precipitation_Particle_effect_rate * particleAmp);
					int safetyCutout = 100;

					int extraRenderCount = 15;

					//attempt to fix the cluttering issue more noticable when barely anything spawning
					if (curPrecipVal < 0.1 && ConfigParticle.Precipitation_Particle_effect_rate > 0) {
						//swap rates
						int oldVal = extraRenderCount;
						extraRenderCount = spawnNeed;
						spawnNeed = oldVal;
					}

					//rain
					if (entP.world.getBiomeProvider().getTemperatureAtHeight(temperature, precipitationHeight) >= 0.15F) {

						spawnCount = 0;
						int spawnAreaSize = 20;

						if (spawnNeed > 0) {
							for (int i = 0; i < safetyCutout; i++) {
								BlockPos pos = new BlockPos(
										entP.posX + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
										entP.posY - 5 + rand.nextInt(25),
										entP.posZ + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));

								if (canPrecipitateAt(world, pos)/*world.isRainingAt(pos)*/) {
									ParticleTexExtraRender rain = new ParticleTexExtraRender(entP.world,
											pos.getX(),
											pos.getY(),
											pos.getZ(),
											0D, 0D, 0D, ParticleRegistry.rain_white);
									rain.setKillWhenUnderTopmostBlock(true);
									rain.setCanCollide(false);
									rain.killWhenUnderCameraAtLeast = 5;
									rain.setTicksFadeOutMaxOnDeath(5);
									rain.setDontRenderUnderTopmostBlock(true);
									rain.setExtraParticlesBaseAmount(extraRenderCount);
									rain.fastLight = true;
									rain.setSlantParticleToWind(true);
									rain.windWeight = 1F;

									if (!RotatingParticleManager.useShaders || !ConfigCoroUtil.particleShaders) {
										//old slanty rain way
										rain.setFacePlayer(true);
										rain.setSlantParticleToWind(true);
									} else {
										//new slanty rain way
										rain.setFacePlayer(false);
										rain.extraYRotation = rain.getWorld().rand.nextInt(360) - 180F;
									}

									rain.setScale(2F);
									rain.isTransparent = true;
									rain.setGravity(2.5F);
									rain.setMaxAge(50);
									rain.setTicksFadeInMax(5);
									rain.setAlphaF(0);
									rain.rotationYaw = rain.getWorld().rand.nextInt(360) - 180F;
									rain.setMotionY(-0.5D/*-5D - (entP.world.rand.nextInt(5) * -1D)*/);
									rain.spawnAsWeatherEffect();
									ClientTickHandler.weatherManager.addWeatheredParticle(rain);

									spawnCount++;
									if (spawnCount >= spawnNeed) {
										break;
									}
								}
							}
						}

						boolean groundSplash = ConfigParticle.Particle_Rain_GroundSplash;
						boolean downfall = ConfigParticle.Particle_Rain_DownfallSheet;

						//TODO: make ground splash and downfall use spawnNeed var style design

						spawnAreaSize = 40;
						//ground splash
						if (groundSplash == true && curPrecipVal > 0.15) {
							for (int i = 0; i < 30F * curPrecipVal * ConfigParticle.Precipitation_Particle_effect_rate * particleAmp * 4F; i++) {
								BlockPos pos = new BlockPos(
										entP.posX + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
										entP.posY - 5 + rand.nextInt(15),
										entP.posZ + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));


								//get the block on the topmost ground
								pos = world.getPrecipitationHeight(pos).down()/*.add(0, 1, 0)*/;

								IBlockState state = world.getBlockState(pos);
								AxisAlignedBB axisalignedbb = state.getBoundingBox(world, pos);

								if (pos.getDistance(MathHelper.floor(entP.posX), MathHelper.floor(entP.posY), MathHelper.floor(entP.posZ)) > spawnAreaSize / 2)
									continue;

								//block above topmost ground
								if (canPrecipitateAt(world, pos.up())/*world.isRainingAt(pos)*/) {
									ParticleTexFX rain = new ParticleTexFX(entP.world,
											pos.getX() + rand.nextFloat(),
											pos.getY() + 0.01D + axisalignedbb.maxY,
											pos.getZ() + rand.nextFloat(),
											0D, 0D, 0D, ParticleRegistry.cloud256_6);
									rain.setKillWhenUnderTopmostBlock(true);
									rain.setCanCollide(false);
									rain.killWhenUnderCameraAtLeast = 5;
									boolean upward = rand.nextBoolean();

									rain.windWeight = 20F;
									rain.setFacePlayer(upward);
									rain.setScale(3F + (rand.nextFloat() * 3F));
									rain.setMaxAge(15);
									rain.setGravity(-0.0F);
									rain.setTicksFadeInMax(0);
									rain.setAlphaF(0);
									rain.setTicksFadeOutMax(4);
									rain.renderOrder = 2;

									rain.rotationYaw = rain.getWorld().rand.nextInt(360) - 180F;
									rain.rotationPitch = 90;
									rain.setMotionY(0D);
									rain.setMotionX((rand.nextFloat() - 0.5F) * 0.01F);
									rain.setMotionZ((rand.nextFloat() - 0.5F) * 0.01F);
									rain.spawnAsWeatherEffect();
									ClientTickHandler.weatherManager.addWeatheredParticle(rain);
								}
							}
						}

						//if (true) return;

						spawnAreaSize = 20;
						//downfall - at just above 0.3 cause rainstorms lock at 0.3 but flicker a bit above and below
						if (downfall && curPrecipVal > 0.32) {

							int scanAheadRange = 0;
							//quick is outside check, prevent them spawning right near ground
							//and especially right above the roof so they have enough space to fade out
							//results in not seeing them through roofs
							if (entP.world.canBlockSeeSky(entP.getPosition())) {
								scanAheadRange = 3;
							} else {
								scanAheadRange = 10;
							}

							for (int i = 0; i < 2F * curPrecipVal * ConfigParticle.Precipitation_Particle_effect_rate; i++) {
								BlockPos pos = new BlockPos(
										entP.posX + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
										entP.posY + 5 + rand.nextInt(15),
										entP.posZ + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));

								if (entP.getDistanceSq(pos) < 10D * 10D) continue;
								if (canPrecipitateAt(world, pos.up(-scanAheadRange))) {
									ParticleTexExtraRender rain = new ParticleTexExtraRender(entP.world,
											pos.getX() + rand.nextFloat(),
											pos.getY() - 1 + 0.01D,
											pos.getZ() + rand.nextFloat(),
											0D, 0D, 0D, ParticleRegistry.downfall3);
									rain.setCanCollide(false);
									rain.killWhenUnderCameraAtLeast = 5;
									rain.setKillWhenUnderTopmostBlock(true);
									rain.setKillWhenUnderTopmostBlock_ScanAheadRange(scanAheadRange);
									rain.setTicksFadeOutMaxOnDeath(10);
									rain.noExtraParticles = true;
									rain.windWeight = 8F;
									rain.setFacePlayer(true);
									rain.setFacePlayer(false);
									rain.facePlayerYaw = true;
									rain.setScale(90F + (rand.nextFloat() * 3F));
									rain.setMaxAge(60);
									rain.setGravity(0.35F);
									//opted to leave the popin for rain, its not as bad as snow, and using fade in causes less rain visual overall
									rain.setTicksFadeInMax(20);
									rain.setAlphaF(0);
									rain.setTicksFadeOutMax(20);

									rain.rotationYaw = rain.getWorld().rand.nextInt(360) - 180F;
									rain.rotationPitch = 90;
									rain.rotationPitch = 0;
									rain.setMotionY(-0.3D);
									rain.setMotionX((rand.nextFloat() - 0.5F) * 0.01F);
									rain.setMotionZ((rand.nextFloat() - 0.5F) * 0.01F);
									rain.spawnAsWeatherEffect();
									ClientTickHandler.weatherManager.addWeatheredParticle(rain);
								}
							}
						}
					//snow
					} else {

						spawnCount = 0;
						spawnNeed = (int)(curPrecipVal * 40F * ConfigParticle.Precipitation_Particle_effect_rate * particleAmp);

						int spawnAreaSize = 50;

						if (spawnNeed > 0) {
							for (int i = 0; i < safetyCutout/*curPrecipVal * 20F * ConfigParticle.Precipitation_Particle_effect_rate*/; i++) {
								BlockPos pos = new BlockPos(
										entP.posX + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
										entP.posY - 5 + rand.nextInt(25),
										entP.posZ + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));

								if (canPrecipitateAt(world, pos)) {
									ParticleTexExtraRender snow = new ParticleTexExtraRender(entP.world, pos.getX(), pos.getY(), pos.getZ(),
											0D, 0D, 0D, ParticleRegistry.snow);

									snow.setCanCollide(false);
									snow.setKillWhenUnderTopmostBlock(true);
									snow.setTicksFadeOutMaxOnDeath(5);
									snow.setDontRenderUnderTopmostBlock(true);
									snow.setExtraParticlesBaseAmount(10);
									snow.killWhenFarFromCameraAtLeast = 20;

									snow.setMotionY(-0.1D);
									snow.setScale(1.3F);
									snow.setGravity(0.1F);
									snow.windWeight = 0.2F;
									snow.setMaxAge(40);
									snow.setFacePlayer(false);
									snow.setTicksFadeInMax(5);
									snow.setAlphaF(0);
									snow.setTicksFadeOutMax(5);
									snow.rotationYaw = snow.getWorld().rand.nextInt(360) - 180F;
									snow.spawnAsWeatherEffect();
									ClientTickHandler.weatherManager.addWeatheredParticle(snow);

									spawnCount++;
									if (spawnCount >= spawnNeed) {
										break;
									}
								}
							}
						}
					}
				}
            }
		}
	}

    private static boolean canPrecipitateAt(World world, BlockPos strikePosition)
	{
		if (world.getPrecipitationHeight(strikePosition).getY() > strikePosition.getY())
		{
			return false;
		}
		return true;
	}
	
	public static float getRainStrengthAndControlVisuals(EntityPlayer entP) {
		return getRainStrengthAndControlVisuals(entP, false);
	}

	/**
	 * Returns value between -1 to 1
	 * -1 is full on snow
	 * 1 is full on rain
	 * 0 is no precipitation
	 *
	 * also controls the client side raining and thundering values for vanilla
	 *
	 * @param entP
	 * @param forOvercast
	 * @return
	 */
	public static float getRainStrengthAndControlVisuals(EntityPlayer entP, boolean forOvercast) {
		
		Minecraft mc = FMLClientHandler.instance().getClient();
		
		double maxStormDist = 512 / 4 * 3;
		Vec3 plPos = new Vec3(entP.posX, StormObject.static_YPos_layer0, entP.posZ);
		StormObject storm = null;
		
		ClientTickHandler.checkClientWeather();
		
		storm = ClientTickHandler.weatherManager.getClosestStorm(plPos, maxStormDist, StormObject.STATE_FORMING, true);
	    boolean closeEnough = false;
	    double stormDist = 9999;
	    float tempAdj = 1F;

    	float sizeToUse = 0;
	    
	    float overcastModeMinPrecip = 0.23F;
		overcastModeMinPrecip = (float)ConfigStorm.Storm_Rain_Overcast_Amount;
	    
	    //evaluate if storms size is big enough to be over player
	    if (storm != null) {
	    	
	    	sizeToUse = storm.size;
	    	//extend overcast effect, using x2 for now since we cant cancel sound and ground particles, originally was 4x, then 3x, change to that for 1.7 if lex made change
	    	if (forOvercast) {
	    		sizeToUse *= 1F;
	    	}
	    	
	    	stormDist = storm.pos.distanceTo(plPos);
	    	if (sizeToUse > stormDist) {
	    		closeEnough = true;
	    	}
	    }
	    
	    if (closeEnough) {
	    	
		    
		    double stormIntensity = (sizeToUse - stormDist) / sizeToUse;
		    
		    tempAdj = storm.levelTemperature > 0 ? 1F : -1F;
		    
		    //limit plain rain clouds to light intensity
		    if (storm.levelCurIntensityStage == StormObject.STATE_NORMAL) {
		    	if (stormIntensity > 0.3) stormIntensity = 0.3;
		    }
		    
		    if (ConfigStorm.Storm_NoRainVisual) {
		    	stormIntensity = 0;
		    }

		    if (stormIntensity < overcastModeMinPrecip) {
		    	stormIntensity = overcastModeMinPrecip;
			}
		    
	    	mc.world.getWorldInfo().setRaining(true);
	    	mc.world.getWorldInfo().setThundering(true);
	    	if (forOvercast) {
	    		curOvercastStrTarget = (float) stormIntensity;
	    	} else {
	    		curPrecipStrTarget = (float) stormIntensity;
	    	}
	    } else {
	    	if (!ClientTickHandler.clientConfigData.overcastMode) {
		    	mc.world.getWorldInfo().setRaining(false);
		    	mc.world.getWorldInfo().setThundering(false);
		    	
		    	if (forOvercast) {
		    		curOvercastStrTarget = 0;
		    	} else {
		    		curPrecipStrTarget = 0;
		    	}
	    	} else {
	    		if (ClientTickHandler.weatherManager.isVanillaRainActiveOnServer) {
	    			mc.world.getWorldInfo().setRaining(true);
			    	mc.world.getWorldInfo().setThundering(true);
			    	
			    	if (forOvercast) {
			    		curOvercastStrTarget = overcastModeMinPrecip;
			    	} else {
			    		curPrecipStrTarget = overcastModeMinPrecip;
			    	}
	    		} else {
	    			if (forOvercast) {
			    		curOvercastStrTarget = 0;
			    	} else {
			    		curPrecipStrTarget = 0;
			    	}
	    		}
	    	}
	    }

	    if (forOvercast) {
			if (curOvercastStr < 0.001 && curOvercastStr > -0.001F) {
				return 0;
			} else {
				return curOvercastStr * tempAdj;
			}
	    } else {
			if (curPrecipStr < 0.001 && curPrecipStr > -0.001F) {
				return 0;
			} else {
				return curPrecipStr * tempAdj;
			}
	    }
	}

	private static void tickRainRates() {

		float rateChange = 0.0005F;

		if (curOvercastStr > curOvercastStrTarget) {
			curOvercastStr -= rateChange;
		} else if (curOvercastStr < curOvercastStrTarget) {
			curOvercastStr += rateChange;
		}

		if (curPrecipStr > curPrecipStrTarget) {
			curPrecipStr -= rateChange;
		} else if (curPrecipStr < curPrecipStrTarget) {
			curPrecipStr += rateChange;
		}
	}
	
	private synchronized void tryParticleSpawning()
    {
    	try {
	        for (int i = 0; i < spawnQueue.size(); i++)
	        {
	            Particle ent = spawnQueue.get(i);
	
	            if (ent != null) {
	            
		            if (ent instanceof EntityRotFX)
		            {
		                ((EntityRotFX) ent).spawnAsWeatherEffect();
		            }
		            ClientTickHandler.weatherManager.addWeatheredParticle(ent);
	            }
	        }
	        for (int i = 0; i < spawnQueueNormal.size(); i++)
	        {
	        	Particle ent = spawnQueueNormal.get(i);
	
	            if (ent != null) {
	            
	            	Minecraft.getMinecraft().effectRenderer.addEffect(ent);
	            }
	        }
    	} catch (Exception ex) {
    		CULog.err("Weather2: Error handling particle spawn queue: ");
    		ex.printStackTrace();
    	}

        spawnQueue.clear();
        spawnQueueNormal.clear();
    }
	
	@SuppressWarnings("deprecation")
	private void profileSurroundings()
    {
    	Minecraft mc = FMLClientHandler.instance().getClient();
    	World worldRef = lastWorldDetected;
    	EntityPlayer player = FMLClientHandler.instance().getClient().player;
        WeatherManagerClient manager = ClientTickHandler.weatherManager;
    	
        if (worldRef == null || player == null || manager == null || manager.windMan == null)
        {
        	try {
        		Thread.sleep(1000L);
        	} catch (Exception ex) {
        		ex.printStackTrace();
        	}
            return;
        }

        if (threadLastWorldTickTime == worldRef.getTotalWorldTime())
        {
            return;
        }

        threadLastWorldTickTime = worldRef.getTotalWorldTime();
        
        Random rand = new Random();
        
        //mining a tree causes leaves to fall
        int size = 40;
        int hsize = size / 2;
        int curX = (int)player.posX;
        int curY = (int)player.posY;
        int curZ = (int)player.posZ;
        
        float windStr = manager.windMan.getWindSpeedForPriority();
        if ((!ConfigParticle.Wind_Particle_leafs && !ConfigParticle.Wind_Particle_waterfall))
        {
            return;
        }

        //Wind requiring code goes below
        int spawnRate = (int)(30 / (windStr + 0.001));
        float lastBlockCount = lastTickFoundBlocks;
        
        float particleCreationRate = (float) ConfigParticle.Wind_Particle_effect_rate;
        float maxScaleSample = 15000;
        if (lastBlockCount > maxScaleSample) lastBlockCount = maxScaleSample-1;
        float scaleRate = (maxScaleSample - lastBlockCount) / maxScaleSample;
        
        spawnRate = (int) ((spawnRate / (scaleRate + 0.001F)) / (particleCreationRate + 0.001F));
        
        int BlockCountRate = (int)(((300 / scaleRate + 0.001F)) / (particleCreationRate + 0.001F)); 
        
        spawnRate *= (mc.gameSettings.particleSetting+1);
        BlockCountRate *= (mc.gameSettings.particleSetting+1);
        
        //since reducing threaded ticking to 200ms sleep, 1/4 rate, must decrease rand size
        spawnRate /= 2;
        
        //performance fix
        if (spawnRate < 40)
        {
            spawnRate = 40;
        }
        
        //performance fix
        if (BlockCountRate < 80) BlockCountRate = 80;
        if (BlockCountRate > 5000) BlockCountRate = 5000;
        lastTickFoundBlocks = 0;
		double particleAmp = 1F;
		if (RotatingParticleManager.useShaders && ConfigCoroUtil.particleShaders) {
			particleAmp = ConfigMisc.shaderParticleRateAmplifier * 2D;
		}

		spawnRate = (int)((double)spawnRate / particleAmp);
        for (int xx = curX - hsize; xx < curX + hsize; xx++)
        {
            for (int yy = curY - (hsize / 2); yy < curY + hsize; yy++)
            {
                for (int zz = curZ - hsize; zz < curZ + hsize; zz++)
                {
                            Block block = getBlock(worldRef, xx, yy, zz);
                            if (block != null && (block.getMaterial(block.getDefaultState()) == Material.LEAVES
									|| block.getMaterial(block.getDefaultState()) == Material.VINE ||
							block.getMaterial(block.getDefaultState()) == Material.PLANTS))
                            {
                            	
                            	lastTickFoundBlocks++;
                            	
                            	if (worldRef.rand.nextInt(spawnRate) == 0)
                                {
	                                if (ConfigParticle.Wind_Particle_leafs) {
										double relAdj = 0.70D;
										BlockPos pos = getRandomWorkingPos(worldRef, new BlockPos(xx, yy, zz));
										double xRand = 0;
										double yRand = 0;
										double zRand = 0;
	                                	if (pos != null) {
	                                		float particleAABB = 0.1F;
											float particleAABBAndBuffer = particleAABB + 0.05F;
											float invert = 1F - (particleAABBAndBuffer * 2F);

											if (pos.getY() != 0) {
												xRand = particleAABBAndBuffer + (rand.nextDouble() - 0.5D) * invert;
												zRand = particleAABBAndBuffer + (rand.nextDouble() - 0.5D) * invert;
											} else if (pos.getX() != 0) {
												yRand = particleAABBAndBuffer + (rand.nextDouble() - 0.5D) * invert;
												zRand = particleAABBAndBuffer + (rand.nextDouble() - 0.5D) * invert;
											} else if (pos.getZ() != 0) {
												yRand = particleAABBAndBuffer + (rand.nextDouble() - 0.5D) * invert;
												xRand = particleAABBAndBuffer + (rand.nextDouble() - 0.5D) * invert;
											}

											EntityRotFX var31 = new ParticleTexLeafColor(worldRef, xx, yy, zz, 0D, 0D, 0D, ParticleRegistry.leaf);
											var31.setPosition(xx + 0.5D + (pos.getX() * relAdj) + xRand,
													yy + 0.5D + (pos.getY() * relAdj) + yRand,
													zz + 0.5D + (pos.getZ() * relAdj) + zRand);
											var31.setPrevPosX(var31.posX);
											var31.setPrevPosY(var31.posY);
											var31.setPrevPosZ(var31.posZ);
											var31.setMotionX(0);
											var31.setMotionY(0);
											var31.setMotionZ(0);
											var31.setSize(particleAABB, particleAABB);
											var31.setGravity(0.05F);
											var31.setCanCollide(true);
											var31.setKillOnCollide(false);
											var31.collisionSpeedDampen = false;
											var31.killWhenUnderCameraAtLeast = 20;
											var31.killWhenFarFromCameraAtLeast = 20;
											var31.isTransparent = false;
											var31.rotationYaw = rand.nextInt(360);
											var31.rotationPitch = rand.nextInt(360);
											var31.updateQuaternion(null);

											spawnQueue.add(var31);
										}

	                                }
                                }
                            }
                            else if (ConfigParticle.Wind_Particle_waterfall && player.getDistance(xx,  yy, zz) < 16 && (block != null && block.getMaterial(block.getDefaultState()) == Material.WATER)) {
                            	
                            	int meta = getBlockMetadata(worldRef, xx, yy, zz);
                            	if ((meta & 8) != 0) {
                            		lastTickFoundBlocks += 70; //adding more to adjust for the rate 1 waterfall block spits out particles
                            		int chance = (int)(1+(((float)BlockCountRate)/120F));
                            		
                            		Block block2 = getBlock(worldRef, xx, yy-1, zz);
                            		int meta2 = getBlockMetadata(worldRef, xx, yy-1, zz);
                            		Block block3 = getBlock(worldRef, xx, yy+10, zz);
                                	if ((((block2 == null || block2.getMaterial(block2.getDefaultState()) != Material.WATER) || (meta2 & 8) == 0) && (block3 != null && block3.getMaterial(block3.getDefaultState()) == Material.WATER)) || worldRef.rand.nextInt(chance) == 0) {
                            		
	                            		float range = 0.5F;
	                            		
	                            		EntityRotFX waterP;
	                            		waterP = new EntityWaterfallFX(worldRef, (double)xx + 0.5F + ((rand.nextFloat() * range) - (range/2)), (double)yy + 0.5F + ((rand.nextFloat() * range) - (range/2)), (double)zz + 0.5F + ((rand.nextFloat() * range) - (range/2)), 0D, 0D, 0D, 6D, 2);
                            			if (((block2 == null || block2.getMaterial(block2.getDefaultState()) != Material.WATER) || (meta2 & 8) == 0) && (block3 != null && block3.getMaterial(block3.getDefaultState()) == Material.WATER)) {
                            				
                            				range = 2F;
                            				float speed = 0.2F;
                            				
                            				for (int i = 0; i < 10; i++) {
                            					if (worldRef.rand.nextInt(chance / 2) == 0) {
                            						waterP = new EntityWaterfallFX(worldRef, 
    	                            						(double)xx + 0.5F + ((rand.nextFloat() * range) - (range/2)), 
    	                            						(double)yy + 0.7F + ((rand.nextFloat() * range) - (range/2)), 
    	                            						(double)zz + 0.5F + ((rand.nextFloat() * range) - (range/2)),
    	                            						((rand.nextFloat() * speed) - (speed/2)),
    	                            						((rand.nextFloat() * speed) - (speed/2)),
    	                            						((rand.nextFloat() * speed) - (speed/2)),
    	                            						2D, 3);
    	                            				waterP.setMotionY(4.5F);
    	                            				spawnQueueNormal.add(waterP);
                            					}
	                            				
                            				}
                            			} else {
                            				waterP = new EntityWaterfallFX(worldRef, 
                            						(double)xx + 0.5F + ((rand.nextFloat() * range) - (range/2)), 
                            						(double)yy + 0.5F + ((rand.nextFloat() * range) - (range/2)), 
                            						(double)zz + 0.5F + ((rand.nextFloat() * range) - (range/2)), 0D, 0D, 0D, 6D, 2);
                            				
                            				waterP.setMotionY(0.5F);
                            				
                            				spawnQueueNormal.add(waterP);
                            			}
                                	}
                            	}
                            	
                            } else if (ConfigParticle.Wind_Particle_fire && (block != null && block == Blocks.FIRE/*block.getMaterial() == Material.fire*/)) {
                            	lastTickFoundBlocks++;
                            	if (worldRef.rand.nextInt(Math.max(1, (spawnRate / 100))) == 0) {
                            		double speed = 0.15D;
                                	EntityRotFX entityfx = pm.spawnNewParticleIconFX(worldRef, ParticleRegistry.smoke, xx + rand.nextDouble(), yy + 0.2D + rand.nextDouble() * 0.2D, zz + rand.nextDouble(), (rand.nextDouble() - rand.nextDouble()) * speed, 0.03D, (rand.nextDouble() - rand.nextDouble()) * speed);//pm.spawnNewParticleWindFX(worldRef, ParticleRegistry.smoke, xx + rand.nextDouble(), yy + 0.2D + rand.nextDouble() * 0.2D, zz + rand.nextDouble(), (rand.nextDouble() - rand.nextDouble()) * speed, 0.03D, (rand.nextDouble() - rand.nextDouble()) * speed);
                                	ParticleBehaviors.setParticleRandoms(entityfx, true, true);
                                	ParticleBehaviors.setParticleFire(entityfx);
                                	entityfx.setMaxAge(100+rand.nextInt(300));
                                	spawnQueueNormal.add(entityfx);
                            	}
                            }
                }
            }
        }
    }

	/**
	 * Returns the successful relative position
	 *
	 * @param world
	 * @param posOrigin
	 * @return
	 */
	private static BlockPos getRandomWorkingPos(World world, BlockPos posOrigin) {
		Collections.shuffle(listPosRandom);
		for (BlockPos posRel : listPosRandom) {
			Block blockCheck = getBlock(world, posOrigin.add(posRel));

			if (blockCheck != null && CoroUtilBlock.isAir(blockCheck)) {
				return posRel;
			}
		}

		return null;
	}
	
	@SideOnly(Side.CLIENT)
	private static void tryWind(World world)
    {
		
		Minecraft mc = FMLClientHandler.instance().getClient();
		EntityPlayer player = mc.player;

        if (player == null)
        {
            return;
        }

        WeatherManagerClient weatherMan = ClientTickHandler.weatherManager;
        if (weatherMan == null) return;
        WindManager windMan = weatherMan.getWindManager();
        if (windMan == null) return;
        
        Random rand = new Random();
        
        //Weather Effects
		for (int i = 0; i < ClientTickHandler.weatherManager.listWeatherEffectedParticles.size(); i++) {

			Particle particle = ClientTickHandler.weatherManager.listWeatherEffectedParticles.get(i);

			if (!particle.isAlive()) {
				ClientTickHandler.weatherManager.listWeatherEffectedParticles.remove(i--);
				continue;
			}

			if (ClientTickHandler.weatherManager.windMan.getWindSpeedForPriority() >= 0.10) {

            	if (particle instanceof EntityRotFX)
				{

					EntityRotFX entity1 = (EntityRotFX) particle;

					if ((WeatherUtilBlock.getPrecipitationHeightSafe(world, new BlockPos(MathHelper.floor(entity1.getPosX()), 0, MathHelper.floor(entity1.getPosZ()))).getY() - 1 < (int)entity1.getPosY() + 1) || (entity1 instanceof ParticleTexFX))
					{
						if (entity1 instanceof IWindHandler) {
							if (((IWindHandler)entity1).getParticleDecayExtra() > 0 && WeatherUtilParticle.getParticleAge(entity1) % 2 == 0)
							{
								WeatherUtilParticle.setParticleAge(entity1, WeatherUtilParticle.getParticleAge(entity1) + ((IWindHandler)entity1).getParticleDecayExtra());
							}
						}
						else if (WeatherUtilParticle.getParticleAge(entity1) % 2 == 0)
						{
							WeatherUtilParticle.setParticleAge(entity1, WeatherUtilParticle.getParticleAge(entity1) + 1);
						}

						if ((entity1 instanceof ParticleTexFX) && ((ParticleTexFX)entity1).getParticleTexture() == ParticleRegistry.leaf/*((ParticleTexFX)entity1).getParticleTextureIndex() == WeatherUtilParticle.effLeafID*/)
						{
							if (entity1.getMotionX() < 0.01F && entity1.getMotionZ() < 0.01F)
							{
								entity1.setMotionY(entity1.getMotionY() + rand.nextDouble() * 0.02 * ((ParticleTexFX) entity1).particleGravity);
							}

							entity1.setMotionY(entity1.getMotionY() - 0.01F * ((ParticleTexFX) entity1).particleGravity);

						}
					}

					windMan.applyWindForceNew(entity1, 1F/20F, 0.5F);
				}
            }
        }
        
        //Particles
        if (WeatherUtilParticle.fxLayers != null && windMan.getWindSpeedForPriority() >= 0.10)
        {
        	//Built in particles
            for (int layer = 0; layer < WeatherUtilParticle.fxLayers.length; layer++)
            {
                for (int i = 0; i < WeatherUtilParticle.fxLayers[layer].length; i++)
                {
                	for (Particle entity1 : WeatherUtilParticle.fxLayers[layer][i])
                    {
	                    if (ConfigParticle.Particle_VanillaAndWeatherOnly) {
	                    	String className = entity1.getClass().getName();
	                    	if (className.contains("net.minecraft.") || className.contains("weather2.")) {
	                    		
	                    	} else {
	                    		continue;
	                    	}
	                    }
	
	                    if ((WeatherUtilBlock.getPrecipitationHeightSafe(world, new BlockPos(MathHelper.floor(CoroUtilEntOrParticle.getPosX(entity1)), 0, MathHelper.floor(CoroUtilEntOrParticle.getPosZ(entity1)))).getY() - 1 < (int)CoroUtilEntOrParticle.getPosY(entity1) + 1) || (entity1 instanceof ParticleTexFX))
	                    {
	                        if ((entity1 instanceof ParticleFlame))
	                        {
	                        	if (windMan.getWindSpeedForPriority() >= 0.20) {
	                        		entity1.particleAge += 1;
								}
	                        }
	                        else if (entity1 instanceof IWindHandler) {
	                        	if (((IWindHandler)entity1).getParticleDecayExtra() > 0 && WeatherUtilParticle.getParticleAge(entity1) % 2 == 0)
	                            {
	                        		entity1.particleAge += ((IWindHandler)entity1).getParticleDecayExtra();
	                            }
	                        }
	
	                        //rustle!
	                        if (!(entity1 instanceof EntityWaterfallFX)) {
		                        if (CoroUtilEntOrParticle.getMotionX(entity1) < 0.01F && CoroUtilEntOrParticle.getMotionZ(entity1) < 0.01F)
		                        {
		                            CoroUtilEntOrParticle.setMotionY(entity1, CoroUtilEntOrParticle.getMotionY(entity1) + rand.nextDouble() * 0.02);
		                        }
	                        }
	                        windMan.applyWindForceNew(entity1, 1F/20F, 0.5F);
	                    }
                    }
                }
            }
        }
    }
	
	//Thread safe functions

	@SideOnly(Side.CLIENT)
	private static Block getBlock(World parWorld, BlockPos pos)
	{
		return getBlock(parWorld, pos.getX(), pos.getY(), pos.getZ());
	}

    @SideOnly(Side.CLIENT)
    private static Block getBlock(World parWorld, int x, int y, int z)
    {
        try
        {
            if (!parWorld.isBlockLoaded(new BlockPos(x, 0, z)))
            {
                return null;
            }

            return parWorld.getBlockState(new BlockPos(x, y, z)).getBlock();
        }
        catch (Exception ex)
        {
            return null;
        }
    }
    
    @SideOnly(Side.CLIENT)
    private static int getBlockMetadata(World parWorld, int x, int y, int z)
    {
        if (!parWorld.isBlockLoaded(new BlockPos(x, 0, z)))
        {
            return 0;
        }

        IBlockState state = parWorld.getBlockState(new BlockPos(x, y, z));
        return state.getBlock().getMetaFromState(state);
    }
    
    /**
     * Manages transitioning fog densities and color from current vanilla settings to our desired settings, and vice versa
     */
    private static void tickSandstorm() {

		if (adjustAmountTargetPocketSandOverride > 0) {
			adjustAmountTargetPocketSandOverride -= 0.01F;
		}

    	Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        World world = mc.world;
    	Vec3 posPlayer = new Vec3(mc.player.posX, 0/*mc.player.posY*/, mc.player.posZ);
    	WeatherObjectSandstorm sandstorm = ClientTickHandler.weatherManager.getClosestSandstormByIntensity(posPlayer);
        WindManager windMan = ClientTickHandler.weatherManager.getWindManager();
    	float scaleIntensityTarget = 0F;
    	if (sandstorm != null) {

			if (mc.world.getTotalWorldTime() % 40 == 0) {
				isPlayerOutside = WeatherUtilEntity.isEntityOutside(mc.player);
			}


    		scaleIntensityTarget = sandstorm.getSandstormScale();
    		List<Vec3> points = sandstorm.getSandstormAsShape();
    		boolean inStorm = CoroUtilPhysics.isInConvexShape(posPlayer, points);
        	if (inStorm) {
        		distToStorm = 0;
        	} else {
        		distToStorm = CoroUtilPhysics.getDistanceToShape(posPlayer, points);
        	}
    	} else {
    		distToStorm = distToStormThreshold + 10;
    	}
    	
    	scaleIntensitySmooth = adjVal(scaleIntensitySmooth, scaleIntensityTarget, 0.01F);

    	adjustAmountTarget = 1F - (float) ((distToStorm) / distToStormThreshold);
    	adjustAmountTarget *= 2F * scaleIntensitySmooth * (isPlayerOutside ? 1F : 0.5F);

		//use override if needed
		boolean pocketSandOverride = false;
		if (adjustAmountTarget < adjustAmountTargetPocketSandOverride) {
			adjustAmountTarget = adjustAmountTargetPocketSandOverride;
			pocketSandOverride = true;
		}
    	
    	if (adjustAmountTarget < 0F) adjustAmountTarget = 0F;
    	if (adjustAmountTarget > 1F) adjustAmountTarget = 1F;

        float sunBrightness = mc.world.getSunBrightness(1F) * 1F;

		if (!pocketSandOverride) {
			if (adjustAmountSmooth < adjustAmountTarget) {
				adjustAmountSmooth = adjVal(adjustAmountSmooth, adjustAmountTarget, 0.003F);
			} else {
				adjustAmountSmooth = adjVal(adjustAmountSmooth, adjustAmountTarget, 0.002F);
			}
		} else {
			adjustAmountSmooth = adjVal(adjustAmountSmooth, adjustAmountTarget, 0.02F);
		}

        EventHandler.sandstormFogAmount = adjustAmountSmooth;

    	
    	if (adjustAmountSmooth > 0) {

            //TODO: remove fetching of colors from this now that we dynamically track that
    		if (needFogState) {
    			
    			try {
    				Object fogState = ObfuscationReflectionHelper.getPrivateValue(GlStateManager.class, null, "field_179155_g");
    				Class<?> innerClass = Class.forName("net.minecraft.client.renderer.GlStateManager$FogState");
    				Field fieldDensity = null;
    				Field fieldStart = null;
    				Field fieldEnd = null;
    				try {
    					fieldDensity = innerClass.getField("field_179048_c");
    					fieldDensity.setAccessible(true);
    					fieldStart = innerClass.getField("field_179045_d");
    					fieldStart.setAccessible(true);
    					fieldEnd = innerClass.getField("field_179046_e");
    					fieldEnd.setAccessible(true);
					} catch (Exception e) {
						//dev env mode
						fieldDensity = innerClass.getField("density");
						fieldDensity.setAccessible(true);
						fieldStart = innerClass.getField("start");
    					fieldStart.setAccessible(true);
    					fieldEnd = innerClass.getField("end");
    					fieldEnd.setAccessible(true);
					}
    				stormFogDensity = fieldDensity.getFloat(fogState);
    				
    				stormFogStart = fieldStart.getFloat(fogState);
    				stormFogEnd = fieldEnd.getFloat(fogState);
    				
    				stormFogStartClouds = 0;
    				stormFogEndClouds = 192;
    				
    				
    				stormFogStartOrig = stormFogStart;
    				stormFogEndOrig = stormFogEnd;
    				stormFogStartCloudsOrig = stormFogStartClouds;
    				stormFogEndCloudsOrig = stormFogEndClouds;
    				
    				stormFogDensityOrig = stormFogDensity;
    				
				} catch (Exception e) {
					e.printStackTrace();
				}
    			needFogState = false;
    		}
    		
    		//new dynamic adjusting
    		stormFogRed = stormFogRedOrig + (-(stormFogRedOrig - (0.7F * sunBrightness)) * adjustAmountSmooth);
    		stormFogGreen = stormFogGreenOrig + (-(stormFogGreenOrig - (0.5F * sunBrightness)) * adjustAmountSmooth);
    		stormFogBlue = stormFogBlueOrig + (-(stormFogBlueOrig - (0.25F * sunBrightness)) * adjustAmountSmooth);
    		
    		stormFogDensity = stormFogDensityOrig + (-(stormFogDensityOrig - 0.02F) * adjustAmountSmooth);
    		
    		stormFogStart = stormFogStartOrig + (-(stormFogStartOrig - 0F) * adjustAmountSmooth);
    		stormFogEnd = stormFogEndOrig + (-(stormFogEndOrig - 7F) * adjustAmountSmooth);
    		stormFogStartClouds = stormFogStartCloudsOrig + (-(stormFogStartCloudsOrig - 0F) * adjustAmountSmooth);
    		stormFogEndClouds = stormFogEndCloudsOrig + (-(stormFogEndCloudsOrig - 20F) * adjustAmountSmooth);
    	} else {
    		needFogState = true;
    	}

    	//enhance the scene further with particles around player, check for sandstorm to account for pocket sand modifying adjustAmountTarget
        if (adjustAmountSmooth > 0.75F && sandstorm != null) {

            Vec3 windForce = windMan.getWindForce();

            Random rand = mc.world.rand;
            int spawnAreaSize = 80;

			double sandstormParticleRateDebris = ConfigParticle.Sandstorm_Particle_Debris_effect_rate;
			double sandstormParticleRateDust = ConfigParticle.Sandstorm_Particle_Dust_effect_rate;

            float adjustAmountSmooth75 = (adjustAmountSmooth * 8F) - 7F;

			//extra dust
            for (int i = 0; i < ((float)30 * adjustAmountSmooth75 * sandstormParticleRateDust)/*adjustAmountSmooth * 20F * ConfigMisc.Particle_Precipitation_effect_rate*/; i++) {

                BlockPos pos = new BlockPos(
                        player.posX + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
                        player.posY - 2 + rand.nextInt(10),
                        player.posZ + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));



                if (canPrecipitateAt(world, pos)) {
                    TextureAtlasSprite sprite = ParticleRegistry.cloud256;

                    ParticleSandstorm part = new ParticleSandstorm(world, pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            0, 0, 0, sprite);
                    particleBehavior.initParticle(part);

                    part.setMotionX(windForce.xCoord);
                    part.setMotionZ(windForce.zCoord);

                    part.setFacePlayer(false);
                    part.isTransparent = true;
                    part.rotationYaw = (float)rand.nextInt(360);
                    part.rotationPitch = (float)rand.nextInt(360);
                    part.setMaxAge(40);
                    part.setGravity(0.09F);
                    part.setAlphaF(0F);
                    float brightnessMulti = 1F - (rand.nextFloat() * 0.5F);
                    part.setRBGColorF(0.65F * brightnessMulti, 0.6F * brightnessMulti, 0.3F * brightnessMulti);
                    part.setScale(40);
                    part.aboveGroundHeight = 0.2D;

                    part.setKillOnCollide(true);

                    part.windWeight = 1F;

                    particleBehavior.particles.add(part);
                    ClientTickHandler.weatherManager.addWeatheredParticle(part);
                    part.spawnAsWeatherEffect();


                }
            }

            //tumbleweed
            for (int i = 0; i < ((float)1 * adjustAmountSmooth75 * sandstormParticleRateDebris)/*adjustAmountSmooth * 20F * ConfigMisc.Particle_Precipitation_effect_rate*/; i++) {

                BlockPos pos = new BlockPos(
                        player.posX + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
                        player.posY - 2 + rand.nextInt(10),
                        player.posZ + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));



                if (canPrecipitateAt(world, pos)) {
                    TextureAtlasSprite sprite = ParticleRegistry.tumbleweed;

                    ParticleSandstorm part = new ParticleSandstorm(world, pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            0, 0, 0, sprite);
                    particleBehavior.initParticle(part);

                    part.setMotionX(windForce.xCoord);
                    part.setMotionZ(windForce.zCoord);

                    part.setFacePlayer(true);
                    part.isTransparent = true;
                    part.rotationYaw = (float)rand.nextInt(360);
                    part.rotationPitch = (float)rand.nextInt(360);
                    part.setMaxAge(80);
                    part.setGravity(0.3F);
                    part.setAlphaF(0F);
                    float brightnessMulti = 1F - (rand.nextFloat() * 0.2F);
                    part.setRBGColorF(1F * brightnessMulti, 1F * brightnessMulti, 1F * brightnessMulti);
                    part.setScale(8);
                    part.aboveGroundHeight = 0.5D;
                    part.collisionSpeedDampen = false;
                    part.bounceSpeed = 0.03D;
                    part.bounceSpeedAhead = 0.03D;

                    part.setKillOnCollide(false);

                    part.windWeight = 1F;

                    particleBehavior.particles.add(part);
                    ClientTickHandler.weatherManager.addWeatheredParticle(part);
                    part.spawnAsWeatherEffect();


                }
            }

            //debris
            for (int i = 0; i < ((float)8 * adjustAmountSmooth75 * sandstormParticleRateDebris)/*adjustAmountSmooth * 20F * ConfigMisc.Particle_Precipitation_effect_rate*/; i++) {
                BlockPos pos = new BlockPos(
                        player.posX + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
                        player.posY - 2 + rand.nextInt(10),
                        player.posZ + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));



                if (canPrecipitateAt(world, pos)) {
                    TextureAtlasSprite sprite = null;
                    int tex = rand.nextInt(3);
                    if (tex == 0) {
                        sprite = ParticleRegistry.debris_1;
                    } else if (tex == 1) {
                        sprite = ParticleRegistry.debris_2;
                    } else if (tex == 2) {
                        sprite = ParticleRegistry.debris_3;
                    }

                    ParticleSandstorm part = new ParticleSandstorm(world, pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            0, 0, 0, sprite);
                    particleBehavior.initParticle(part);

                    part.setMotionX(windForce.xCoord);
                    part.setMotionZ(windForce.zCoord);

                    part.setFacePlayer(false);
                    part.spinFast = true;
                    part.isTransparent = true;
                    part.rotationYaw = (float)rand.nextInt(360);
                    part.rotationPitch = (float)rand.nextInt(360);

                    part.setMaxAge(80);
                    part.setGravity(0.3F);
                    part.setAlphaF(0F);
                    float brightnessMulti = 1F - (rand.nextFloat() * 0.5F);
                    part.setRBGColorF(1F * brightnessMulti, 1F * brightnessMulti, 1F * brightnessMulti);
                    part.setScale(8);
                    part.aboveGroundHeight = 0.5D;
                    part.collisionSpeedDampen = false;
                    part.bounceSpeed = 0.03D;
                    part.bounceSpeedAhead = 0.03D;

                    part.setKillOnCollide(false);

                    part.windWeight = 1F;

                    particleBehavior.particles.add(part);
                    ClientTickHandler.weatherManager.addWeatheredParticle(part);
                    part.spawnAsWeatherEffect();


                }
            }
        }



		tickSandstormSound();
    }

	/**
	 *
	 */
    private static void tickSandstormSound() {
		/**
		 * dist + storm intensity
		 * 0F - 1F
		 *
		 * 0 = low
		 * 0.33 = med
		 * 0.66 = high
		 *
		 * static sound volume, keep at player
		 */

		Minecraft mc = Minecraft.getMinecraft();
		if (adjustAmountSmooth > 0) {
			if (adjustAmountSmooth < 0.33F) {
				tryPlayPlayerLockedSound(WeatherUtilSound.snd_sandstorm_low, 5, mc.player, 1F);
			} else if (adjustAmountSmooth < 0.66F) {
				tryPlayPlayerLockedSound(WeatherUtilSound.snd_sandstorm_med, 4, mc.player, 1F);
			} else {
				tryPlayPlayerLockedSound(WeatherUtilSound.snd_sandstorm_high, 3, mc.player, 1F);
			}
		}
	}

    private static boolean tryPlayPlayerLockedSound(String[] sound, int arrIndex, Entity source, float vol)
	{
		Random rand = new Random();

		if (WeatherUtilSound.soundTimer[arrIndex] <= System.currentTimeMillis())
		{

			String soundStr = sound[WeatherUtilSound.snd_rand[arrIndex]];

			WeatherUtilSound.playPlayerLockedSound(new Vec3(source.getPositionVector()), new StringBuilder().append("streaming." + soundStr).toString(), vol, 1.0F);

			int length = WeatherUtilSound.soundToLength.get(soundStr);
			//-500L, for blending
			WeatherUtilSound.soundTimer[arrIndex] = System.currentTimeMillis() + length - 500L;
			WeatherUtilSound.snd_rand[arrIndex] = rand.nextInt(sound.length);
		}

		return false;
	}
    
    public static boolean isFogOverridding() {
		Minecraft mc = Minecraft.getMinecraft();
		IBlockState iblockstate = ActiveRenderInfo.getBlockStateAtEntityViewpoint(mc.world, mc.getRenderViewEntity(), 1F);
		if (iblockstate.getMaterial().isLiquid()) return false;
    	return adjustAmountSmooth > 0;
    }

    public static void renderTick(TickEvent.RenderTickEvent event) {

		if (ConfigMisc.Client_PotatoPC_Mode) return;

		if (event.phase == TickEvent.Phase.START) {
			Minecraft mc = FMLClientHandler.instance().getClient();
			EntityPlayer entP = mc.player;
			if (entP != null) {
				float curRainStr = SceneEnhancer.getRainStrengthAndControlVisuals(entP, true);
				curRainStr = Math.abs(curRainStr);
				mc.world.setRainStrength(curRainStr);
			}
		}
	}
    
    private static float adjVal(float source, float target, float adj) {
        if (source < target) {
            source += adj;
            //fix over adjust
            if (source > target) {
                source = target;
            }
        } else if (source > target) {
            source -= adj;
            //fix over adjust
            if (source < target) {
                source = target;
            }
        }
        return source;
    }
}
