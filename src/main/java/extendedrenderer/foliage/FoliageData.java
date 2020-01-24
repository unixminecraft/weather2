package extendedrenderer.foliage;

import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;

public class FoliageData {

    //orig values
    public static ConcurrentHashMap<IBlockState, IBakedModel> backupBakedModelStore = new ConcurrentHashMap<>();

}
