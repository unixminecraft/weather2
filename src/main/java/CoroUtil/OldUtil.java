package CoroUtil;

import java.lang.reflect.Field;

import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3i;

public class OldUtil {
	
	public static String refl_mcp_Item_maxStackSize = "maxStackSize";
    public static String refl_s_Item_maxStackSize = "cq";
	
	public static String refl_mcp_EntityPlayer_itemInUse = "itemInUse";
	public static String refl_s_EntityPlayer_itemInUse = "field_71074_e";
	public static String refl_mcp_EntityPlayer_itemInUseCount = "itemInUseCount";
	public static String refl_s_EntityPlayer_itemInUseCount = "field_71072_f";
	public static String refl_mcp_FoodStats_foodLevel = "foodLevel";
	public static String refl_s_FoodStats_foodLevel = "field_75127_a";
	
	public static String refl_thrower_mcp = "thrower";
	public static String refl_thrower_obf = "field_70192_c";
	
	public static String refl_loadedChunks_mcp = "loadedChunks";
	public static String refl_loadedChunks_obf = "field_73245_g";
	
	public static String refl_curBlockDamageMP_mcp = "curBlockDamageMP";
	public static String refl_curBlockDamageMP_obf = "field_78770_f";
	
	public static boolean checkforMCP = true;
	public static boolean runningMCP = true;
	
	//Tropicraft reflection
	public static boolean hasTropicraft = true; //try reflection once
	public static String tcE = "tropicraft.entities.";
	public static String[] koaEnemyWhitelist = {""};
	public static Item fishingRodTropical;
	public static Item dagger;
	public static Item leafBall;
	
	
	public OldUtil() {
		//wut
	}
	
	public static void check() {
		checkforMCP = false;
		try {
			//runningMCP = getPrivateValue(MinecraftServer.class, MinecraftServer.getServer(), "tickables") != null;
			//runningMCP = getPrivateValue(MinecraftServer.class, FMLCommonHandler.instance().getMinecraftServerInstance(), "motd") != null;
			runningMCP = getPrivateValue(Vec3i.class, null, "NULL_VECTOR") != null;
		} catch (Exception e) {
			runningMCP = false;
			System.out.println("CoroAI: 'tickables' field not found, mcp mode disabled");
		}
	}
	
	public static Object getPrivateValueSRGMCP(Class<?> var0, Object var1, String srg, String mcp) {
    	if (checkforMCP) check();
    	try {
    		
    		if (!runningMCP) {
    			return getPrivateValue(var0, var1, srg);
    		} else {
    			return getPrivateValue(var0, var1, mcp);
    		}
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    public static Object getPrivateValue(Class<?> var0, Object var1, String var2)/* throws IllegalArgumentException, SecurityException, NoSuchFieldException*/
    {
        try
        {
            Field var3 = var0.getDeclaredField(var2);
            var3.setAccessible(true);
            return var3.get(var1);
        }
        catch (Exception var4)
        {
            return null;
        }
    }
    
}
