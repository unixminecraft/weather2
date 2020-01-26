package weather2.client.foliage;

import java.util.Map;

import CoroUtil.util.Vec3;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class FoliageReplacerCrossGrass extends FoliageReplacerCross {

    public FoliageReplacerCrossGrass(IBlockState state) {
        super(state);
    }

    @Override
    public boolean validFoliageSpot(World world, BlockPos pos) {
        if (baseMaterial == null || world.getBlockState(pos).getMaterial() == baseMaterial) {
            if (stateSensitive) {
                IBlockState stateScan = world.getBlockState(pos.up());
                if (stateScan.getBlock() == state.getBlock()) {
                    boolean fail = false;
                    for (Map.Entry<IProperty<?>, Comparable<?>> entrySet : lookupPropertiesToComparable.entrySet()) {
                        if (stateScan.getValue(entrySet.getKey()) != entrySet.getValue()) {
                            fail = true;
                            break;
                        }
                    }
                    if (fail) {
                        return false;
                    }
                    return true;
                } else {
                    return false;
                }
            } else {
                return world.getBlockState(pos.up()).getBlock() == state.getBlock();
            }
        } else {
            return false;
        }

    }

    @Override
    public void addForPos(World world, BlockPos pos) {
        //TODO: handle multi height cross detection here or make child class based off this one to do it
        int height = expectedHeight;
        if (height == -1) {
            Block block = state.getBlock();
            height = 0;

            while (block == state.getBlock()) {
                height++;
                block = world.getBlockState(pos.up(height)).getBlock();
            }
        }
        Vec3 vec = new Vec3(0.2, 0, 0.2);
        FoliageEnhancerShader.addForPos(this, height, pos, vec, biomeColorize, -1, new Vec3(0, 0, 0));
    }
}
