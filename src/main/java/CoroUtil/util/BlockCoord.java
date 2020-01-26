package CoroUtil.util;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

public class BlockCoord {
	
	public int posX;
	public int posY;
	public int posZ;
	
	public Block block;
	public int meta;

	public BlockCoord(int p_i1354_1_, int p_i1354_2_, int p_i1354_3_)
    {
        this.posX = p_i1354_1_;
        this.posY = p_i1354_2_;
        this.posZ = p_i1354_3_;
    }

    public BlockCoord(BlockCoord p_i1355_1_)
    {
        this.posX = p_i1355_1_.posX;
        this.posY = p_i1355_1_.posY;
        this.posZ = p_i1355_1_.posZ;
    }
    
    public BlockCoord(int posX, int posY, int posZ, Block block, int meta) {
    	this(posX, posY, posZ);
    	this.block = block;
    	this.meta = meta;
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
    
    public int getX() {
    	return posX;
    }
    
    public int getY() {
    	return posY;
    }
    
    public int getZ() {
    	return posZ;
    }
    
    public double getDistanceSquared(double toX, double toY, double toZ) {
    	return distanceSq(toX, toY, toZ);
    }
    
    public double distanceSq(double toX, double toY, double toZ)
    {
        double d0 = (double)this.getX() - toX;
        double d1 = (double)this.getY() - toY;
        double d2 = (double)this.getZ() - toZ;
        return d0 * d0 + d1 * d1 + d2 * d2;
    }
    
    public BlockPos toBlockPos() {
    	return new BlockPos(this.posX, this.posY, this.posZ);
    }
}
