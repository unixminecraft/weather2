package extendedrenderer.render;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;

import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.annotation.Nullable;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import CoroUtil.config.ConfigCoroUtil;
import extendedrenderer.particle.ParticleRegistry;
import extendedrenderer.particle.ShaderManager;
import extendedrenderer.particle.entity.EntityRotFX;
import extendedrenderer.shader.InstancedMeshParticle;
import extendedrenderer.shader.Matrix4fe;
import extendedrenderer.shader.MeshBufferManagerParticle;
import extendedrenderer.shader.ShaderEngine;
import extendedrenderer.shader.ShaderProgram;
import extendedrenderer.shader.Transformation;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEmitter;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ICrashReportDetail;
import net.minecraft.entity.Entity;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RotatingParticleManager {
	
	private static final ResourceLocation PARTICLE_TEXTURES = new ResourceLocation("textures/particle/particles.png");
	/** Reference to the World object. */
	private World world;
	/**
	 * Second dimension: 0 = GlStateManager.depthMask false aka transparent textures, 1 = true
	 */
	private final LinkedHashMap<TextureAtlasSprite, List<ArrayDeque<Particle>[][]>> fxLayers = new LinkedHashMap<>();
	private final Queue<ParticleEmitter> particleEmitters = Queues.<ParticleEmitter>newArrayDeque();
	private final TextureManager renderer;
	private final Queue<Particle> queueEntityFX = Queues.<Particle>newArrayDeque();
	
	// ExtendedRenderer Additions
	
	public static int debugParticleRenderCount;
	
	public static boolean useShaders;
	
	private static FloatBuffer viewMatrixBuffer = BufferUtils.createFloatBuffer(16);
	
	public static boolean forceShaderReset = false;
	
	public RotatingParticleManager(World worldIn, TextureManager rendererIn) {
		
		this.world = worldIn;
		this.renderer = rendererIn;
	}
	
	@SuppressWarnings("unchecked")
	private void initNewArrayData(TextureAtlasSprite sprite) {
		
		List<ArrayDeque<Particle>[][]> list = new ArrayList<>();
		
		// main default layer
		list.add(0, new ArrayDeque[4][]);
		
		// layer for tornado funnel
		list.add(1, new ArrayDeque[4][]);
		
		// close up stuff like precipitation
		list.add(2, new ArrayDeque[4][]);
		
		for(ArrayDeque<Particle>[][] entry : list) {
			for(int i = 0; i < 4; ++i) {
				entry[i] = new ArrayDeque[2];
				
				for(int j = 0; j < 2; ++j) {
					entry[i][j] = Queues.newArrayDeque();
				}
			}
		}
		
		fxLayers.put(sprite, list);
	}
	
	public void addEffect(Particle effect) {
		
		if(effect == null)
			return;
		this.queueEntityFX.add(effect);
	}
	
	public void updateEffects() {
		
		for(int i = 0; i < 4; ++i) {
			this.updateEffectLayer(i);
		}
		
		if(!this.particleEmitters.isEmpty()) {
			List<ParticleEmitter> list = Lists.<ParticleEmitter>newArrayList();
			
			for(ParticleEmitter particleemitter : this.particleEmitters) {
				particleemitter.onUpdate();
				
				if(!particleemitter.isAlive()) {
					list.add(particleemitter);
				}
			}
			
			this.particleEmitters.removeAll(list);
		}
		
		if(!this.queueEntityFX.isEmpty()) {
			
			for(Particle particle = (Particle) this.queueEntityFX.poll(); particle != null; particle = (Particle) this.queueEntityFX.poll()) {
				int j = particle.getFXLayer();
				int k = particle.shouldDisableDepth() ? 0 : 1;
				
				int renderOrder = 0;
				if(particle instanceof EntityRotFX) {
					renderOrder = ((EntityRotFX) particle).renderOrder;
				}
				
				if(!fxLayers.containsKey(particle.particleTexture)) {
					initNewArrayData(particle.particleTexture);
				}
				
				ArrayDeque<Particle>[][] entry = fxLayers.get(particle.particleTexture).get(renderOrder);
				
				if(entry[j][k].size() >= 16384) {
					entry[j][k].getFirst().setExpired();
					entry[j][k].removeFirst();
				}
				
				entry[j][k].add(particle);
			}
		}
	}
	
	private void updateEffectLayer(int layer) {
		
		for(int i = 0; i < 2; ++i) {
			for(Map.Entry<TextureAtlasSprite, List<ArrayDeque<Particle>[][]>> entry1 : fxLayers.entrySet()) {
				for(ArrayDeque<Particle>[][] entry2 : entry1.getValue()) {
					this.tickParticleList(entry2[layer][i]);
				}
			}
		}
	}
	
	private void tickParticleList(Queue<Particle> p_187240_1_) {
		
		if(!p_187240_1_.isEmpty()) {
			Iterator<Particle> iterator = p_187240_1_.iterator();
			
			while(iterator.hasNext()) {
				Particle particle = iterator.next();
				this.tickParticle(particle);
				
				if(!particle.isAlive()) {
					iterator.remove();
				}
			}
		}
	}
	
	private void tickParticle(final Particle particle) {
		
		try {
			particle.onUpdate();
		}
		catch(Throwable throwable) {
			CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Ticking Rotating Particle");
			CrashReportCategory crashreportcategory = crashreport.makeCategory("Particle being ticked");
			final int i = particle.getFXLayer();
			crashreportcategory.addDetail("Rotating Particle", new ICrashReportDetail<String>() {
				
				public String call() throws Exception {
					
					return particle.toString();
				}
			});
			crashreportcategory.addDetail("Particle Type", new ICrashReportDetail<String>() {
				
				public String call() throws Exception {
					
					return i == 0 ? "MISC_TEXTURE" : (i == 1 ? "TERRAIN_TEXTURE" : (i == 3 ? "ENTITY_PARTICLE_TEXTURE" : "Unknown - " + i));
				}
			});
			throw new ReportedException(crashreport);
		}
	}
	
	/**
	 * Renders all current particles. Args player, partialTickTime
	 */
	public void renderParticles(Entity entityIn, float partialTicks) {
		
		boolean useParticleShaders = useShaders && ConfigCoroUtil.particleShaders;
		
		float f = ActiveRenderInfo.getRotationX();
		float f1 = ActiveRenderInfo.getRotationZ();
		float f2 = ActiveRenderInfo.getRotationYZ();
		float f3 = ActiveRenderInfo.getRotationXY();
		float f4 = ActiveRenderInfo.getRotationXZ();
		Particle.interpPosX = entityIn.lastTickPosX + (entityIn.posX - entityIn.lastTickPosX) * (double) partialTicks;
		Particle.interpPosY = entityIn.lastTickPosY + (entityIn.posY - entityIn.lastTickPosY) * (double) partialTicks;
		Particle.interpPosZ = entityIn.lastTickPosZ + (entityIn.posZ - entityIn.lastTickPosZ) * (double) partialTicks;
		Particle.cameraViewDir = entityIn.getLook(partialTicks);
		
		debugParticleRenderCount = 0;
		
		if(useParticleShaders) {
			// temp render ordering setup, last to first
			// background stuff
			MeshBufferManagerParticle.setupMeshForParticleIfMissing(ParticleRegistry.cloud256_test);
			MeshBufferManagerParticle.setupMeshForParticleIfMissing(ParticleRegistry.cloud256_fire);
			MeshBufferManagerParticle.setupMeshForParticleIfMissing(ParticleRegistry.cloud256);
			// foreground stuff
			MeshBufferManagerParticle.setupMeshForParticleIfMissing(ParticleRegistry.downfall3);
			MeshBufferManagerParticle.setupMeshForParticleIfMissing(ParticleRegistry.cloud256_6); // ground splash
			MeshBufferManagerParticle.setupMeshForParticleIfMissing(ParticleRegistry.rain_white);
			MeshBufferManagerParticle.setupMeshForParticleIfMissing(ParticleRegistry.snow);
			MeshBufferManagerParticle.setupMeshForParticleIfMissing(ParticleRegistry.leaf);
			MeshBufferManagerParticle.setupMeshForParticleIfMissing(ParticleRegistry.debris_1);
			MeshBufferManagerParticle.setupMeshForParticleIfMissing(ParticleRegistry.debris_2);
			MeshBufferManagerParticle.setupMeshForParticleIfMissing(ParticleRegistry.debris_3);
			MeshBufferManagerParticle.setupMeshForParticleIfMissing(ParticleRegistry.tumbleweed);
		}
		
		Transformation transformation = null;
		Matrix4fe viewMatrix = null;
		
		int glCalls = 0;
		int trueRenderCount = 0;
		int particles = 0;
		
		if(useParticleShaders) {
			ShaderProgram shaderProgram = ShaderEngine.renderer.getShaderProgram("particle");
			transformation = ShaderEngine.renderer.transformation;
			shaderProgram.bind();
			Matrix4fe projectionMatrix = new Matrix4fe();
			FloatBuffer buf = BufferUtils.createFloatBuffer(16);
			GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, buf);
			buf.rewind();
			Matrix4fe.get(projectionMatrix, 0, buf);
			
			viewMatrix = new Matrix4fe();
			FloatBuffer buf2 = BufferUtils.createFloatBuffer(16);
			GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buf2);
			buf2.rewind();
			Matrix4fe.get(viewMatrix, 0, buf2);
			
			Matrix4fe modelViewMatrix = projectionMatrix.mul(viewMatrix);
			
			shaderProgram.setUniformEfficient("modelViewMatrixCamera", modelViewMatrix, viewMatrixBuffer);
			
			shaderProgram.setUniform("texture_sampler", 0);
			int glFogMode = GL11.glGetInteger(GL11.GL_FOG_MODE);
			int modeIndex = 0;
			if(glFogMode == GL11.GL_LINEAR) {
				modeIndex = 0;
			}
			else if(glFogMode == GL11.GL_EXP) {
				modeIndex = 1;
			}
			else if(glFogMode == GL11.GL_EXP2) {
				modeIndex = 2;
			}
			shaderProgram.setUniform("fogmode", modeIndex);
			
		}
		
		// do sprite/mesh list
		for(Map.Entry<TextureAtlasSprite, List<ArrayDeque<Particle>[][]>> entry1 : fxLayers.entrySet()) {
			
			InstancedMeshParticle mesh = null;
			if(useParticleShaders) {
				mesh = MeshBufferManagerParticle.getMesh(entry1.getKey());
				// TODO: register if missing, maybe relocate this
				if(mesh == null) {
					MeshBufferManagerParticle.setupMeshForParticle(entry1.getKey());
					mesh = MeshBufferManagerParticle.getMesh(entry1.getKey());
				}
			}
			
			if(mesh != null || !useParticleShaders) {
				// do cloud layer, then funnel layer
				for(ArrayDeque<Particle>[][] entry : entry1.getValue()) {
					// do each texture mode, 0 and 1 are the only ones used now
					for(int i_nf = 0; i_nf < 3; ++i_nf) {
						final int i = i_nf;
						
						// do non depth mask (for transparent ones), then depth mask
						for(int j = 0; j < 2; ++j) {
							if(!entry[i][j].isEmpty()) {
								switch (j) {
									
									/**
									 * TODO: make sure alpha test toggling doesnt interfere with anything else with it on, it speeds up
									 * rendering of non transparent particles, does it also allow for full transparent particle pixels?
									 */
									
									case 0:
										GlStateManager.depthMask(false);
										break;
									case 1:
										GlStateManager.depthMask(true);
								}
								
								switch (i) {
									case 0:
									default:
										this.renderer.bindTexture(PARTICLE_TEXTURES);
										break;
									case 1:
										this.renderer.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
								}
								
								if(useParticleShaders) {
									
									// all VBO VertexAttribArrays must be enabled for so glDrawElementsInstanced can use them, so might
										// as well enable all at same time
									mesh.initRender();
									mesh.initRenderVBO1();
									// also resets position
									mesh.instanceDataBuffer.clear();
									mesh.curBufferPos = 0;
									particles = entry[i][j].size();
									
									for(final Particle particle : entry[i][j]) {
										if(particle instanceof EntityRotFX) {
											EntityRotFX part = (EntityRotFX) particle;
											part.renderParticleForShader(mesh, transformation, viewMatrix, entityIn, partialTicks, f, f4, f1, f2, f3);
										}
									}
									
									mesh.instanceDataBuffer.limit(mesh.curBufferPos * InstancedMeshParticle.INSTANCE_SIZE_FLOATS);
									
									OpenGlHelper.glBindBuffer(GL_ARRAY_BUFFER, mesh.instanceDataVBO);
									ShaderManager.glBufferData(GL_ARRAY_BUFFER, mesh.instanceDataBuffer, GL_DYNAMIC_DRAW);
									
									ShaderManager.glDrawElementsInstanced(GL_TRIANGLES, mesh.getVertexCount(), GL_UNSIGNED_INT, 0, mesh.curBufferPos);
									
									glCalls++;
									trueRenderCount += mesh.curBufferPos;
									
									OpenGlHelper.glBindBuffer(GL_ARRAY_BUFFER, 0);
									
									mesh.endRenderVBO1();
									mesh.endRender();
								}
								else {
									Tessellator tessellator = Tessellator.getInstance();
									BufferBuilder vertexbuffer = tessellator.getBuffer();
									vertexbuffer.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
									
									for(final Particle particle : entry[i][j]) {
										particle.renderParticle(vertexbuffer, entityIn, partialTicks, f, f4, f1, f2, f3);
										debugParticleRenderCount++;
									}
									
									tessellator.draw();
								}
								
							}
						}
					}
				}
			}
		}
		
		if(useParticleShaders) {
			ShaderEngine.renderer.getShaderProgram("particle").unbind();
		}
		
		if(ConfigCoroUtil.debugShaders && world.getTotalWorldTime() % 60 == 0) {
			System.out.println("particles: " + particles);
			System.out.println("debugParticleRenderCount: " + debugParticleRenderCount);
			System.out.println("trueRenderCount: " + trueRenderCount);
			System.out.println("glCalls: " + glCalls);
		}
	}
	
	public void renderLitParticles(Entity entityIn, float partialTick) {
		
		float f1 = MathHelper.cos(entityIn.rotationYaw * 0.017453292F);
		float f2 = MathHelper.sin(entityIn.rotationYaw * 0.017453292F);
		float f3 = -f2 * MathHelper.sin(entityIn.rotationPitch * 0.017453292F);
		float f4 = f1 * MathHelper.sin(entityIn.rotationPitch * 0.017453292F);
		float f5 = MathHelper.cos(entityIn.rotationPitch * 0.017453292F);
		
		for(Map.Entry<TextureAtlasSprite, List<ArrayDeque<Particle>[][]>> entry1 : fxLayers.entrySet()) {
			for(ArrayDeque<Particle>[][] entry : entry1.getValue()) {
				for(int i = 0; i < 2; ++i) {
					Queue<Particle> queue = entry[3][i];
					if(!queue.isEmpty()) {
						Tessellator tessellator = Tessellator.getInstance();
						BufferBuilder vertexbuffer = tessellator.getBuffer();
						
						for(Particle particle : queue) {
							particle.renderParticle(vertexbuffer, entityIn, partialTick, f1, f5, f2, f3, f4);
						}
					}
				}
			}
		}
	}
	
	public void clearEffects(@Nullable World worldIn) {
		
		this.world = worldIn;
		
		// shader way
		for(Map.Entry<TextureAtlasSprite, List<ArrayDeque<Particle>[][]>> entry1 : fxLayers.entrySet()) {
			for(ArrayDeque<Particle>[][] entry : entry1.getValue()) {
				for(int i = 0; i < entry.length; i++) {
					for(int j = 0; j < entry[i].length; j++) {
						if(entry[i][j] != null) {
							entry[i][j].clear();
						}
					}
				}
				
			}
		}
		
		this.particleEmitters.clear();
	}
}