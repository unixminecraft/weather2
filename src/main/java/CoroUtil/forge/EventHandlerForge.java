package CoroUtil.forge;

import CoroUtil.packet.PacketHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

public class EventHandlerForge {

	@SubscribeEvent
	public void playerLoggedIn(PlayerLoggedInEvent event) {
		PacketHelper.syncBlockLists();
	}
}
