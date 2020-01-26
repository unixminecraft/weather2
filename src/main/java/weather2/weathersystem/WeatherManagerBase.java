package weather2.weathersystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;

import CoroUtil.util.CoroUtilFile;
import CoroUtil.util.CoroUtilPhysics;
import CoroUtil.util.Vec3;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import weather2.ServerTickHandler;
import weather2.Weather;
import weather2.weathersystem.storm.EnumWeatherObjectType;
import weather2.weathersystem.storm.StormObject;
import weather2.weathersystem.storm.WeatherObject;
import weather2.weathersystem.storm.WeatherObjectSandstorm;
import weather2.weathersystem.wind.WindManager;

public class WeatherManagerBase {

	//shared stuff, stormfront list
	
	public int dim;
	
	//storms
	private List<WeatherObject> listStormObjects = new ArrayList<>();
	public HashMap<Long, WeatherObject> lookupStormObjectsByID = new HashMap<>();
	private HashMap<Integer, ArrayList<StormObject>> lookupStormObjectsByLayer = new HashMap<>();
	
	//wind
	public WindManager windMan;
	
	//for client only
	public boolean isVanillaRainActiveOnServer = false;
	public boolean isVanillaThunderActiveOnServer = false;
	public int vanillaRainTimeOnServer = 0;
	
	public long lastStormFormed = 0;

	public long lastSandstormFormed = 0;

	//0 = none, 1 = usual max overcast
	public float cloudIntensity = 1F;

	public WeatherManagerBase(int parDim) {
		dim = parDim;
		windMan = new WindManager(this);
		lookupStormObjectsByLayer.put(0, new ArrayList<>());
		lookupStormObjectsByLayer.put(1, new ArrayList<>());
		lookupStormObjectsByLayer.put(2, new ArrayList<>());
	}
	
	protected void reset() {
		for (int i = 0; i < getStormObjects().size(); i++) {
			WeatherObject so = getStormObjects().get(i);
			
			so.reset();
		}
		
		getStormObjects().clear();
		lookupStormObjectsByID.clear();
		try {
			for (int i = 0; i < lookupStormObjectsByLayer.size(); i++) {
				lookupStormObjectsByLayer.get(i).clear();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		windMan.reset();
	}
	
	public World getWorld() {
		return null;
	}
	
	protected void tick() {
		World world = getWorld();
		if (world != null) {
			//tick storms
			List<WeatherObject> list = getStormObjects();
			for (int i = 0; i < list.size(); i++) {
				WeatherObject so = list.get(i);
				if (this instanceof WeatherManagerServer && so.isDead) {
					removeStormObject(so.ID);
					((WeatherManagerServer)this).syncStormRemove(so);
				} else {
					
						if (!so.isDead) {
							so.tick();
						} else {
							if (getWorld().isRemote) {
								Weather.dbg("WARNING!!! - detected isDead storm object still in client side list, had to remove storm object with ID " + so.ID + " from client side, wasnt properly removed via main channels");
								removeStormObject(so.ID);
							}
						}
				}
			}

			//tick wind
			windMan.tick();
		}
	}
	
	public void tickRender(float partialTick) {
		World world = getWorld();
		if (world != null) {
			try {
				for (int i = 0; i < getStormObjects().size(); i++) {
					WeatherObject obj = getStormObjects().get(i);
					if (obj != null) {
						obj.tickRender(partialTick);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public List<WeatherObject> getStormObjects() {
		return listStormObjects;
	}
	
	protected List<StormObject> getStormObjectsByLayer(int layer) {
		return lookupStormObjectsByLayer.get(layer);
	}
	
	public void addStormObject(WeatherObject so) {
		if (!lookupStormObjectsByID.containsKey(so.ID)) {
			listStormObjects.add(so);
			lookupStormObjectsByID.put(so.ID, so);
			if (so instanceof StormObject) {
				StormObject so2 = (StormObject) so;
				lookupStormObjectsByLayer.get(so2.layer).add(so2);
			}
		} else {
			Weather.dbg("Weather2 WARNING!!! Received new storm create for an ID that is already active! design bug or edgecase with PlayerEvent.Clone, ID: " + so.ID);
			Weather.dbgStackTrace();

		}
	}
	
	public void removeStormObject(long ID) {
		WeatherObject so = lookupStormObjectsByID.get(ID);
		
		if (so != null) {
			so.setDead();
			listStormObjects.remove(so);
			lookupStormObjectsByID.remove(ID);
			if (so instanceof StormObject) {
				StormObject so2 = (StormObject) so;
				lookupStormObjectsByLayer.get(so2.layer).remove(so2);
			}
		} else {
			Weather.dbg("error looking up storm ID on server for removal: " + ID + " - lookup count: " + lookupStormObjectsByID.size() + " - last used ID: " + WeatherObject.lastUsedStormID);
		}
	}
	
	protected StormObject getClosestStormAny(Vec3 parPos, double maxDist) {
		return getClosestStorm(parPos, maxDist, -1, true);
	}
	
	public StormObject getClosestStorm(Vec3 parPos, double maxDist, int severityFlagMin) {
		return getClosestStorm(parPos, maxDist, severityFlagMin, false);
	}
	
	public StormObject getClosestStorm(Vec3 parPos, double maxDist, int severityFlagMin, boolean orRain) {
		
		StormObject closestStorm = null;
		double closestDist = 9999999;
		
		List<WeatherObject> listStorms = getStormObjects();
		
		for (int i = 0; i < listStorms.size(); i++) {
			WeatherObject wo = listStorms.get(i);
			if (wo instanceof StormObject) {
				StormObject storm = (StormObject) wo;
				if (storm == null || storm.isDead) continue;
				double dist = storm.pos.distanceTo(parPos);
				if (dist < closestDist && dist <= maxDist) {
					if ((storm.attrib_precipitation && orRain) || (severityFlagMin == -1 || storm.levelCurIntensityStage >= severityFlagMin)) {
						closestStorm = storm;
						closestDist = dist;
					}
				}
			}
			
		}
		
		return closestStorm;
	}

	/**
	 * Gets the most intense sandstorm, used for effects and sounds
	 *
	 * @param parPos
	 * @return
	 */
	public WeatherObjectSandstorm getClosestSandstormByIntensity(Vec3 parPos) {

		WeatherObjectSandstorm bestStorm = null;
		double closestDist = 9999999;
		double mostIntense = 0;

		List<WeatherObject> listStorms = getStormObjects();

		for (int i = 0; i < listStorms.size(); i++) {
			WeatherObject wo = listStorms.get(i);
			if (wo instanceof WeatherObjectSandstorm) {
				WeatherObjectSandstorm sandstorm = (WeatherObjectSandstorm) wo;
				if (sandstorm == null || sandstorm.isDead) continue;

				List<Vec3> points = sandstorm.getSandstormAsShape();

				double scale = sandstorm.getSandstormScale();
				boolean inStorm = CoroUtilPhysics.isInConvexShape(parPos, points);
				double dist = CoroUtilPhysics.getDistanceToShape(parPos, points);
				//if best is within storm, compare intensity
				if (inStorm) {
					closestDist = 0;
					if (scale > mostIntense) {
						mostIntense = scale;
						bestStorm = sandstorm;
					}
				//if best is not within storm, compare distance to shape
				} else if (closestDist > 0) {
					if (dist < closestDist) {
						closestDist = dist;
						bestStorm = sandstorm;
					}
				}
			}

		}

		return bestStorm;
	}

	public List<WeatherObject> getStormsAround(Vec3 parPos, double maxDist) {
		List<WeatherObject> storms = new ArrayList<>();
		
		for (int i = 0; i < getStormObjects().size(); i++) {
			WeatherObject wo = getStormObjects().get(i);
			if (wo.isDead) continue;
			if (wo instanceof StormObject) {
				StormObject storm = (StormObject) wo;
				if (storm.pos.distanceTo(parPos) < maxDist && (storm.attrib_precipitation || storm.levelCurIntensityStage > StormObject.STATE_NORMAL)) {
					storms.add(storm);
				}
			} else if (wo instanceof WeatherObjectSandstorm) {
				WeatherObjectSandstorm sandstorm = (WeatherObjectSandstorm)wo;
				List<Vec3> points = sandstorm.getSandstormAsShape();
				double distToStorm = CoroUtilPhysics.getDistanceToShape(parPos, points);
				if (distToStorm < maxDist) {
					storms.add(wo);
				}
			}
		}
		
		return storms;
	}
	
	public void writeToFile() {
		NBTTagCompound mainNBT = new NBTTagCompound();
		
		NBTTagCompound listStormsNBT = new NBTTagCompound();
		for (int i = 0; i < listStormObjects.size(); i++) {
			WeatherObject obj = listStormObjects.get(i);
			obj.getNbtCache().setUpdateForced(true);
			obj.writeToNBT();
			obj.getNbtCache().setUpdateForced(false);
			listStormsNBT.setTag("storm_" + obj.ID, obj.getNbtCache().getNewNBT());
		}
		mainNBT.setTag("stormData", listStormsNBT);
		mainNBT.setLong("lastUsedIDStorm", WeatherObject.lastUsedStormID);
		
		mainNBT.setLong("lastStormFormed", lastStormFormed);

		mainNBT.setLong("lastSandstormFormed", lastSandstormFormed);

		mainNBT.setFloat("cloudIntensity", this.cloudIntensity);

		mainNBT.setTag("windMan", windMan.writeToNBT(new NBTTagCompound()));
		
		String saveFolder = CoroUtilFile.getWorldSaveFolderPath() + CoroUtilFile.getWorldFolderName() + "weather2" + File.separator;
		
		try {
			//Write out to file
			if (!(new File(saveFolder).exists())) (new File(saveFolder)).mkdirs();
			FileOutputStream fos = new FileOutputStream(saveFolder + "WeatherData_" + dim + ".dat");
	    	CompressedStreamTools.writeCompressed(mainNBT, fos);
	    	fos.close();
		} catch (Exception ex) { ex.printStackTrace(); }
	}
	
	public void readFromFile() {
		
		NBTTagCompound rtsNBT = new NBTTagCompound();
		
		String saveFolder = CoroUtilFile.getWorldSaveFolderPath() + CoroUtilFile.getWorldFolderName() + "weather2" + File.separator;
		
		boolean readFail = false;
		
		try {
			if ((new File(saveFolder + "WeatherData_" + dim + ".dat")).exists()) {
				rtsNBT = CompressedStreamTools.readCompressed(new FileInputStream(saveFolder + "WeatherData_" + dim + ".dat"));
			}
		} catch (Exception ex) { 
			ex.printStackTrace();
			readFail = true;
		}
		
		//If reading file was ok, make a backup and shift names for second backup
		if (!readFail) {
			try {
				File tmp = (new File(saveFolder + "WeatherData_" + dim + "_BACKUP0.dat"));
				if (tmp.exists()) FileUtils.copyFile(tmp, (new File(saveFolder + "WeatherData_" + dim + "_BACKUP1.dat")));
				if ((new File(saveFolder + "WeatherData_" + dim + ".dat").exists())) FileUtils.copyFile((new File(saveFolder + "WeatherData_" + dim + ".dat")), (new File(saveFolder + "WeatherData_" + dim + "_BACKUP0.dat")));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			
		} else {
			System.out.println("WARNING! Weather2 File: WeatherData.dat failed to load, automatically restoring to backup from previous game run");
			try {
				//auto restore from most recent backup
				if ((new File(saveFolder + "WeatherData_" + dim + "_BACKUP0.dat")).exists()) {
					rtsNBT = CompressedStreamTools.readCompressed(new FileInputStream(saveFolder + "WeatherData_" + dim + "_BACKUP0.dat"));
				} else {
					System.out.println("WARNING! Failed to find backup file WeatherData_BACKUP0.dat, nothing loaded");
				}
			} catch (Exception ex) { 
				ex.printStackTrace();
				System.out.println("WARNING! Error loading backup file WeatherData_BACKUP0.dat, nothing loaded");
			}
		}
		
		lastStormFormed = rtsNBT.getLong("lastStormFormed");
		lastSandstormFormed = rtsNBT.getLong("lastSandstormFormed");

		//prevent setting to 0 for worlds updating to new weather version
		if (rtsNBT.hasKey("cloudIntensity")) {
			cloudIntensity = rtsNBT.getFloat("cloudIntensity");
		}
		
		WeatherObject.lastUsedStormID = rtsNBT.getLong("lastUsedIDStorm");

		windMan.readFromNBT(rtsNBT.getCompoundTag("windMan"));
		
		NBTTagCompound nbtVolcanoes = rtsNBT.getCompoundTag("volcanoData");
		
		Iterator<String> it = nbtVolcanoes.getKeySet().iterator();
		
		NBTTagCompound nbtStorms = rtsNBT.getCompoundTag("stormData");
		
		it = nbtStorms.getKeySet().iterator();
		
		while (it.hasNext()) {
			String tagName = (String) it.next();
			NBTTagCompound data = nbtStorms.getCompoundTag(tagName);
			
			if (ServerTickHandler.lookupDimToWeatherMan.get(dim) != null) {
                WeatherObject wo = null;
                if (data.getInteger("weatherObjectType") == EnumWeatherObjectType.CLOUD.ordinal()) {
                    wo = new StormObject(this);
                } else if (data.getInteger("weatherObjectType") == EnumWeatherObjectType.SAND.ordinal()) {
                    wo = new WeatherObjectSandstorm(this);
                    //initStormNew???
                }
				try {
					wo.getNbtCache().setNewNBT(data);
					wo.readFromNBT();
					wo.getNbtCache().updateCacheFromNew();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				addStormObject(wo);

				((WeatherManagerServer)(this)).syncStormNew(wo);
			} else {
				System.out.println("WARNING: trying to load storm objects for missing dimension: " + dim);
			}
		}
	}
	
	public WindManager getWindManager() {
		return this.windMan;
	}
}
