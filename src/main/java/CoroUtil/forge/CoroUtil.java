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
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.biome.Biome;
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
		MinecraftForge.EVENT_BUS.register(new EventHandlerFML());
    	MinecraftForge.EVENT_BUS.register(new EventHandlerForge());
    }
    
    @Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {

    	if (ConfigCoroUtilAdvanced.fixBadBiomeEntitySpawnEntries) {

            CULog.log("fixBadBiomeEntitySpawnEntries enabled, scanning and fixing all biome entity spawn lists for potential crash risks");

            fixBadBiomeEntitySpawns();
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
	
    /**
     * Game likes to force an exception and crash out if a mod adds a zero weight spawn entry to a biome+enum ceature type when the total weight is zero for all entries in the list
     * this method checks for this and removes them
     */
    private static void fixBadBiomeEntitySpawns() {
        for (Biome biome : Biome.REGISTRY) {

            for (EnumCreatureType type : EnumCreatureType.values()) {

                List<Biome.SpawnListEntry> list = biome.getSpawnableList(type);
                boolean found = false;
                String str = "";
                int totalWeight = 0;

                for (Biome.SpawnListEntry entry : list) {
                    totalWeight += entry.itemWeight;
                    if (entry.itemWeight == 0) {
                        found = true;
                        str += entry.entityClass.getName() + ", ";
                    }
                }

                if (found) {
                    if (totalWeight == 0) {
                        CULog.log("Detected issue for entity(s)" + str);
                        CULog.log("Biome '" + biome.biomeName + "' for EnumCreatureType '" + type.name() + "', SpawnListEntry size: " + list.size());
                        CULog.log("Clearing relevant spawnableList to fix issue");
                        //detected crashable state of data, clear out spawnlist then
                        if (type == EnumCreatureType.MONSTER) {
                            biome.getSpawnableList(EnumCreatureType.MONSTER).clear();
                        } else if (type == EnumCreatureType.CREATURE) {
                            biome.getSpawnableList(EnumCreatureType.CREATURE).clear();
                        } else if (type == EnumCreatureType.WATER_CREATURE) {
                            biome.getSpawnableList(EnumCreatureType.WATER_CREATURE).clear();
                        } else if (type == EnumCreatureType.AMBIENT) {
                            biome.getSpawnableList(EnumCreatureType.AMBIENT).clear();
                        } else {
                            //theres also Biome.modSpawnableLists for modded entries, but ive decided not to care about this one
                        }
                    }
                }
            }
        }
    }
}
