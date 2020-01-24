package CoroUtil.forge;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.registry.EntityRegistry;

@Mod.EventBusSubscriber(modid = CoroUtil.modID)
public class CommonProxy implements IGuiHandler {
	
	public World mainWorld;
	
	public CoroUtil mod;
	
	public CommonProxy() {
		
	}
	
	public void init(CoroUtil pMod) {
		
		mod = pMod;
	}
	
	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		
		return null;
	}
	
	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		
		return null;
	}
	
	public void addMapping(Class<? extends Entity> par0Class, String par1Str, int entityId, int distSync, int tickRateSync, boolean syncMotion) {
		
		EntityRegistry.registerModEntity(new ResourceLocation(CoroUtil.modID, par1Str), par0Class, par1Str, entityId, CoroUtil.instance, distSync, tickRateSync, syncMotion);
	}
}
