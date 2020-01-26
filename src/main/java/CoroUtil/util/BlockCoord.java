package CoroUtil.util;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

public class BlockCoord {
	
	public int posX;
	public int posY;
	public int posZ;
	
	public Block block;
	
	public BlockCoord(int posX, int posY, int posZ, Block block, int meta) {
    	this.posX = posX;
    	this.posY = posY;
    	this.posZ = posZ;
    	this.block = block;
    }
    
    public boolean equals(Object p_equals_1_)
    {
        if (!(p_equals_1_ instanceof BlockCoord))
        {
            return false;
        }
        else
        {
        	BlockCoord BlockCoord = (BlockCoord)p_equals_1_;
            return this.posX == BlockCoord.posX && this.posY == BlockCoord.posY && this.posZ == BlockCoord.posZ;
        }
    }

    public int hashCode()
    {
        return this.posX + this.posZ << 8 + this.posY << 16;
    }
    
    public String toString()
    {
        return "Pos{x=" + this.posX + ", y=" + this.posY + ", z=" + this.posZ + '}';
    }
    
    public double getDistanceSquared(double toX, double toY, double toZ) {
        double d0 = (double) posX - toX;
        double d1 = (double) posY - toY;
        double d2 = (double) posZ - toZ;
        return d0 * d0 + d1 * d1 + d2 * d2;
    }
    
    public BlockPos toBlockPos() {
    	return new BlockPos(this.posX, this.posY, this.posZ);
    }
}
