package weather2.block;

import CoroUtil.util.Vec3;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import weather2.util.WeatherUtilEntity;
import weather2.util.WindReader;

public class TileEntityWindVane extends TileEntity implements ITickable
{
	
	//since client receives data every couple seconds, we need to smooth out everything for best visual
	
	public float smoothAngle = 0;
	private float smoothAngleRotationalVelAccel = 0;
	
	private float smoothAngleAdj = 0.1F;
	private boolean isOutsideCached = false;

	@Override
    public void update()
    {
    	if (world.isRemote) {
    		
    		if (world.getTotalWorldTime() % 40 == 0) {
    			isOutsideCached = WeatherUtilEntity.isPosOutside(world, new Vec3(getPos().getX()+0.5F, getPos().getY()+0.5F, getPos().getZ()+0.5F));
    		}
    		
    		if (isOutsideCached) {
	    		
	    		float targetAngle = WindReader.getWindAngle(world, new Vec3(getPos().getX(), getPos().getY(), getPos().getZ()));
	    		float windSpeed = WindReader.getWindSpeed(world, new Vec3(getPos().getX(), getPos().getY(), getPos().getZ()));
	    		
	    		if (smoothAngle > 180) smoothAngle-=360;
	    		if (smoothAngle < -180) smoothAngle+=360;
	    		
	    		float bestMove = MathHelper.wrapDegrees(targetAngle - smoothAngle);
	    		
	    		smoothAngleAdj = windSpeed;
	    		
	    		if (Math.abs(bestMove) < 180) {
	    			float realAdj = smoothAngleAdj;
	    			
	    			if (realAdj * 2 > windSpeed) {
		    			if (bestMove > 0) smoothAngleRotationalVelAccel -= realAdj;
		    			if (bestMove < 0) smoothAngleRotationalVelAccel += realAdj;
	    			}
	    			
	    			if (smoothAngleRotationalVelAccel > 0.3 || smoothAngleRotationalVelAccel < -0.3) {
	    				smoothAngle += smoothAngleRotationalVelAccel;
	    			}
	    				    			
	    			smoothAngleRotationalVelAccel *= 0.80F;
	    		}
    		}
    	}
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
    	return new AxisAlignedBB(getPos().getX(), getPos().getY(), getPos().getZ(), getPos().getX() + 1, getPos().getY() + 3, getPos().getZ() + 1);
    }
}
