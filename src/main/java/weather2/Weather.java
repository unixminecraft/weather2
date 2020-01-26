package weather2;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import weather2.config.ConfigFoliage;
import weather2.config.ConfigMisc;
import weather2.player.PlayerData;
import weather2.util.WeatherUtilConfig;
import weather2.weathersystem.WeatherManagerServer;

@Mod(modid = "weather2", name="weather2", version=Weather.version, dependencies="required-after:coroutil@[1.12.1-1.2.12,)")
public class Weather {
	
	@Mod.Instance( value = Weather.modID )
	public static Weather instance;

	public static final String modID = "weather2";
	public static final String version = "${version}";

    @SidedProxy(clientSide = "weather2.ClientProxy", serverSide = "weather2.CommonProxy")
    public static CommonProxy proxy;
    
    public static String eventChannelName = "weather2";
	public static final FMLEventChannel eventChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel(eventChannelName);


	@Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
    	eventChannel.register(new EventHandlerPacket());
    	
    	MinecraftForge.EVENT_BUS.register(new EventHandlerFML());
		MinecraftForge.EVENT_BUS.register(new EventHandlerForge());

    	WeatherUtilConfig.nbtLoadDataAll();

		proxy.preInit();
    }
    
	@Mod.EventHandler
    public void load(FMLInitializationEvent event)
    {
    	proxy.init();
    }

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
		proxy.postInit();

		//setting last state to track after configs load, but before ticking that uses it
		EventHandlerFML.extraGrassLast = ConfigFoliage.extraGrass;
	}
    
    @Mod.EventHandler
	public void serverStarting(FMLServerStartingEvent event) {
    	event.registerServerCommand(new CommandWeather2());
    }
    
    @Mod.EventHandler
    public void serverStart(FMLServerStartedEvent event) {
    	
    }
    
    @Mod.EventHandler
    public void serverStop(FMLServerStoppedEvent event) {
    	writeOutData(true);
    	resetStates();
    }
    
    private static void resetStates() {
    	ServerTickHandler.reset();
    }
    
    public static void writeOutData(boolean unloadInstances) {
    	//write out overworld only, because only dim with volcanos planned
    	try {
    		WeatherManagerServer wm = ServerTickHandler.lookupDimToWeatherMan.get(0);
    		if (wm != null) {
    			wm.writeToFile();
    		}
    		PlayerData.writeAllPlayerNBT(unloadInstances);
    		//doesnt cover all needs, client connected to server needs this called from gui close too
    		//maybe dont call this from here so client connected to server doesnt override what a client wants his 'server' settings to be in his singleplayer world
    		//factoring in we dont do per world settings for this
    	} catch (Exception ex) {
    		ex.printStackTrace();
    	}
    }
	
	public static void dbg(Object obj) {
		if (ConfigMisc.consoleDebug) {
			System.out.println(obj);
		}
	}

	public static void dbgStackTrace() {
		if (ConfigMisc.consoleDebug) {
			StackTraceElement[] arr = Thread.currentThread().getStackTrace();
			for (StackTraceElement ele : arr) {
				System.out.println(ele.toString());
			}
		}
	}
}
