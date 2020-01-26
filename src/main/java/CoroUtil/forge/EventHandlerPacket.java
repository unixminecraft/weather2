package CoroUtil.forge;

import CoroUtil.packet.INBTPacketHandler;
import CoroUtil.packet.PacketHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EventHandlerPacket {
	
	//if im going to load nbt, i probably should package it at the VERY end of the packet so it loads properly
	//does .payload continue from where i last read or is it whole thing?
	//maybe i should just do nbt only
	
	//changes from 1.6.4 to 1.7.2:
	//all nbt now:
	//- inv writes stack to nbt, dont use buffer
	//- any sending code needs a full reverification that it matches up with how its received in this class
	//- READ ABOVE ^
	//- CoroAI_Inv could be factored out and replaced with CoroAI_Ent, epoch entities use it this way

	@SideOnly(Side.CLIENT)
	public World getClientWorld() {
		return Minecraft.getMinecraft().world;
	}
	
	@SideOnly(Side.CLIENT)
	public EntityPlayer getClientPlayer() {
		return Minecraft.getMinecraft().player;
	}
	
	@SideOnly(Side.CLIENT)
	public INBTPacketHandler getClientDataInterface() {
		if (Minecraft.getMinecraft().currentScreen instanceof INBTPacketHandler) {
			return (INBTPacketHandler)Minecraft.getMinecraft().currentScreen;
		}
		return null;
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onPacketFromServer(FMLNetworkEvent.ClientCustomPacketEvent event) {
		try {
			final NBTTagCompound nbt = PacketHelper.readNBTTagCompound(event.getPacket().payload());

			String command = nbt.getString("command");

			if (command.equals("Ent_Motion")) {

				final int entID = nbt.getInteger("entityID");

				Minecraft.getMinecraft().addScheduledTask(() -> {

					Entity entity = getClientWorld().getEntityByID(entID);
					if (entity != null) {
						entity.motionX += nbt.getDouble("motionX");
						entity.motionY += nbt.getDouble("motionY");
						entity.motionZ += nbt.getDouble("motionZ");
					}
				});
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
