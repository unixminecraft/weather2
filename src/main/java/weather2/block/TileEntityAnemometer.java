package weather2.block;

import CoroUtil.util.Vec3;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import weather2.util.WeatherUtilEntity;
import weather2.util.WindReader;

public class TileEntityAnemometer extends TileEntity implements ITickable
{
	
	//since client receives data every couple seconds, we need to smooth out everything for best visual
	
	public float smoothAngle = 0;
	public float smoothAnglePrev = 0;
	private float smoothAngleRotationalVel = 0;
	private boolean isOutsideCached = false;

	@Override
    public void update()
    {
    	if (world.isRemote) {
    		
    		if (world.getTotalWorldTime() % 40 == 0) {
    			isOutsideCached = WeatherUtilEntity.isPosOutside(world, new Vec3(getPos().getX()+0.5F, getPos().getY()+0.5F, getPos().getZ()+0.5F));
    		}
    		
    		if (isOutsideCached) {
	    		
	    		float windSpeed = WindReader.getWindSpeed(world, new Vec3(getPos().getX(), getPos().getY(), getPos().getZ()));
	    		
	    		smoothAngleRotationalVel += windSpeed * 1F;
	    		
	    		
	    		if (smoothAngleRotationalVel > 50F) smoothAngleRotationalVel = 50F;
	    		
	    		if (smoothAngle >= 180) smoothAngle -= 360;
	    		if (smoothAnglePrev >= 180) smoothAnglePrev -= 360;
    		}
    		
    		smoothAnglePrev = smoothAngle;
    		smoothAngle += smoothAngleRotationalVel;
    		smoothAngleRotationalVel -= 0.1F;
    		
    		smoothAngleRotationalVel *= 0.97F;
    		
    		if (smoothAngleRotationalVel <= 0) smoothAngleRotationalVel = 0;
    	}
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
    	return new AxisAlignedBB(getPos().getX(), getPos().getY(), getPos().getZ(), getPos().getX() + 1, getPos().getY() + 3, getPos().getZ() + 1);
    }
}
