package CoroUtil.packet;

import java.io.IOException;

import CoroUtil.forge.CoroUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

public class PacketHelper {
	
	//1.7 plan, for the existing packets, we add the old 'channel' as a command string, so we can use 1 event channel, then just convert existing code to buffer way
	
	//in handler, WE MUST MODIFY TO READ THE STRING FOR COMMAND!
	
	//dont forget to test tile datawatchers
	
	//modify to be fully nbt! less headache!
	
	public static NBTTagCompound readNBTTagCompound(ByteBuf fullBuffer) throws IOException
    {
		return ByteBufUtils.readTag(fullBuffer);
    }
	
	public static FMLProxyPacket getNBTPacket(NBTTagCompound parNBT, String parChannel) {
        ByteBuf byteBuf = Unpooled.buffer();
        
        try {
        	ByteBufUtils.writeTag(byteBuf, parNBT);
        } catch (Exception ex) {
        	ex.printStackTrace();
        }

        return new FMLProxyPacket(new PacketBuffer(byteBuf), parChannel);
    }

	private static FMLProxyPacket getPacketForUpdateBlockList() {
		NBTTagCompound data = new NBTTagCompound();
		data.setString("command", "UpdateBlockList");
		return getNBTPacket(data, CoroUtil.eventChannelName);
	}

	public static void syncBlockLists() {
		if (getServerPlayerCount() <= 0) return;
		CoroUtil.eventChannel.sendToAll(PacketHelper.getPacketForUpdateBlockList());
	}

	private static int getServerPlayerCount() {
		if (FMLCommonHandler.instance().getMinecraftServerInstance() == null) return 0;
		if (FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList() == null) return 0;
		return FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getCurrentPlayerCount();
	}
}
