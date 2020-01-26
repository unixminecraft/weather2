package weather2.client.entity;

import java.util.List;

import CoroUtil.util.Vec3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import weather2.ClientTickHandler;
import weather2.entity.EntityLightningBoltCustom;
import weather2.weathersystem.storm.WeatherObjectSandstorm;

@SideOnly(Side.CLIENT)
public class RenderLightningBoltCustom extends Render<EntityLightningBoltCustom>
{
    public RenderLightningBoltCustom(RenderManager renderManagerIn)
    {
        super(renderManagerIn);
    }

    /**
     * Actually renders the given argument. This is a synthetic bridge method, always casting down its argument and then
     * handing it off to a worker function which does the actual work. In all probabilty, the class Render is generic
     * (Render<T extends Entity>) and this method has signature public void func_76986_a(T entity, double d, double d1,
     * double d2, float f, float f1). But JAD is pre 1.5 so doe
     */
    @Override
    public void doRender(EntityLightningBoltCustom entity, double x, double y, double z, float entityYaw, float partialTicks)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder worldrenderer = tessellator.getBuffer();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 1);
        GlStateManager.disableCull();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        
        float r = 1F;
        float g = 1F;
        float b = 1F;
        float alpha = 0.4F;
        double sizeRadius = 0.3D;

        //temp - visualize sandstorm
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        Vec3 posPlayer = new Vec3(mc.player.posX, 0/*mc.player.posY*/, mc.player.posZ);
        WeatherObjectSandstorm sandstorm = ClientTickHandler.weatherManager.getClosestSandstormByIntensity(posPlayer);
        if (sandstorm != null) {
            List<Vec3> wat = sandstorm.getSandstormAsShape();
            entity.listVec.clear();
            for (Vec3 wat2 : wat) {
                Vec3d wat3 = new Vec3d(wat2.xCoord - player.posX, -10, wat2.zCoord - player.posZ);
                entity.listVec.add(wat3);
            }


        }

        for (int i = 0; i < entity.listVec.size() - 1; i++) {
        	Vec3d vec = entity.listVec.get(i);
        	Vec3d vec2 = entity.listVec.get(i+1);
            worldrenderer.pos(vec.x - sizeRadius, vec.y, vec.z - sizeRadius).color(r, g, b, alpha).endVertex();
            worldrenderer.pos(vec.x + sizeRadius, vec.y, vec.z - sizeRadius).color(r, g, b, alpha).endVertex();
            worldrenderer.pos(vec2.x + sizeRadius, vec2.y, vec2.z + sizeRadius).color(r, g, b, alpha).endVertex();
            worldrenderer.pos(vec2.x - sizeRadius, vec2.y, vec2.z + sizeRadius).color(r, g, b, alpha).endVertex();
        }

        //temp - visualize sandstorm
        Vec3d vec = entity.listVec.get(0);
        Vec3d vec2 = entity.listVec.get(entity.listVec.size()-1);
        worldrenderer.pos(vec.x - sizeRadius, vec.y, vec.z - sizeRadius).color(r, g, b, alpha).endVertex();
        worldrenderer.pos(vec.x + sizeRadius, vec.y, vec.z - sizeRadius).color(r, g, b, alpha).endVertex();
        worldrenderer.pos(vec2.x + sizeRadius, vec2.y, vec2.z + sizeRadius).color(r, g, b, alpha).endVertex();
        worldrenderer.pos(vec2.x - sizeRadius, vec2.y, vec2.z + sizeRadius).color(r, g, b, alpha).endVertex();
        tessellator.draw();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
    }

    /**
     * Returns the location of an entity's texture. Doesn't seem to be called unless you call Render.bindEntityTexture.
     */
    protected ResourceLocation getEntityTexture(EntityLightningBoltCustom entity)
    {
        return null;
    }
}