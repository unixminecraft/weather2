package CoroUtil.forge;

import CoroUtil.packet.PacketHelper;
import CoroUtil.util.CoroUtilPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class EventHandlerForge {
	
	@SubscribeEvent
	public void entityTick(LivingUpdateEvent event) {
		
		EntityLivingBase ent = event.getEntityLiving();
		if (!ent.world.isRemote) {
			if (ent instanceof EntityPlayer) {
				CoroUtilPlayer.trackPlayerForSpeed((EntityPlayer) ent);
			}
		}
	}

	@SubscribeEvent
	public void playerLoggedIn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
		PacketHelper.syncBlockLists();
	}
}
