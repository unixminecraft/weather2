package weather2;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import weather2.config.ConfigFoliage;
import weather2.config.ConfigMisc;

public class EventHandlerFML {

	private static boolean sleepFlag = false;
	private static boolean wasRain = false;
	private static int rainTime = 0;
	private static boolean wasThunder = false;
	private static int thunderTime = 0;

	//initialized at post init after configs loaded in
	public static boolean extraGrassLast;

	@SubscribeEvent
	public void tickWorld(WorldTickEvent event) {
		if (event.phase == Phase.START) {

		}
	}

	@SubscribeEvent
	public void tickServer(ServerTickEvent event) {

		if (event.phase == Phase.START) {
			ServerTickHandler.onTickInGame();
		}

		if (ConfigMisc.Global_Overcast_Prevent_Rain_Reset_On_Sleep) {
			WorldServer world = DimensionManager.getWorld(0);
			if (world != null) {
				if (event.phase == Phase.START) {
					if (world.areAllPlayersAsleep()) {
						sleepFlag = true;
						wasRain = world.getWorldInfo().isRaining();
						wasThunder = world.getWorldInfo().isThundering();
						rainTime = world.getWorldInfo().getRainTime();
						thunderTime = world.getWorldInfo().getThunderTime();
					} else {
						sleepFlag = false;
					}
				} else {
					if (sleepFlag) {
						world.getWorldInfo().setRaining(wasRain);
						world.getWorldInfo().setRainTime(rainTime);
						world.getWorldInfo().setThundering(wasThunder);
						world.getWorldInfo().setThunderTime(thunderTime);
					}
				}
			}
		}

	}

	@SubscribeEvent
	public void tickClient(ClientTickEvent event) {
		if (event.phase == Phase.START) {
			try {
				ClientProxy.clientTickHandler.onTickInGame();

				if (extraGrassLast != ConfigFoliage.extraGrass) {
					extraGrassLast = ConfigFoliage.extraGrass;
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	@SubscribeEvent
	public void playerLoggedIn(PlayerLoggedInEvent event) {
		if (event.player instanceof EntityPlayerMP) {
			ServerTickHandler.syncServerConfigToClientPlayer((EntityPlayerMP) event.player);
		}
	}
}
