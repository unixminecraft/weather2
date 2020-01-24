package CoroUtil.forge;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import CoroUtil.config.ConfigCoroUtil;
import CoroUtil.config.ConfigCoroUtilAdvanced;
import CoroUtil.util.CoroUtilFile;
import CoroUtil.util.CoroUtilMisc;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@Mod(modid = CoroUtil.modID, name=CoroUtil.modID, version=CoroUtil.version, acceptableRemoteVersions="*")
public class CoroUtil {
	
	@Mod.Instance( value = CoroUtil.modID )
	public static CoroUtil instance;
	public static final String modID = "coroutil";

	public static final String modID_HWMonsters = "hw_monsters";
	public static final String modID_HWInvasions = "hw_inv";


	//public static final String version = "${version}";
	//when we definitely need to enforce a new CoroUtil version outside dev, use this for production
	//TODO: find a way to perminently do this for dev only
	public static final String version = "1.12.1-1.2.36";
    
    @SidedProxy(clientSide = "CoroUtil.forge.ClientProxy", serverSide = "CoroUtil.forge.CommonProxy")
    public static CommonProxy proxy;
    
    public static boolean initProperNeededForInstance = true;
    
    public static String eventChannelName = "coroutil";
	public static final FMLEventChannel eventChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel(eventChannelName);
    
    public static ConfigCoroUtil configCoroUtil = new ConfigCoroUtil();
    public static ConfigCoroUtilAdvanced configDev = new ConfigCoroUtilAdvanced();
    
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
		migrateOldConfig();

    	//for good measure
		configDev.hookUpdatedValues();
    	
    	eventChannel.register(new EventHandlerPacket());
    }

    public static void migrateOldConfig() {

    	File path = new File("." + File.separator + "config" + File.separator + "CoroUtil");
    	try {
			path.mkdirs();
		} catch (Exception ex) {
    		ex.printStackTrace();
		}

		File oldFile = new File("." + File.separator + "config" + File.separator + "CoroUtil.cfg");
		File newFile = new File("." + File.separator + "config" + File.separator + "CoroUtil" + File.separator + "General.cfg");

		fixConfigFile(oldFile, newFile, "coroutil {", "general {");

		oldFile = new File("." + File.separator + "config" + File.separator + "CoroUtil_DynamicDifficulty.cfg");
		newFile = new File("." + File.separator + "config" + File.separator + "CoroUtil" + File.separator + "DynamicDifficulty.cfg");

		fixConfigFile(oldFile, newFile, "coroutil_dynamicdifficulty {", "dynamicdifficulty {");
	}

	public static void fixConfigFile(File oldFile, File newFile, String oldCat, String newCat) {
		if (oldFile.exists() && !newFile.exists()) {
			CULog.log("Detected old " + oldFile.toString() + ", relocating to " + newFile.toString());
			try {
				Files.move(oldFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

				//fix category
				List<String> newLines = new ArrayList<>();
				for (String line : Files.readAllLines(newFile.toPath(), StandardCharsets.UTF_8)) {
					if (line.contains(oldCat)) {
						newLines.add(line.replace(oldCat, newCat));
					} else {
						newLines.add(line);
					}
				}
				Files.write(newFile.toPath(), newLines, StandardCharsets.UTF_8);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
    
    @Mod.EventHandler
    public void load(FMLInitializationEvent event)
    {
    	//TickRegistry.registerTickHandler(new ServerTickHandler(this), Side.SERVER);
		MinecraftForge.EVENT_BUS.register(new EventHandlerFML());
    	MinecraftForge.EVENT_BUS.register(new EventHandlerForge());
    	//MinecraftForge.EVENT_BUS.register(new EventHandler());
    	proxy.init(this);

    	//petsManager = new PetsManager();
    }
    
    @Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {

    	if (ConfigCoroUtilAdvanced.fixBadBiomeEntitySpawnEntries) {

            CULog.log("fixBadBiomeEntitySpawnEntries enabled, scanning and fixing all biome entity spawn lists for potential crash risks");

            CoroUtilMisc.fixBadBiomeEntitySpawns();
        }
	}
    
    public CoroUtil() {
    	
    }
    
    @Mod.EventHandler
    public void serverStart(FMLServerStartedEvent event) {
    	
    }
    
    @Mod.EventHandler
    public void serverStop(FMLServerStoppedEvent event) {
    	initProperNeededForInstance = true;
    }
    
    public static void initTry() {
    	if (initProperNeededForInstance) {
    		CULog.log("CoroUtil being reinitialized");
    		initProperNeededForInstance = false;
	    	CoroUtilFile.getWorldFolderName();
    	}
    }
    
	public static void dbg(String obj) {
    	CULog.dbg(obj);
	}
}