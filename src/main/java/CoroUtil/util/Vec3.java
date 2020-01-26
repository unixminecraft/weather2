package CoroUtil.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Vec3
{
    /** X coordinate of Vec3D */
    public double xCoord;
    /** Y coordinate of Vec3D */
    public double yCoord;
    /** Z coordinate of Vec3D */
    public double zCoord;
    
    public Vec3(Vec3 pos) {
    	this(pos.xCoord, pos.yCoord, pos.zCoord);
    }
    
    public Vec3(Vec3d pos) {
    	this(pos.x, pos.y, pos.z);
    }
    
    public Vec3(BlockPos pos) {
    	this(pos.getX(), pos.getY(), pos.getZ());
    }

    public Vec3(double p_i1108_1_, double p_i1108_3_, double p_i1108_5_)
    {
        if (p_i1108_1_ == -0.0D)
        {
            p_i1108_1_ = 0.0D;
        }

        if (p_i1108_3_ == -0.0D)
        {
            p_i1108_3_ = 0.0D;
        }

        if (p_i1108_5_ == -0.0D)
        {
            p_i1108_5_ = 0.0D;
        }

        this.xCoord = p_i1108_1_;
        this.yCoord = p_i1108_3_;
        this.zCoord = p_i1108_5_;
    }

    /**
     * Adds the specified x,y,z vector components to this vector and returns the resulting vector. Does not change this
     * vector.
     */
    public Vec3 addVector(double p_72441_1_, double p_72441_3_, double p_72441_5_)
    {
        /**
         * Static method for creating a new Vec3D given the three x,y,z values. This is only called from the other
         * static method which creates and places it in the list.
         */
        return new Vec3(this.xCoord + p_72441_1_, this.yCoord + p_72441_3_, this.zCoord + p_72441_5_);
    }

    /**
     * Euclidean distance between this and the specified vector, returned as double.
     */
    public double distanceTo(Vec3 p_72438_1_)
    {
        double d0 = p_72438_1_.xCoord - this.xCoord;
        double d1 = p_72438_1_.yCoord - this.yCoord;
        double d2 = p_72438_1_.zCoord - this.zCoord;
        return (double)MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
    }

    public String toString()
    {
        return "(" + this.xCoord + ", " + this.yCoord + ", " + this.zCoord + ")";
    }
    
    public net.minecraft.util.math.Vec3d toMCVec() {
    	return new net.minecraft.util.math.Vec3d(this.xCoord, this.yCoord, this.zCoord);
    }
    
    public BlockPos toBlockPos() {
    	return new BlockPos(this.xCoord, this.yCoord, this.zCoord);
    }
}