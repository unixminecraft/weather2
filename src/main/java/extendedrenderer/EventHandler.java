package extendedrenderer;

import org.lwjgl.opengl.GL11;

import CoroUtil.config.ConfigCoroUtil;
import CoroUtil.config.ConfigCoroUtilAdvanced;
import CoroUtil.forge.CULog;
import CoroUtil.util.CoroUtilBlockLightCache;
import extendedrenderer.particle.ParticleRegistry;
import extendedrenderer.particle.ShaderManager;
import extendedrenderer.render.RotatingParticleManager;
import extendedrenderer.shader.ShaderEngine;
import extendedrenderer.shader.ShaderListenerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EventHandler {

	
    private static World lastWorld;

    private static int mip_min = 0;
    private static int mip_mag = 0;


    //a hack to enable fog for particles when weather2 sandstorm is active
    public static float sandstormFogAmount = 0F;

    //initialized at post init after configs loaded in
    public static boolean foliageUseLast;

    private static boolean flagFoliageUpdate = false;

    private static boolean lastLightningBoltLightState = false;

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void tickClient(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {

            Minecraft mc = Minecraft.getMinecraft();
            if (mc.world != null) {
                if (!isPaused()) {
                    ExtendedRenderer.rotEffRenderer.updateEffects();

                    boolean lightningActive = mc.world.getLastLightningBolt() > 0;

                    if (mc.world.getTotalWorldTime() % 2 == 0 || lightningActive != lastLightningBoltLightState) {
                        CoroUtilBlockLightCache.clear();
                    }

                    lastLightningBoltLightState = lightningActive;
                }
            }

            if (ConfigCoroUtil.foliageShaders != foliageUseLast) {
                foliageUseLast = ConfigCoroUtil.foliageShaders;
                flagFoliageUpdate = true;
            }

            if (flagFoliageUpdate) {
                CULog.dbg("CoroUtil detected a need to reload resource packs, initiating");
                flagFoliageUpdate = false;
                Minecraft.getMinecraft().refreshResources();
            }
        }
    }

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
    public void worldRender(RenderWorldLastEvent event)
    {
        if (!ConfigCoroUtil.useEntityRenderHookForShaders) {
            EventHandler.hookRenderShaders(event.getPartialTicks());
        }
    }

    public static boolean queryUseOfShaders() {
        RotatingParticleManager.useShaders = ShaderManager.canUseShadersInstancedRendering();

        if (ConfigCoroUtil.forceShadersOff) {
            RotatingParticleManager.useShaders = false;
        }

        return RotatingParticleManager.useShaders;
    }

    @SideOnly(Side.CLIENT)
    public static void hookRenderShaders(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.world == null || mc.player == null) return;

        //update world reference and clear old effects on world change or on no world
        if (lastWorld != mc.world) {
            CULog.log("CoroUtil: resetting rotating particle renderer");
            ExtendedRenderer.rotEffRenderer.clearEffects(mc.world);
            lastWorld = mc.world;
        }

        EntityRenderer er = mc.entityRenderer;

        if (!ConfigCoroUtilAdvanced.disableParticleRenderer) {

            //Rotating particles hook, copied and adjusted code from ParticleManagers render context in EntityRenderer

            er.enableLightmap();
            mc.mcProfiler.endStartSection("litParticles");
            ExtendedRenderer.rotEffRenderer.renderLitParticles((Entity) mc.getRenderViewEntity(), (float) partialTicks);
            RenderHelper.disableStandardItemLighting();
            mc.mcProfiler.endStartSection("particles");
            queryUseOfShaders();

            if (RotatingParticleManager.forceShaderReset) {

                CULog.log("Extended Renderer: Resetting shaders");

                RotatingParticleManager.forceShaderReset = false;
                ShaderEngine.cleanup();
                ShaderListenerRegistry.postReset();
                ExtendedRenderer.foliageRenderer.foliage.clear();
                ShaderEngine.renderer = null;
                //ExtendedRenderer.foliageRenderer.needsUpdate = true;
                //ExtendedRenderer.foliageRenderer.vbo2BufferPos = 0;
                ShaderManager.resetCheck();
            }

            if (RotatingParticleManager.useShaders && ShaderEngine.renderer == null) {

                //currently for if shader compiling fails, which is an ongoing issue for some machines...
                if (!ShaderEngine.init()) {

                    CULog.log("Extended Renderer: Shaders failed to initialize");

                    ShaderManager.disableShaders();
                    RotatingParticleManager.useShaders = false;
                } else {
                    CULog.log("Extended Renderer: Initialized instanced rendering shaders");
                    ShaderListenerRegistry.postInit();
                }
            }

            preShaderRender(mc.getRenderViewEntity(), partialTicks);

            if (ConfigCoroUtil.foliageShaders) {
                ExtendedRenderer.foliageRenderer.render(mc.getRenderViewEntity(), partialTicks);
            }


            ExtendedRenderer.rotEffRenderer.renderParticles(mc.getRenderViewEntity(), partialTicks);


            postShaderRender(mc.getRenderViewEntity(), partialTicks);

            er.disableLightmap();
        }
    }

    @SideOnly(Side.CLIENT)
    private static void preShaderRender(Entity entityIn, float partialTicks) {

        Minecraft mc = Minecraft.getMinecraft();
        EntityRenderer er = mc.entityRenderer;

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.004F);

        er.setupFog(0, partialTicks);
        GlStateManager.disableCull();

        CoroUtilBlockLightCache.brightnessPlayer = CoroUtilBlockLightCache.getBrightnessFromLightmap(mc.world, (float)entityIn.posX, (float)entityIn.posY, (float)entityIn.posZ);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        mip_min = 0;
        mip_mag = 0;

        //fix mipmapping making low alpha transparency particles dissapear based on distance, window size, particle size
        if (!ConfigCoroUtilAdvanced.disableMipmapFix) {
            mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            mip_min = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
            mip_mag = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        }
    }

    @SideOnly(Side.CLIENT)
    private static void postShaderRender(Entity entityIn, float partialTicks) {

        //restore original mipmap state
        if (!ConfigCoroUtilAdvanced.disableMipmapFix) {
            Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mip_min);
            GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mip_mag);
        }

        GlStateManager.enableCull();

            GlStateManager.disableFog();

        GlStateManager.depthMask(false);
        GlStateManager.disableBlend();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
    }
	
	@SideOnly(Side.CLIENT)
	private boolean isPaused() {
        if (FMLClientHandler.instance().getClient().isGamePaused()) return true;
    	return false;
    }
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void registerIcons(TextureStitchEvent.Pre event) {
		ParticleRegistry.init(event);
	}

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void registerIconsPost(TextureStitchEvent.Post event) {
        ParticleRegistry.initPost(event);
    }
}
