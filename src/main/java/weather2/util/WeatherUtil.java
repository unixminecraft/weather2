package weather2.util;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;
import weather2.config.ConfigTornado;

public class WeatherUtil {

	private static HashMap<Block, Boolean> blockIDToUseMapping = new HashMap<Block, Boolean>();
	
    public static boolean isPaused() {
    	if (FMLClientHandler.instance().getClient().isGamePaused()) return true;
    	return false;
    }
    
    public static boolean isPausedSideSafe(World world) {
    	//return false if server side because it cant be paused legit
    	if (!world.isRemote) return false;
    	return isPausedForClient();
    }
    
    private static boolean isPausedForClient() {
    	if (FMLClientHandler.instance().getClient().isGamePaused()) return true;
    	return false;
    }
    
    @SuppressWarnings("deprecation")
	public static boolean shouldRemoveBlock(Block blockID)
    {
        if (blockID.getMaterial(blockID.getDefaultState()) == Material.WATER)
        {
            return false;
        }

        return true;
    }
    
    public static boolean isOceanBlock(Block blockID)
    {
        return false;
    }
	
    public static void doBlockList()
    {
        blockIDToUseMapping.clear();
        String[] splEnts = ConfigTornado.Storm_Tornado_GrabList.split(",");
        if (splEnts.length > 0) {
	        for (int i = 0; i < splEnts.length; i++)
	        {
	        	splEnts[i] = splEnts[i].trim();
	        }
        }
        
        blockIDToUseMapping.put(Blocks.AIR, false);

        Set<ResourceLocation> set = Block.REGISTRY.getKeys();
        Iterator<ResourceLocation> it = set.iterator();
        while (it.hasNext()) {
        	Object obj = it.next();
        	ResourceLocation tagName = ((ResourceLocation)obj);
        	Block block = (Block) Block.REGISTRY.getObject(tagName);
        	if (block != null)
            {
                boolean foundEnt = false;

                for (int j = 0; j < splEnts.length; j++)
                {
                	if (ConfigTornado.Storm_Tornado_GrabCond_List_PartialMatches) {
                		if (tagName.toString().contains(splEnts[j])) {
                			foundEnt = true;
                			break;
                		}
                	} else {
	                    Block blockEntry = (Block)Block.REGISTRY.getObject(new ResourceLocation(splEnts[j]));
	
	                    if (blockEntry != null && block == blockEntry)
	                    {
	                        foundEnt = true;
	                        break;
	                    }
                	}
                }

                blockIDToUseMapping.put(block, foundEnt);
            }
        }
    }

    public static boolean isAprilFoolsDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        return calendar.get(Calendar.MONTH) == Calendar.APRIL && calendar.get(Calendar.DAY_OF_MONTH) == 1;
    }
}
