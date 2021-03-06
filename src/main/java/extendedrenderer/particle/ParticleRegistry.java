package extendedrenderer.particle;

import java.util.ArrayList;
import java.util.List;

import extendedrenderer.ExtendedRenderer;
import extendedrenderer.render.RotatingParticleManager;
import extendedrenderer.shader.MeshBufferManagerFoliage;
import extendedrenderer.shader.MeshBufferManagerParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;

public class ParticleRegistry {

	public static TextureAtlasSprite smoke;
	public static TextureAtlasSprite cloud256;
	public static TextureAtlasSprite cloud256_fire;
	public static TextureAtlasSprite cloud256_test;
	public static TextureAtlasSprite cloud256_6;
	public static TextureAtlasSprite downfall3;
	public static TextureAtlasSprite chicken;
	public static TextureAtlasSprite potato;
	public static TextureAtlasSprite leaf;
	public static TextureAtlasSprite rain_white;
	public static TextureAtlasSprite snow;
	public static TextureAtlasSprite tumbleweed;
	public static TextureAtlasSprite debris_1;
	public static TextureAtlasSprite debris_2;
	public static TextureAtlasSprite debris_3;
	public static List<TextureAtlasSprite> listFish = new ArrayList<>();
	public static List<TextureAtlasSprite> listSeaweed = new ArrayList<>();
	
	public static void init(TextureStitchEvent.Pre event) {
		
		//optifine breaks (removes) forge added method setTextureEntry, dont use it

		MeshBufferManagerParticle.cleanup();
		MeshBufferManagerFoliage.cleanup();

		event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/white"));
		cloud256 = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/cloud256"));
		cloud256_fire = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/cloud256_fire"));
		cloud256_test = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/cloud256_test"));
		//ground splash
		cloud256_6 = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/cloud256_6"));
		downfall3 = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/downfall3"));
		chicken = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/chicken"));
		potato = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/potato"));
		leaf = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/leaf"));
		event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/test_texture"));
		event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/white_square"));
		rain_white = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/rain_white"));
		snow = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/snow"));
		tumbleweed = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/tumbleweed"));
		debris_1 = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/debris_1"));
		debris_2 = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/debris_2"));
		debris_3 = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/debris_3"));
		event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/grass"));
	}

	public static void initPost(TextureStitchEvent.Post event) {
		if (RotatingParticleManager.useShaders) {
			RotatingParticleManager.forceShaderReset = true;
		}
	}
}
