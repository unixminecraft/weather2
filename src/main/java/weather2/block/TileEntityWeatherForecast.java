package weather2.block;

import java.util.ArrayList;
import java.util.List;

import CoroUtil.util.Vec3;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import weather2.ClientTickHandler;
import weather2.config.ConfigMisc;
import weather2.weathersystem.storm.StormObject;
import weather2.weathersystem.storm.WeatherObject;

public class TileEntityWeatherForecast extends TileEntity implements ITickable
{
	
	//since client receives data every couple seconds, we need to smooth out everything for best visual
	
	public StormObject lastTickStormObject = null;
	
	public List<WeatherObject> storms = new ArrayList<>();
	
	@Override
    public void update()
    {
    	if (world.isRemote) {
    		if (world.getTotalWorldTime() % 200 == 0 || storms.size() == 0) {

    			//catch race condition triggered by very slow computers
				ClientTickHandler.checkClientWeather();
				if (ClientTickHandler.weatherManager == null) return;

    			lastTickStormObject = ClientTickHandler.weatherManager.getClosestStorm(new Vec3(getPos().getX(), StormObject.layers.get(0), getPos().getZ()), 1024, StormObject.STATE_FORMING, true);

    			if (ConfigMisc.radarCloudDebug) {
					List<WeatherObject> listAdd = new ArrayList<>();
    				for (WeatherObject wo : ClientTickHandler.weatherManager.getStormObjects()) {
							listAdd.add(wo);
					}
					storms = listAdd;
				} else {
					storms = ClientTickHandler.weatherManager.getStormsAround(new Vec3(getPos().getX(), StormObject.layers.get(0), getPos().getZ()), 1024);
				}
    		}
    	}
    }
}
