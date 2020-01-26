package weather2.client.foliage;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.lwjgl.BufferUtils;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;

import CoroUtil.config.ConfigCoroUtil;
import CoroUtil.forge.CULog;
import CoroUtil.util.CoroUtilBlockLightCache;
import CoroUtil.util.Vec3;
import extendedrenderer.EventHandler;
import extendedrenderer.ExtendedRenderer;
import extendedrenderer.foliage.Foliage;
import extendedrenderer.foliage.FoliageData;
import extendedrenderer.render.FoliageRenderer;
import extendedrenderer.render.RotatingParticleManager;
import extendedrenderer.shader.InstancedMeshFoliage;
import extendedrenderer.shader.MeshBufferManagerFoliage;
import net.minecraft.block.BlockBeetroot;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import weather2.Weather;
import weather2.config.ConfigFoliage;
import weather2.config.ConfigMisc;
import weather2.util.WeatherUtilConfig;

@SuppressWarnings("deprecation")
public class FoliageEnhancerShader implements Runnable {

    public static boolean useThread = true;

    private static List<FoliageReplacerBase> listFoliageReplacers = new ArrayList<>();

    //for position tracking mainly, to be used for all foliage types maybe?
    private static ConcurrentHashMap<BlockPos, FoliageLocationData> lookupPosToFoliage = new ConcurrentHashMap<>();

    @SuppressWarnings("rawtypes")
	private static final Class multipartModelClass;
    @SuppressWarnings("rawtypes")
	private static final Class vanillaModelWrapperClass;
    private static final Field multipartPartModels;
    private static final Field modelWrapperModel;
    static {
        try {
            multipartModelClass = Class.forName("net.minecraftforge.client.model.ModelLoader$MultipartModel");
            multipartPartModels = multipartModelClass.getDeclaredField("partModels");
            multipartPartModels.setAccessible(true);
            vanillaModelWrapperClass = Class.forName("net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper");
            modelWrapperModel = vanillaModelWrapperClass.getDeclaredField("model");
            modelWrapperModel.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchFieldException | SecurityException e) {
            throw Throwables.propagate(e);
        }
    }

    private static ModelLoader modelLoader;
    private static IRegistry<ModelResourceLocation, IBakedModel> modelRegistry;
    private static HashMap<ModelResourceLocation, IBakedModel> lookupBackupReplacedModels = new HashMap<>();

    public static void modelBakeEvent(ModelBakeEvent event) {
        modelLoader = event.getModelLoader();
        modelRegistry = event.getModelRegistry();

        processModels();
    }

    private static void processModels() {

        if (modelLoader == null || modelRegistry == null) {
            CULog.err("modelLoader or modelRegistry null, aborting");
        }
        /**
         * ways to avoid a full resource pack reload and just invoke chunk render changes
         * - tterrag: you might also be able to do some delegating nonsense to avoid it entirely
         * - tterrag: check the config at runtime, if false just return vanilla quads
         *
         * - then finally do:
         * -- this.mc.renderGlobal.loadRenderers();
         */

        boolean replaceVanillaModels = ConfigCoroUtil.foliageShaders && EventHandler.queryUseOfShaders() && !ConfigMisc.Client_PotatoPC_Mode;

        FoliageData.backupBakedModelStore.clear();

        if (replaceVanillaModels) {

            lookupBackupReplacedModels.clear();

            String str = "Weather2: Replacing shaderized models";

            CULog.log(str);
            ProgressManager.ProgressBar prog = ProgressManager.push(str, modelRegistry.getKeys().size(), true);

            Map<ModelResourceLocation, IModel> stateModels = ReflectionHelper.getPrivateValue(ModelLoader.class, modelLoader, "stateModels");
            IBakedModel blank = modelRegistry.getObject(new ModelResourceLocation("coroutil:blank", "normal"));

            //shortcut to getting the data loaded into bakedModelStore, is empty on first minecraft run otherwise
            //would this cause bugs for mods that use ModelBakeEvent? meaning we might miss their models if we shaderize them
            modelLoader.blockModelShapes.reloadModels();

            CULog.dbg("bakedModelStore size: " + modelLoader.blockModelShapes.bakedModelStore.size());

            //make backup
            for (Map.Entry<IBlockState, IBakedModel> entry : modelLoader.blockModelShapes.bakedModelStore.entrySet()) {
                IBlockState state = entry.getKey();
                if (state instanceof IExtendedBlockState) {
                    state = ((IExtendedBlockState) state).getClean();
                }
                FoliageData.backupBakedModelStore.put(state, entry.getValue());
            }

            for (ModelResourceLocation res : modelRegistry.getKeys()) {
                prog.step(res.toString());
                IModel model = stateModels.get(res);
                if (model != null) {
                    //just in case of any cross mod weirdness
                    try {
                        Set<ResourceLocation> textures = Sets.newHashSet(model.getTextures());

                        /**
                         * TODO: special cases: flower pots with the specific plant variants, needs partial shader thing...
                         */

                        escape:
                        if (!res.getVariant().equals("inventory")) {
                            for (FoliageReplacerBase replacer : listFoliageReplacers) {
                                for (TextureAtlasSprite sprite : replacer.sprites) {
                                    for (ResourceLocation res2 : textures) {
                                        if (res2.toString().equals(sprite.getIconName())) {
                                            if (!res.toString().contains("flower_pot")) {
                                                lookupBackupReplacedModels.put(res, modelRegistry.getObject(res));
                                                modelRegistry.putObject(res, blank);
                                                break escape;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            ProgressManager.pop(prog);
        }
    }

    /**
     * Called from shaders listener
     */
    public static void shadersInit() {

        //temp? for sake of hot reloading replacers
        FoliageEnhancerShader.setupReplacers();

        CULog.log("Weather2: Setting up meshes for foliage shader");

        for (FoliageReplacerBase replacer : listFoliageReplacers) {
            for (TextureAtlasSprite sprite : replacer.sprites) {
                MeshBufferManagerFoliage.setupMeshIfMissing(sprite);
            }
        }
    }
    /**
     * Called from TextureStitchEvent.Post
     */
    public static void setupReplacers() {

        CULog.log("Weather2: Setting up foliage replacers");

        listFoliageReplacers.clear();

        HashMap<Comparable<BlockPlanks.EnumType>, String> lookup = new HashMap<>();

        lookup.clear();
        lookup.put(BlockPlanks.EnumType.OAK, "minecraft:blocks/sapling_oak");
        lookup.put(BlockPlanks.EnumType.SPRUCE, "minecraft:blocks/sapling_spruce");
        lookup.put(BlockPlanks.EnumType.BIRCH, "minecraft:blocks/sapling_birch");
        lookup.put(BlockPlanks.EnumType.JUNGLE, "minecraft:blocks/sapling_jungle");
        lookup.put(BlockPlanks.EnumType.ACACIA, "minecraft:blocks/sapling_acacia");
        lookup.put(BlockPlanks.EnumType.DARK_OAK, "minecraft:blocks/sapling_roofed_oak");

        for (Map.Entry<Comparable<BlockPlanks.EnumType>, String> entrySet : lookup.entrySet()) {
            listFoliageReplacers.add(new FoliageReplacerCross(Blocks.SAPLING.getDefaultState())
                    .setSprite(getMeshAndSetupSprite(entrySet.getValue()))
                    .setStateSensitive(true)
                    .setBiomeColorize(false)
                    .addComparable(BlockSapling.TYPE, entrySet.getKey()));
        }

        HashMap<Comparable<BlockTallGrass.EnumType>, String> lookup2 = new HashMap<>();
        lookup2.put(BlockTallGrass.EnumType.DEAD_BUSH, "minecraft:blocks/deadbush");
        lookup2.put(BlockTallGrass.EnumType.GRASS, "minecraft:blocks/tallgrass");
        lookup2.put(BlockTallGrass.EnumType.FERN, "minecraft:blocks/fern");

        for (Map.Entry<Comparable<BlockTallGrass.EnumType>, String> entrySet : lookup2.entrySet()) {
            boolean colorize = entrySet.getKey() == BlockTallGrass.EnumType.DEAD_BUSH ? false : true;
            listFoliageReplacers.add(new FoliageReplacerCross(Blocks.TALLGRASS.getDefaultState())
                    .setSprite(getMeshAndSetupSprite(entrySet.getValue()))
                    .setStateSensitive(true)
                    .setRandomizeCoord(false)
                    .setBiomeColorize(colorize)
                    .addComparable(BlockTallGrass.TYPE, entrySet.getKey()));
        }

        listFoliageReplacers.add(new FoliageReplacerCross(Blocks.YELLOW_FLOWER.getDefaultState())
                .setSprite(getMeshAndSetupSprite("minecraft:blocks/flower_dandelion"))
                .setRandomizeCoord(false)
                .setBiomeColorize(false));

        HashMap<Comparable<BlockFlower.EnumFlowerType>, String> lookup3 = new HashMap<>();
        lookup3.put(BlockFlower.EnumFlowerType.ALLIUM, "minecraft:blocks/flower_allium");
        lookup3.put(BlockFlower.EnumFlowerType.BLUE_ORCHID, "minecraft:blocks/flower_blue_orchid");
        lookup3.put(BlockFlower.EnumFlowerType.HOUSTONIA, "minecraft:blocks/flower_houstonia");
        lookup3.put(BlockFlower.EnumFlowerType.ORANGE_TULIP, "minecraft:blocks/flower_tulip_orange");
        lookup3.put(BlockFlower.EnumFlowerType.OXEYE_DAISY, "minecraft:blocks/flower_oxeye_daisy");
        lookup3.put(BlockFlower.EnumFlowerType.PINK_TULIP, "minecraft:blocks/flower_tulip_pink");
        lookup3.put(BlockFlower.EnumFlowerType.POPPY, "minecraft:blocks/flower_rose");
        lookup3.put(BlockFlower.EnumFlowerType.RED_TULIP, "minecraft:blocks/flower_tulip_red");
        lookup3.put(BlockFlower.EnumFlowerType.WHITE_TULIP, "minecraft:blocks/flower_tulip_white");

        for (Map.Entry<Comparable<BlockFlower.EnumFlowerType>, String> entrySet : lookup3.entrySet()) {
            listFoliageReplacers.add(new FoliageReplacerCross(Blocks.RED_FLOWER.getDefaultState())
                    .setSprite(getMeshAndSetupSprite(entrySet.getValue()))
                    .setRandomizeCoord(false)
                    .setStateSensitive(true)
                    .setBiomeColorize(false)
                    .addComparable(Blocks.RED_FLOWER.getTypeProperty(), entrySet.getKey()));
        }
        for (int i = 0; i < 8; i++) {
            int temp = i;
            listFoliageReplacers.add(new FoliageReplacerCross(Blocks.WHEAT.getDefaultState())
                    .setBaseMaterial(Material.GROUND)
                    .setSprite(getMeshAndSetupSprite("minecraft:blocks/wheat_stage_" + temp))
                    .setRandomizeCoord(false)
                    .setStateSensitive(true)
                    .addComparable(BlockCrops.AGE, i));
        }

        listFoliageReplacers.add(new FoliageReplacerCross(Blocks.REEDS.getDefaultState(), -1)
                .setSprite(getMeshAndSetupSprite("minecraft:blocks/reeds"))
                .setBaseMaterial(Material.SAND).setBiomeColorize(true).setRandomizeCoord(false).setLooseness(0.3F));

        //ugh
        HashMap<Integer, Integer> lookupStateToModel = new HashMap<>();
        lookupStateToModel.put(0, 0);
        lookupStateToModel.put(1, 0);
        lookupStateToModel.put(2, 1);
        lookupStateToModel.put(3, 1);
        lookupStateToModel.put(4, 2);
        lookupStateToModel.put(5, 2);
        lookupStateToModel.put(6, 2);
        lookupStateToModel.put(7, 3);

        for (Map.Entry<Integer, Integer> entrySet : lookupStateToModel.entrySet()) {
            listFoliageReplacers.add(new FoliageReplacerCross(Blocks.CARROTS.getDefaultState())
                    .setBaseMaterial(Material.GROUND)
                    .setSprite(getMeshAndSetupSprite("minecraft:blocks/carrots_stage_" + entrySet.getValue()))
                    .setRandomizeCoord(false)
                    .setStateSensitive(true)
                    .addComparable(BlockCrops.AGE, entrySet.getKey()));
        }

        for (Map.Entry<Integer, Integer> entrySet : lookupStateToModel.entrySet()) {
            listFoliageReplacers.add(new FoliageReplacerCross(Blocks.POTATOES.getDefaultState())
                    .setBaseMaterial(Material.GROUND)
                    .setSprite(getMeshAndSetupSprite("minecraft:blocks/potatoes_stage_" + entrySet.getValue()))
                    .setRandomizeCoord(false)
                    .setStateSensitive(true)
                    .addComparable(BlockCrops.AGE, entrySet.getKey()));
        }

        for (int i = 0; i < 4; i++) {
            listFoliageReplacers.add(new FoliageReplacerCross(Blocks.BEETROOTS.getDefaultState())
                    .setBaseMaterial(Material.GROUND)
                    .setSprite(getMeshAndSetupSprite("minecraft:blocks/beetroots_stage_" + i))
                    .setRandomizeCoord(false)
                    .setStateSensitive(true)
                    .addComparable(BlockBeetroot.BEETROOT_AGE, i));
        }

        List<TextureAtlasSprite> sprites = new ArrayList<>();
        sprites.add(getMeshAndSetupSprite("minecraft:blocks/double_plant_grass_bottom"));
        sprites.add(getMeshAndSetupSprite("minecraft:blocks/double_plant_grass_top"));
        listFoliageReplacers.add(new FoliageReplacerCross(Blocks.DOUBLE_PLANT.getDefaultState(),2).setSprites(sprites)
                .setStateSensitive(true)
                .addComparable(BlockDoublePlant.VARIANT, BlockDoublePlant.EnumPlantType.GRASS));

        sprites = new ArrayList<>();
        sprites.add(getMeshAndSetupSprite("minecraft:blocks/double_plant_rose_bottom"));
        sprites.add(getMeshAndSetupSprite("minecraft:blocks/double_plant_rose_top"));
        listFoliageReplacers.add(new FoliageReplacerCross(Blocks.DOUBLE_PLANT.getDefaultState(),2).setSprites(sprites)
                .setBiomeColorize(false)
                .setStateSensitive(true)
                .addComparable(BlockDoublePlant.VARIANT, BlockDoublePlant.EnumPlantType.ROSE));

        sprites = new ArrayList<>();
        sprites.add(getMeshAndSetupSprite("minecraft:blocks/double_plant_fern_bottom"));
        sprites.add(getMeshAndSetupSprite("minecraft:blocks/double_plant_fern_top"));
        listFoliageReplacers.add(new FoliageReplacerCross(Blocks.DOUBLE_PLANT.getDefaultState(),2).setSprites(sprites)
                .setBiomeColorize(true)
                .setStateSensitive(true)
                .addComparable(BlockDoublePlant.VARIANT, BlockDoublePlant.EnumPlantType.FERN));

        sprites = new ArrayList<>();
        sprites.add(getMeshAndSetupSprite("minecraft:blocks/double_plant_paeonia_bottom"));
        sprites.add(getMeshAndSetupSprite("minecraft:blocks/double_plant_paeonia_top"));
        listFoliageReplacers.add(new FoliageReplacerCross(Blocks.DOUBLE_PLANT.getDefaultState(),2).setSprites(sprites)
                .setBiomeColorize(false)
                .setStateSensitive(true)
                .addComparable(BlockDoublePlant.VARIANT, BlockDoublePlant.EnumPlantType.PAEONIA));

        sprites = new ArrayList<>();
        sprites.add(getMeshAndSetupSprite("minecraft:blocks/double_plant_syringa_bottom"));
        sprites.add(getMeshAndSetupSprite("minecraft:blocks/double_plant_syringa_top"));
        listFoliageReplacers.add(new FoliageReplacerCross(Blocks.DOUBLE_PLANT.getDefaultState(),2).setSprites(sprites)
                .setBiomeColorize(false)
                .setStateSensitive(true)
                .addComparable(BlockDoublePlant.VARIANT, BlockDoublePlant.EnumPlantType.SYRINGA));


        if (ConfigFoliage.extraGrass) {
            listFoliageReplacers.add(new FoliageReplacerCrossGrass(Blocks.AIR.getDefaultState()) {
                @Override
                public boolean isActive() {
                    return ConfigFoliage.extraGrass;
                }
            }
                    .setSprite(getMeshAndSetupSprite(ExtendedRenderer.modid + ":particles/grass"))
                    .setRandomizeCoord(true)
                    .setBiomeColorize(true));
        }
    }

    /**
     * Called from shaders listener
     */
    public static void shadersReset() {
        lookupPosToFoliage.clear();
    }

    private static TextureAtlasSprite getMeshAndSetupSprite(String spriteLoc) {
        TextureMap map = Minecraft.getMinecraft().getTextureMapBlocks();
        TextureAtlasSprite sprite = map.getAtlasSprite(spriteLoc);
        return sprite;
    }

    @Override
    public void run() {
        if (useThread) {
            while (true) {
                try {
                    if (ConfigCoroUtil.foliageShaders && RotatingParticleManager.useShaders && !ConfigMisc.Client_PotatoPC_Mode) {
                        boolean gotLock = tickClientThreaded();
                        if (gotLock) {
                            Thread.sleep(ConfigFoliage.Thread_Foliage_Process_Delay);
                        } else {
                            Thread.sleep(20);
                        }
                    } else {
                        Thread.sleep(5000);
                    }
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }
    }

    /**
     * TODO: For faster updating close to player, however it still gets locked out by thread, that needs solving
     *
     * @return
     */
    public static boolean tickClientCloseToPlayer() {
        Minecraft mc = FMLClientHandler.instance().getClient();

        if (mc.world != null && mc.player != null && WeatherUtilConfig.listDimensionsWindEffects.contains(mc.world.provider.getDimension())) {
            return tickFoliage(5, false);
        } else {
            return true;
        }
    }

    //run from our newly created thread
    public static boolean tickClientThreaded() {
        Minecraft mc = FMLClientHandler.instance().getClient();

        if (mc.world != null && mc.player != null && WeatherUtilConfig.listDimensionsWindEffects.contains(mc.world.provider.getDimension())) {
            return tickFoliage(ConfigFoliage.foliageShaderRange, true);
        } else {
            return true;
        }
    }

    private static boolean tickFoliage(int radialRange, boolean trimRange) {
        if (ExtendedRenderer.foliageRenderer.lockVBO2.tryLock()) {
            try {
            	boolean result = profileForFoliageShader(radialRange, trimRange);
            	ExtendedRenderer.foliageRenderer.lockVBO2.unlock();
                return result;
            }
            catch(Throwable t) {
                
                return true;
            }
        } else {
            return false;
        }
    }

    private static boolean profileForFoliageShader(int radialRange, boolean trimRange) {

        /**
         *
         * TODO: if thread couldnt get lock, dont wait the full Thread_Particle_Process_Delay for the next tick
         *
         * double_plant - double height sway
         * tallgrass - sway
         * flowers - sway
         * crops - sway
         * - all wheat stages
         * -- how to keep them rendering far? eg https://i.imgur.com/WltFr7x.png
         * vines - variable negative height sway
         * - not cross stitched, specific angles
         * -- shader should be able to handle it fine as is
         *
         * extra ideas:
         * regular grass with my own texture
         * tree leafs with cross stitch render
         *
         * mod support:
         * - plant material blocks
         * - find and override in our resource pack
         *
         */

        World world = Minecraft.getMinecraft().world;
        Entity entityIn = Minecraft.getMinecraft().player;
        BlockPos pos = entityIn.getPosition();

        int xzRange = radialRange;
        int yRange = radialRange;
        //prevent circular distance check position from changing as thread runs
        double centerX = entityIn.posX;
        double centerY = entityIn.posY;
        double centerZ = entityIn.posZ;

        for (TextureAtlasSprite sprite : ExtendedRenderer.foliageRenderer.foliage.keySet()) {
            InstancedMeshFoliage mesh = MeshBufferManagerFoliage.getMesh(sprite);
            mesh.lastAdditionCount = 0;
            mesh.lastRemovalCount = 0;
        }

        //cleanup list
        if (true) {
            Iterator<Map.Entry<BlockPos, FoliageLocationData>> it = lookupPosToFoliage.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<BlockPos, FoliageLocationData> entry = it.next();
                if (!entry.getValue().foliageReplacer.isActive() || !entry.getValue().foliageReplacer.validFoliageSpot(world, entry.getKey().down())) {
                    it.remove();
                    for (Foliage entry2 : entry.getValue().listFoliage) {
                        entry.getValue().foliageReplacer.markMeshesDirty();
                        ExtendedRenderer.foliageRenderer.getFoliageForSprite(entry2.particleTexture).remove(entry2);
                        MeshBufferManagerFoliage.getMesh(entry2.particleTexture).lastRemovalCount++;
                    }
                } else if (trimRange && entry.getKey().distanceSq(centerX, centerY, centerZ)/*entityIn.getDistanceSq(entry.getKey())*/ > radialRange * radialRange) {
                    it.remove();
                    for (Foliage entry2 : entry.getValue().listFoliage) {
                        entry.getValue().foliageReplacer.markMeshesDirty();
                        ExtendedRenderer.foliageRenderer.getFoliageForSprite(entry2.particleTexture).remove(entry2);
                        MeshBufferManagerFoliage.getMesh(entry2.particleTexture).lastRemovalCount++;
                    }
                }
            }
        }

            for (int x = -xzRange; x <= xzRange; x++) {
                for (int z = -xzRange; z <= xzRange; z++) {
                    for (int y = -yRange; y <= yRange; y++) {
                        BlockPos posScan = pos.add(x, y, z);
                        if (!lookupPosToFoliage.containsKey(posScan)) {
                            if (posScan.distanceSq(centerX, centerY, centerZ) <= radialRange * radialRange) {

                                for (FoliageReplacerBase replacer : listFoliageReplacers) {

								    if (replacer.isActive() && replacer.validFoliageSpot(entityIn.world, posScan.down())) {
								        //System.out.println("add");
								        replacer.addForPos(entityIn.world, posScan);
								        replacer.markMeshesDirty();

								        for (TextureAtlasSprite sprite : replacer.sprites) {
								            MeshBufferManagerFoliage.getMesh(sprite).lastAdditionCount++;
								        }

								    }
								}
                            }

                        }
                    }
                }
            }

            try {
                for (Map.Entry<TextureAtlasSprite, List<Foliage>> entry : ExtendedRenderer.foliageRenderer.foliage.entrySet()) {
                    InstancedMeshFoliage mesh = MeshBufferManagerFoliage.getMesh(entry.getKey());

                    if (mesh.dirtyVBO2Flag) {
                        mesh.interpPosXThread = entityIn.posX;
                        mesh.interpPosYThread = entityIn.posY;
                        mesh.interpPosZThread = entityIn.posZ;

                        updateVBO2Threaded(entry.getKey());
                    }
                }
            }
            catch(Throwable t) {
            	return true;
            }
            return true;
    }

    public static void markMeshDirty(TextureAtlasSprite sprite, boolean flag) {
        InstancedMeshFoliage mesh = MeshBufferManagerFoliage.getMesh(sprite);

        if (mesh != null) {
            mesh.dirtyVBO2Flag = flag;
        } else {
            Weather.dbg("MESH NULL HERE, FIX INIT ORDER");
        }
    }

    private static void updateVBO2Threaded(TextureAtlasSprite sprite) {

        Minecraft mc = Minecraft.getMinecraft();
        Entity entityIn = mc.getRenderViewEntity();

        float partialTicks = 1F;

        InstancedMeshFoliage mesh = MeshBufferManagerFoliage.getMesh(sprite);
        if (mesh == null) {
            return;
        }

        int lastPos = mesh.curBufferPosVBO2;

        mesh.curBufferPosVBO2 = 0;
        mesh.instanceDataBufferVBO2.clear();

        int guessAtExtraMeshesPerFoliage = 4;
        int extraMeshes = (mesh.lastAdditionCount * guessAtExtraMeshesPerFoliage) - (mesh.lastRemovalCount * guessAtExtraMeshesPerFoliage);
        if (lastPos + extraMeshes > mesh.numInstances) {
            if (mesh.numInstances * 4 < lastPos + extraMeshes) {
                mesh.numInstances = (int)(Math.ceil((float)(lastPos + extraMeshes) / 10000F) * 10000F);
            } else {
                mesh.numInstances *= 4;
            }
            mesh.instanceDataBufferVBO2 = BufferUtils.createFloatBuffer(mesh.numInstances * InstancedMeshFoliage.INSTANCE_SIZE_FLOATS_SELDOM);
            mesh.instanceDataBufferVBO2.clear();

            mesh.instanceDataBufferVBO1 = BufferUtils.createFloatBuffer(mesh.numInstances * InstancedMeshFoliage.INSTANCE_SIZE_FLOATS);
        }
        for (Foliage foliage : ExtendedRenderer.foliageRenderer.getFoliageForSprite(sprite)) {
            foliage.updateQuaternion(entityIn);
            foliage.renderForShaderVBO2(mesh, ExtendedRenderer.foliageRenderer.transformation, null, entityIn, partialTicks);
        }
        if (FoliageRenderer.testStaticLimit) {
            mesh.instanceDataBufferVBO2.limit(30000 * InstancedMeshFoliage.INSTANCE_SIZE_FLOATS_SELDOM);
        } else {
            mesh.instanceDataBufferVBO2.limit(mesh.curBufferPosVBO2 * InstancedMeshFoliage.INSTANCE_SIZE_FLOATS_SELDOM);
        }
    }

    public static void addForPos(FoliageReplacerBase replacer, int height, BlockPos pos, Vec3 randPosVar, boolean biomeColorize) {
        addForPos(replacer, height, pos, randPosVar, biomeColorize, 0);
    }

    private static void addForPos(FoliageReplacerBase replacer, int height, BlockPos pos, Vec3 randPosVar, boolean biomeColorize, int colorizeOffset) {
        addForPos(replacer, height, pos, randPosVar, biomeColorize, colorizeOffset, null);
    }

    public static void addForPos(FoliageReplacerBase replacer, int height, BlockPos pos, Vec3 randPosVar, boolean biomeColorize, int colorizeOffset, Vec3 extraPos) {

        World world = Minecraft.getMinecraft().world;

        Random rand = new Random();
        FoliageLocationData data = new FoliageLocationData(replacer);
        int heightIndex;
        float randX = 0;
        float randZ = 0;
        if (randPosVar != null) {
            randX = (rand.nextFloat() - rand.nextFloat()) * (float) randPosVar.xCoord;
            randZ = (rand.nextFloat() - rand.nextFloat()) * (float) randPosVar.zCoord;
        }

        int clutterSize = 2;
        int meshesPerLayer = 2;

        if (replacer instanceof FoliageReplacerCross) {
            clutterSize = 2 * height;
        }

        if (replacer instanceof FoliageReplacerCrossGrass) {
            clutterSize = 4;
        }

        for (int i = 0; i < clutterSize; i++) {
            heightIndex = i / meshesPerLayer;

            if (replacer instanceof FoliageReplacerCrossGrass) {
                heightIndex = 0;
            }

            TextureAtlasSprite sprite = replacer.sprites.get(0);
            if (replacer instanceof FoliageReplacerCross) {
                if (heightIndex < replacer.sprites.size()) {
                    sprite = replacer.sprites.get(heightIndex);
                }
            }

            Foliage foliage = new Foliage(sprite);
            foliage.setPosition(pos.add(0, 0, 0));
            foliage.prevPosY = foliage.posY;
            foliage.heightIndex = heightIndex;
            Vec3d vec = world.getBlockState(pos).getOffset(world, pos);
            foliage.posX += 0.5F + randX + vec.x;
            foliage.prevPosX = foliage.posX;
            foliage.posZ += 0.5F + randZ + vec.z;
            if (extraPos != null) {
                foliage.posX += extraPos.xCoord;
                foliage.posZ += extraPos.zCoord;
            }
            foliage.prevPosZ = foliage.posZ;
            foliage.rotationYaw = 0;
            foliage.rotationYaw = world.rand.nextInt(360);
            foliage.rotationYaw = 45;
            if ((i+1) % 2 == 0) {
                foliage.rotationYaw += 90;
            }


            if (replacer instanceof FoliageReplacerCrossGrass) {
                foliage.rotationYaw = 45;
                double dist = 0.17;
                if (i == 0) {
                    foliage.rotationYaw += 90;
                    foliage.posX += dist;
                    foliage.posZ += dist;
                } else if (i == 1) {
                    foliage.rotationYaw += 90;
                    foliage.posX -= dist;
                    foliage.posZ -= dist;
                } else if (i == 2) {
                    foliage.posX += dist;
                    foliage.posZ -= dist;
                } else if (i == 3) {
                    foliage.posX -= dist;
                    foliage.posZ += dist;
                }
            }

            foliage.looseness = replacer.looseness;
            foliage.particleScale /= 0.2;

            if (biomeColorize) {
                int color = Minecraft.getMinecraft().getBlockColors().colorMultiplier(world.getBlockState(pos.up(colorizeOffset)), world, pos.up(colorizeOffset)/*.down()*/, 0);
                foliage.particleRed = (float) (color >> 16 & 255) / 255.0F;
                foliage.particleGreen = (float) (color >> 8 & 255) / 255.0F;
                foliage.particleBlue = (float) (color & 255) / 255.0F;

                if (replacer instanceof FoliageReplacerCrossGrass) {
                    color = Minecraft.getMinecraft().getBlockColors().colorMultiplier(Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS), world, pos.up(colorizeOffset)/*.down()*/, 0);
                    foliage.particleRed = (float) (color >> 16 & 255) / 255.0F;
                    foliage.particleGreen = (float) (color >> 8 & 255) / 255.0F;
                    foliage.particleBlue = (float) (color & 255) / 255.0F;
                }
            }
            foliage.brightnessCache = CoroUtilBlockLightCache.brightnessPlayer;

            data.listFoliage.add(foliage);
            ExtendedRenderer.foliageRenderer.getFoliageForSprite(sprite).add(foliage);

        }

        lookupPosToFoliage.put(pos, data);

    }
}
