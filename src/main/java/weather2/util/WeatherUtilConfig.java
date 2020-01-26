package weather2.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import CoroUtil.util.CoroUtilFile;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import weather2.ServerTickHandler;
import weather2.Weather;
import weather2.config.ConfigMisc;
import weather2.config.ConfigSand;
import weather2.config.ConfigSnow;
import weather2.config.ConfigStorm;
import weather2.config.ConfigTornado;

public class WeatherUtilConfig {

	public static List<Integer> listDimensionsWeather = new ArrayList<Integer>();
	public static List<Integer> listDimensionsClouds = new ArrayList<Integer>();
	//used for deadly storms and sandstorms
	public static List<Integer> listDimensionsStorms = new ArrayList<Integer>();
	public static List<Integer> listDimensionsWindEffects = new ArrayList<Integer>();
	
	private static int CMD_BTN_PERF_STORM = 2;
	private static int CMD_BTN_PERF_NATURE = 3;
	private static int CMD_BTN_PERF_PRECIPRATE = 12;
	private static int CMD_BTN_PERF_SHADERS_PARTICLE = 18;
	private static int CMD_BTN_PERF_SHADERS_FOLIAGE = 19;
	
	private static int CMD_BTN_COMP_STORM = 4;
	private static int CMD_BTN_COMP_LOCK = 5;
	private static int CMD_BTN_COMP_PARTICLEPRECIP = 6;
	private static int CMD_BTN_COMP_SNOWFALLBLOCKS = 7;
	private static int CMD_BTN_COMP_LEAFFALLBLOCKS = 8;
	private static int CMD_BTN_COMP_PARTICLESNOMODS = 13;
	
	private static int CMD_BTN_PREF_RATEOFSTORM = 9;
	private static int CMD_BTN_PREF_CHANCEOFSTORM = 14;
	private static int CMD_BTN_PREF_CHANCEOFRAIN = 10;
	private static int CMD_BTN_PREF_BLOCKDESTRUCTION = 11;
	private static int CMD_BTN_PREF_TORNADOANDCYCLONES = 15;
	private static int CMD_BTN_PREF_SANDSTORMS = 16;
	private static int CMD_BTN_PREF_GLOBALRATE = 17;
	
	private static int CMD_BTN_HIGHEST_ID = 19;

	private static List<String> LIST_RATES2 = new ArrayList<String>(Arrays.asList("High", "Medium", "Low", "None"));
	private static List<String> LIST_TOGGLE = new ArrayList<String>(Arrays.asList("Off", "On"));
	
	private static List<String> LIST_STORMSWHEN = new ArrayList<String>(Arrays.asList("Local Biomes", "Global Overcast"));
	
	private static List<Integer> listSettingsClient = new ArrayList<Integer>();
	private static List<Integer> listSettingsServer = new ArrayList<Integer>();
	
	//actual data that gets written out to disk
	public static NBTTagCompound nbtServerData = new NBTTagCompound();
	
	static {
		listSettingsClient.add(CMD_BTN_PERF_STORM);
		listSettingsClient.add(CMD_BTN_PERF_NATURE);
		listSettingsClient.add(CMD_BTN_COMP_PARTICLEPRECIP);
		listSettingsClient.add(CMD_BTN_PERF_PRECIPRATE);
		listSettingsClient.add(CMD_BTN_COMP_PARTICLESNOMODS);
		listSettingsClient.add(CMD_BTN_PERF_SHADERS_PARTICLE);
		listSettingsClient.add(CMD_BTN_PERF_SHADERS_FOLIAGE);
		
		
		listSettingsServer.add(CMD_BTN_COMP_STORM);
		listSettingsServer.add(CMD_BTN_COMP_LOCK);
		listSettingsServer.add(CMD_BTN_COMP_SNOWFALLBLOCKS);
		listSettingsServer.add(CMD_BTN_COMP_LEAFFALLBLOCKS);
		listSettingsServer.add(CMD_BTN_PREF_RATEOFSTORM);
		listSettingsServer.add(CMD_BTN_PREF_CHANCEOFSTORM);
		listSettingsServer.add(CMD_BTN_PREF_CHANCEOFRAIN);
		listSettingsServer.add(CMD_BTN_PREF_BLOCKDESTRUCTION);
		listSettingsServer.add(CMD_BTN_PREF_TORNADOANDCYCLONES);
		listSettingsServer.add(CMD_BTN_PREF_SANDSTORMS);
		listSettingsServer.add(CMD_BTN_PREF_GLOBALRATE);
	}
	
	//server should call this on detecting of a save request (close of GUI packet send)
	private static void processNBTToModConfigServer() {
		nbtSaveDataServer();
		
		Weather.dbg("processNBTToModConfigServer");
		
		Weather.dbg("nbtServerData: " + nbtServerData);
		try {
			if (nbtServerData.hasKey("btn_" + CMD_BTN_COMP_STORM)) {
				ConfigMisc.overcastMode = LIST_STORMSWHEN.get(nbtServerData.getInteger("btn_" + CMD_BTN_COMP_STORM)).equalsIgnoreCase("Global Overcast");
			}
			
			if (nbtServerData.hasKey("btn_" + CMD_BTN_COMP_LOCK)) {
				int val = nbtServerData.getInteger("btn_" + CMD_BTN_COMP_LOCK);
				if (val == 1) {
					ConfigMisc.lockServerWeatherMode = 1;
				} else if (val == 0) {
					ConfigMisc.lockServerWeatherMode = 0;
				} else {
					ConfigMisc.lockServerWeatherMode = -1;
				}
			}
			
			if (nbtServerData.hasKey("btn_" + CMD_BTN_COMP_SNOWFALLBLOCKS)) {
				boolean val = nbtServerData.getInteger("btn_" + CMD_BTN_COMP_SNOWFALLBLOCKS) == 1;
				ConfigSnow.Snow_PerformSnowfall = val;
			}
			
			if (nbtServerData.hasKey("btn_" + CMD_BTN_PREF_RATEOFSTORM)) {
				int numDays = nbtServerData.getInteger("btn_" + CMD_BTN_PREF_RATEOFSTORM);
				if (numDays == 0) {
					ConfigStorm.Player_Storm_Deadly_TimeBetweenInTicks = 12000;
					ConfigStorm.Server_Storm_Deadly_TimeBetweenInTicks = 12000;
				} else if (numDays == 11) {
					//potentially remove the 'never' clause from here in favor of the dimension specific disabling of 'storms' which is already used in code
					//for now consider this a second layer of rules to the storm creation process, probably not a user friendly idea
					ConfigStorm.Player_Storm_Deadly_TimeBetweenInTicks = -1;
					ConfigStorm.Server_Storm_Deadly_TimeBetweenInTicks = -1;
				} else {
					ConfigStorm.Player_Storm_Deadly_TimeBetweenInTicks = 24000*numDays;
					ConfigStorm.Server_Storm_Deadly_TimeBetweenInTicks = 24000*numDays;
				}
				
			}
			
			if (nbtServerData.hasKey("btn_" + CMD_BTN_PREF_CHANCEOFSTORM)) {
				if (LIST_RATES2.get(nbtServerData.getInteger("btn_" + CMD_BTN_PREF_CHANCEOFSTORM)).equalsIgnoreCase("high")) {
					ConfigStorm.Player_Storm_Deadly_OddsTo1 = 30;
					ConfigStorm.Server_Storm_Deadly_OddsTo1 = 30;
				} else if (LIST_RATES2.get(nbtServerData.getInteger("btn_" + CMD_BTN_PREF_CHANCEOFSTORM)).equalsIgnoreCase("medium")) {
					ConfigStorm.Player_Storm_Deadly_OddsTo1 = 45;
					ConfigStorm.Server_Storm_Deadly_OddsTo1 = 45;
				} else if (LIST_RATES2.get(nbtServerData.getInteger("btn_" + CMD_BTN_PREF_CHANCEOFSTORM)).equalsIgnoreCase("low")) {
					ConfigStorm.Player_Storm_Deadly_OddsTo1 = 60;
					ConfigStorm.Server_Storm_Deadly_OddsTo1 = 60;
				}
			}
			
			if (nbtServerData.hasKey("btn_" + CMD_BTN_PREF_CHANCEOFRAIN)) {
				if (LIST_RATES2.get(nbtServerData.getInteger("btn_" + CMD_BTN_PREF_CHANCEOFRAIN)).equalsIgnoreCase("high")) {
					ConfigStorm.Storm_Rain_OddsTo1 = 150;
					ConfigStorm.Storm_Rain_Overcast_OddsTo1 = ConfigStorm.Storm_Rain_OddsTo1 / 3;
				} else if (LIST_RATES2.get(nbtServerData.getInteger("btn_" + CMD_BTN_PREF_CHANCEOFRAIN)).equalsIgnoreCase("medium")) {
					ConfigStorm.Storm_Rain_OddsTo1 = 300;
					ConfigStorm.Storm_Rain_Overcast_OddsTo1 = ConfigStorm.Storm_Rain_OddsTo1 / 3;
				} else if (LIST_RATES2.get(nbtServerData.getInteger("btn_" + CMD_BTN_PREF_CHANCEOFRAIN)).equalsIgnoreCase("low")) {
					ConfigStorm.Storm_Rain_OddsTo1 = 450;
					ConfigStorm.Storm_Rain_Overcast_OddsTo1 = ConfigStorm.Storm_Rain_OddsTo1 / 3;
				} else if (LIST_RATES2.get(nbtServerData.getInteger("btn_" + CMD_BTN_PREF_CHANCEOFRAIN)).equalsIgnoreCase("none")) {
					ConfigStorm.Storm_Rain_OddsTo1 = -1;
					ConfigStorm.Storm_Rain_Overcast_OddsTo1 = -1;
				}
			}
			
			if (nbtServerData.hasKey("btn_" + CMD_BTN_PREF_BLOCKDESTRUCTION)) {
				ConfigTornado.Storm_Tornado_grabBlocks = LIST_TOGGLE.get(nbtServerData.getInteger("btn_" + CMD_BTN_PREF_BLOCKDESTRUCTION)).equalsIgnoreCase("on");
			}
			
			if (nbtServerData.hasKey("btn_" + CMD_BTN_PREF_TORNADOANDCYCLONES)) {
				ConfigTornado.Storm_NoTornadosOrCyclones = LIST_TOGGLE.get(nbtServerData.getInteger("btn_" + CMD_BTN_PREF_TORNADOANDCYCLONES)).equalsIgnoreCase("off");
			}

			if (nbtServerData.hasKey("btn_" + CMD_BTN_PREF_SANDSTORMS)) {
				ConfigSand.Storm_NoSandstorms = LIST_TOGGLE.get(nbtServerData.getInteger("btn_" + CMD_BTN_PREF_SANDSTORMS)).equalsIgnoreCase("off");
			}

			if (nbtServerData.hasKey("btn_" + CMD_BTN_PREF_GLOBALRATE)) {
				ConfigStorm.Server_Storm_Deadly_UseGlobalRate = nbtServerData.getInteger("btn_" + CMD_BTN_PREF_GLOBALRATE) == 0;
				ConfigSand.Sandstorm_UseGlobalServerRate = nbtServerData.getInteger("btn_" + CMD_BTN_PREF_GLOBALRATE) == 0;

				//System.out.println("ConfigStorm.Server_Storm_Deadly_UseGlobalRate: " + ConfigStorm.Server_Storm_Deadly_UseGlobalRate);
			}
			
			NBTTagCompound nbtDims = nbtServerData.getCompoundTag("dimData");
			
			Weather.dbg("before: " + listDimensionsWeather);
			
			Iterator<String> it = nbtDims.getKeySet().iterator();
			while (it.hasNext()) {
			 	String tagName = (String) it.next();
			 	NBTTagInt entry = (NBTTagInt) nbtDims.getTag(tagName);
				String[] vals = tagName.split("_");
				//if weather
				if (vals[2].equals("0")) {
					int dimID = Integer.parseInt(vals[1]);
					if (entry.getInt() == 0) {
						//if off			
						if (listDimensionsWeather.contains(dimID)) {
							listDimensionsWeather.remove(dimID);
						}
					} else {
						//if on
						if (!listDimensionsWeather.contains(dimID)) {
							listDimensionsWeather.add(dimID);
						}
					}					
				} else if (vals[2].equals("1")) {
					int dimID = Integer.parseInt(vals[1]);
					if (entry.getInt() == 0) {
						//if off			
						if (listDimensionsClouds.contains(dimID)) {
							listDimensionsClouds.remove(dimID);
						}
					} else {
						//if on
						if (!listDimensionsClouds.contains(dimID)) {
							listDimensionsClouds.add(dimID);
						}
					}					
				} else if (vals[2].equals("2")) {
					int dimID = Integer.parseInt(vals[1]);
					if (entry.getInt() == 0) {
						//if off			
						if (listDimensionsStorms.contains(dimID)) {
							listDimensionsStorms.remove(dimID);
						}
					} else {
						//if on
						if (!listDimensionsStorms.contains(dimID)) {
							listDimensionsStorms.add(dimID);
						}
					}					
				}
				Weather.dbg("dim: " + vals[1] + " - setting ID: " + vals[2] + " - data: " + entry.getInt());
			}
			
			Weather.dbg("after: " + listDimensionsWeather);
			
			processListsReverse();
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		ServerTickHandler.syncServerConfigToClient();

		//ConfigMod.configLookup.get(modID).writeConfigFile(true);
		//ConfigMod.populateData(modID);
		
		//work lists here
		
		//client nbt to client mod config setttings, and server nbt to server mod config settings
		//invoke whatever method modconfig uses to write out its data, for both its client and server side
	}
	
	public static void nbtReceiveClientData(NBTTagCompound parNBT) {
		for (int i = 0; i <= CMD_BTN_HIGHEST_ID; i++) {
			if (parNBT.hasKey("btn_" + i)) {
				nbtServerData.setInteger("btn_" + i, parNBT.getInteger("btn_" + i));
			}
		}
		
		//also add dimension feature config, its iterated over
		nbtServerData.setTag("dimData", parNBT.getCompoundTag("dimData"));
		
		processNBTToModConfigServer();
	}
	
	private static void nbtSaveDataServer() {
		nbtWriteNBTToDisk(nbtServerData, false);
	}
	
	public static void nbtLoadDataAll() {
		nbtLoadDataServer();
	}
	
	private static void nbtLoadDataServer() {
		nbtServerData = nbtReadNBTFromDisk(false);
	}
	
	public static NBTTagCompound createNBTDimensionListing() {
		NBTTagCompound data = new NBTTagCompound();
		
		World[] worlds = DimensionManager.getWorlds();
		
		for (int i = 0; i < worlds.length; i++) {
			NBTTagCompound nbtDim = new NBTTagCompound();
			int dimID = worlds[i].provider.getDimension();
			nbtDim.setInteger("ID", dimID); //maybe redundant if we name tag as dimID too
			nbtDim.setString("name", worlds[i].provider.getDimensionType().getName());
			nbtDim.setBoolean("weather", listDimensionsWeather.contains(dimID));
			nbtDim.setBoolean("clouds", listDimensionsClouds.contains(dimID));
			nbtDim.setBoolean("storms", listDimensionsStorms.contains(dimID));
			
			data.setTag("" + dimID, nbtDim);
		}
		
		return data;
	}
	
	public static void processLists() {
		listDimensionsWeather = parseList(ConfigMisc.Dimension_List_Weather);
		listDimensionsClouds = parseList(ConfigMisc.Dimension_List_Clouds);
		listDimensionsStorms = parseList(ConfigMisc.Dimension_List_Storms);
		listDimensionsWindEffects = parseList(ConfigMisc.Dimension_List_WindEffects);
	}
	
	private static void processListsReverse() {
		ConfigMisc.Dimension_List_Weather = StringUtils.join(listDimensionsWeather, " ");
		ConfigMisc.Dimension_List_Clouds = StringUtils.join(listDimensionsClouds, " ");
		ConfigMisc.Dimension_List_Storms = StringUtils.join(listDimensionsStorms, " ");
		ConfigMisc.Dimension_List_WindEffects = StringUtils.join(listDimensionsWindEffects, " ");
	}
	
	private static List<Integer> parseList(String parData) {
		String listStr = parData;
		listStr = listStr.replace(",", " ");
		String[] arrStr = listStr.split(" ");
		Integer[] arrInt = new Integer[arrStr.length];
		for (int i = 0; i < arrStr.length; i++) {
			try {
				arrInt[i] = Integer.parseInt(arrStr[i]);
			} catch (Exception ex) {
				arrInt[i] = -999999; //set to -999999, hope no dimension id of this exists
			}
		}
		return new ArrayList<Integer>(Arrays.asList(arrInt));
	}
	
	private static void nbtWriteNBTToDisk(NBTTagCompound parData, boolean saveForClient) {
		String fileURL = null;
		if (saveForClient) {
			fileURL = CoroUtilFile.getMinecraftSaveFolderPath() + File.separator + "Weather2" + File.separator + "EZGUIConfigClientData.dat";
		} else {
			fileURL = CoroUtilFile.getMinecraftSaveFolderPath() + File.separator + "Weather2" + File.separator + "EZGUIConfigServerData.dat";
		}
		
		try {
			FileOutputStream fos = new FileOutputStream(fileURL);
	    	CompressedStreamTools.writeCompressed(parData, fos);
	    	fos.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			Weather.dbg("Error writing Weather2 EZ GUI data");
		}
	}
	
	private static NBTTagCompound nbtReadNBTFromDisk(boolean loadForClient) {
		NBTTagCompound data = new NBTTagCompound();
		String fileURL = null;
		if (loadForClient) {
			fileURL = CoroUtilFile.getMinecraftSaveFolderPath() + File.separator + "Weather2" + File.separator + "EZGUIConfigClientData.dat";
		} else {
			fileURL = CoroUtilFile.getMinecraftSaveFolderPath() + File.separator + "Weather2" + File.separator + "EZGUIConfigServerData.dat";
		}
		
		try {
			if ((new File(fileURL)).exists()) {
				data = CompressedStreamTools.readCompressed(new FileInputStream(fileURL));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			Weather.dbg("Error reading Weather2 EZ GUI data");
		}
		return data;
	}

	public static void setOvercastModeServerSide(boolean val) {
		nbtServerData.setInteger("btn_" + CMD_BTN_COMP_STORM, val ? 1 : 0);
		nbtSaveDataServer();
	}
	
}
