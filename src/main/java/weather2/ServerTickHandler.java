package weather2;

import java.util.ArrayList;
import java.util.HashMap;

import CoroUtil.forge.CULog;
import CoroUtil.packet.PacketHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import weather2.config.ConfigMisc;
import weather2.util.WeatherUtilConfig;
import weather2.weathersystem.WeatherManagerBase;
import weather2.weathersystem.WeatherManagerServer;

public class ServerTickHandler
{   
	//Used for easy iteration, could be replaced
    private static ArrayList<WeatherManagerServer> listWeatherMans;
    
    //Main lookup method for dim to weather systems
    public static HashMap<Integer, WeatherManagerServer> lookupDimToWeatherMan;
    
    private static World lastWorld;
    	
    static {
    	
    	listWeatherMans = new ArrayList<WeatherManagerServer>();
    	lookupDimToWeatherMan = new HashMap<Integer, WeatherManagerServer>();
    	
    }
    
    public static void onTickInGame()
    {
    	
        if (FMLCommonHandler.instance() == null || FMLCommonHandler.instance().getMinecraftServerInstance() == null)
        {
            return;
        }

        World world = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);
        
        if (world != null && lastWorld != world) {
        	lastWorld = world;
        }
        
        //regularly save data
        if (world != null) {
        	if (world.getTotalWorldTime() % ConfigMisc.Misc_AutoDataSaveIntervalInTicks == 0) {
        		Weather.writeOutData(false);
        	}
        }
        
        World worlds[] = DimensionManager.getWorlds();
        
        //add use of CSV of supported dimensions here once feature is added, for now just overworld
        
        for (int i = 0; i < worlds.length; i++) {
        	if (!lookupDimToWeatherMan.containsKey(worlds[i].provider.getDimension())) {
        		
        		if (WeatherUtilConfig.listDimensionsWeather.contains(worlds[i].provider.getDimension())) {
        			addWorldToWeather(worlds[i].provider.getDimension());
        		}
        	}
        	
        	//tick it
        	WeatherManagerServer wms = lookupDimToWeatherMan.get(worlds[i].provider.getDimension());
        	if (wms != null) {
        		lookupDimToWeatherMan.get(worlds[i].provider.getDimension()).tick();
        	}
        }

        if (ConfigMisc.Aesthetic_Only_Mode) {
        	if (!ConfigMisc.overcastMode) {
        		ConfigMisc.overcastMode = true;
				CULog.dbg("detected Aesthetic_Only_Mode on, setting overcast mode on");
				WeatherUtilConfig.setOvercastModeServerSide(ConfigMisc.overcastMode);
				syncServerConfigToClient();
			}
		}

        //TODO: only sync when things change? is now sent via PlayerLoggedInEvent at least
		if (world.getTotalWorldTime() % 200 == 0) {
			syncServerConfigToClient();
		}
    }
    
    //must only be used when world is active, soonest allowed is TickType.WORLDLOAD
    private static void addWorldToWeather(int dim) {
    	Weather.dbg("Registering Weather2 manager for dim: " + dim);
    	WeatherManagerServer wm = new WeatherManagerServer(dim);
    	
    	listWeatherMans.add(wm);
    	lookupDimToWeatherMan.put(dim, wm);
    	
    	wm.readFromFile();
    }
    
    private static void removeWorldFromWeather(int dim) {
    	Weather.dbg("Weather2: Unregistering manager for dim: " + dim);
    	WeatherManagerServer wm = lookupDimToWeatherMan.get(dim);
    	
    	if (wm != null) {
	    	listWeatherMans.remove(wm);
	    	lookupDimToWeatherMan.remove(dim);
    	}
    	wm.writeToFile();
    }

    public static void playerClientRequestsFullSync(EntityPlayerMP entP) {
		WeatherManagerServer wm = lookupDimToWeatherMan.get(entP.world.provider.getDimension());
		if (wm != null) {
			wm.playerJoinedWorldSyncFull(entP);
		}
	}
    
    public static void initialize() {
    	if (ServerTickHandler.lookupDimToWeatherMan.get(0) == null) {
    		ServerTickHandler.addWorldToWeather(0);
    	}
    }
    
    public static void reset() {
		Weather.dbg("Weather2: ServerTickHandler resetting");
    	for (int i = 0; i < listWeatherMans.size(); i++) {
    		WeatherManagerBase wm = listWeatherMans.get(i);
    		int dim = wm.dim;
    		if (lookupDimToWeatherMan.containsKey(dim)) {
    			removeWorldFromWeather(dim);
    		}
    	}

    	//should never happen
    	if (listWeatherMans.size() > 0 || lookupDimToWeatherMan.size() > 0) {
    		Weather.dbg("Weather2: reset state failed to manually clear lists, listWeatherMans.size(): " + listWeatherMans.size() + " - lookupDimToWeatherMan.size(): " + lookupDimToWeatherMan.size() + " - forcing a full clear of lists");
    		listWeatherMans.clear();
    		lookupDimToWeatherMan.clear();
    	}
    }
    
    public static WeatherManagerServer getWeatherSystemForDim(int dimID) {
    	return lookupDimToWeatherMan.get(dimID);
    }

	public static void syncServerConfigToClient() {
		//packets
		NBTTagCompound data = new NBTTagCompound();
		data.setString("packetCommand", "ClientConfigData");
		data.setString("command", "syncUpdate");

		ClientConfigData.writeNBT(data);

		Weather.eventChannel.sendToAll(PacketHelper.getNBTPacket(data, Weather.eventChannelName));
	}

	public static void syncServerConfigToClientPlayer(EntityPlayerMP player) {
		//packets
		NBTTagCompound data = new NBTTagCompound();
		data.setString("packetCommand", "ClientConfigData");
		data.setString("command", "syncUpdate");

		ClientConfigData.writeNBT(data);

		Weather.eventChannel.sendTo(PacketHelper.getNBTPacket(data, Weather.eventChannelName), player);
	}
}
