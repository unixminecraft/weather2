package weather2;

import extendedrenderer.render.FoliageRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIMoveIndoors;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.EntityViewRenderEvent.FogColors;
import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.world.WorldEvent.Save;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import weather2.api.WeatherUtilData;
import weather2.client.SceneEnhancer;
import weather2.client.foliage.FoliageEnhancerShader;
import weather2.config.ConfigFoliage;
import weather2.config.ConfigMisc;
import weather2.entity.AI.EntityAIMoveIndoorsStorm;
import weather2.util.UtilEntityBuffsMini;
import weather2.weathersystem.wind.WindManager;

public class EventHandlerForge {

	@SubscribeEvent
	public void worldSave(Save event) {
		Weather.writeOutData(false);
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
    public void worldRender(RenderWorldLastEvent event)
    {

		if (ConfigMisc.Client_PotatoPC_Mode) return;

		ClientTickHandler.checkClientWeather();
		ClientTickHandler.weatherManager.tickRender(event.getPartialTicks());

		FoliageRenderer.radialRange = ConfigFoliage.foliageShaderRange;
    }
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void registerIcons(TextureStitchEvent.Pre event) {
		
		//optifine breaks (removes) forge added method setTextureEntry, dont use it
		
		ClientProxy.radarIconRain = event.getMap().registerSprite(new ResourceLocation(Weather.modID + ":radar/radar_icon_rain"));
		ClientProxy.radarIconLightning = event.getMap().registerSprite(new ResourceLocation(Weather.modID + ":radar/radar_icon_lightning"));
		ClientProxy.radarIconWind = event.getMap().registerSprite(new ResourceLocation(Weather.modID + ":radar/radar_icon_wind"));
		ClientProxy.radarIconHail = event.getMap().registerSprite(new ResourceLocation(Weather.modID + ":radar/radar_icon_hail"));
		ClientProxy.radarIconTornado = event.getMap().registerSprite(new ResourceLocation(Weather.modID + ":radar/radar_icon_tornado"));
		ClientProxy.radarIconCyclone = event.getMap().registerSprite(new ResourceLocation(Weather.modID + ":radar/radar_icon_cyclone"));
		ClientProxy.radarIconSandstorm = event.getMap().registerSprite(new ResourceLocation(Weather.modID + ":radar/radar_icon_sandstorm"));
		
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
    public void onFogColors(FogColors event) {

		if (ConfigMisc.Client_PotatoPC_Mode) return;
		
        if (SceneEnhancer.isFogOverridding()) {
			//backup original fog colors that are actively being adjusted based on time of day
			SceneEnhancer.stormFogRedOrig = event.getRed();
			SceneEnhancer.stormFogGreenOrig = event.getGreen();
			SceneEnhancer.stormFogBlueOrig = event.getBlue();
        	event.setRed(SceneEnhancer.stormFogRed);
        	event.setGreen(SceneEnhancer.stormFogGreen);
        	event.setBlue(SceneEnhancer.stormFogBlue);
			GlStateManager.setFog(GlStateManager.FogMode.LINEAR);
        }
		
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onFogRender(RenderFogEvent event) {

		if (ConfigMisc.Client_PotatoPC_Mode) return;

		if (SceneEnhancer.isFogOverridding()) {
			GlStateManager.setFog(GlStateManager.FogMode.LINEAR);
			if (event.getFogMode() == -1) {
				GlStateManager.setFogStart(SceneEnhancer.stormFogStartClouds);
	            GlStateManager.setFogEnd(SceneEnhancer.stormFogEndClouds);
			} else {
				GlStateManager.setFogStart(SceneEnhancer.stormFogStart);
	            GlStateManager.setFogEnd(SceneEnhancer.stormFogEnd);
			}
        }
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onRenderTick(TickEvent.RenderTickEvent event) {
		SceneEnhancer.renderTick(event);
	}

	@SubscribeEvent
	public void onEntityCreatedOrLoaded(EntityJoinWorldEvent event) {
		if (event.getEntity().world.isRemote) return;

		if (ConfigMisc.Villager_MoveInsideForStorms) {
			if (event.getEntity() instanceof EntityVillager) {
				EntityVillager ent = (EntityVillager) event.getEntity();
				UtilEntityBuffsMini.replaceTaskIfMissing(ent, EntityAIMoveIndoors.class, EntityAIMoveIndoorsStorm.class, 2);
			}
		}
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void registerIcons(TextureStitchEvent.Post event) {
		FoliageEnhancerShader.setupReplacers();
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void modelBake(ModelBakeEvent event) {
		FoliageEnhancerShader.modelBakeEvent(event);
	}

	@SubscribeEvent
	public void onEntityLivingUpdate(LivingEvent.LivingUpdateEvent event) {

		Entity ent = event.getEntity();
		if (!ent.world.isRemote) {
			if (WeatherUtilData.isWindAffected(ent)) {
				WindManager windMan = ServerTickHandler.getWeatherSystemForDim(ent.world.provider.getDimension()).windMan;
				windMan.applyWindForceNew(ent, 1F / 20F, 0.5F);
			}
		}
	}
}
