package weather2.config;

import java.io.File;

import weather2.Weather;


public class ConfigStorm {



    public static int Storm_OddsTo1OfHighWindWaterSpout = 150;
	public static boolean Storm_FlyingBlocksHurt = true;
	public static int Storm_MaxPerPlayerPerLayer = 20;
	public static int Storm_Deadly_CollideDistance = 128;
	public static int Storm_LightningStrikeBaseValueOddsTo1 = 200;
	public static boolean Storm_NoRainVisual = false;
	public static int Storm_MaxRadius = 300;
	public static int Storm_AllTypes_TickRateDelay = 60;
	public static int Storm_Rain_WaterBuildUpRate = 10;
	public static int Storm_Rain_WaterSpendRate = 3;
	public static int Storm_Rain_WaterBuildUpOddsTo1FromSource = 15;
	public static int Storm_Rain_WaterBuildUpOddsTo1FromNothing = 100;
	public static int Storm_Rain_WaterBuildUpOddsTo1FromOvercastRaining = 30;
	//public static int Storm_Rain_WaterBuildUp = 150;
	public static double Storm_TemperatureAdjustRate = 0.1D;
	//public static double Storm_Deadly_MinIntensity = 5.3D;
	public static int Storm_HailPerTick = 10;
	public static int Storm_OddsTo1OfOceanBasedStorm = 300;
	public static int Storm_OddsTo1OfLandBasedStorm = -1;
	//public static int Storm_OddsTo1OfProgressionBase = 15;
	//public static int Storm_OddsTo1OfProgressionStageMultiplier = 3;
	public static int Storm_PercentChanceOf_HighWind = 90;
	public static int Storm_PercentChanceOf_Hail = 80;
	public static int Storm_PercentChanceOf_F0_Tornado = 70;
	public static int Storm_PercentChanceOf_C0_Cyclone = 70;
	public static int Storm_PercentChanceOf_F1_Tornado = 50;
	public static int Storm_PercentChanceOf_C1_Cyclone = 50;
	public static int Storm_PercentChanceOf_F2_Tornado = 40;
	public static int Storm_PercentChanceOf_C2_Cyclone = 40;
	public static int Storm_PercentChanceOf_F3_Tornado = 30;
	public static int Storm_PercentChanceOf_C3_Cyclone = 30;
	public static int Storm_PercentChanceOf_F4_Tornado = 20;
	public static int Storm_PercentChanceOf_C4_Cyclone = 20;
	public static int Storm_PercentChanceOf_F5_Tornado = 10;
	public static int Storm_PercentChanceOf_C5_Cyclone = 10;
	public static int Storm_ParticleSpawnDelay = 3;
	
	//per player storm settings
	public static int Player_Storm_Deadly_OddsTo1 = 30;
	public static int Player_Storm_Deadly_TimeBetweenInTicks = 20*60*20*3; //3 mc days
	
	//per server storm settings
	public static boolean Server_Storm_Deadly_UseGlobalRate = false;
	public static int Server_Storm_Deadly_OddsTo1 = 30;
	public static int Server_Storm_Deadly_TimeBetweenInTicks = 20*60*20*3;
	public static boolean preventServerThunderstorms = true;
	//lightning
	public static int Lightning_OddsTo1OfFire = 20;
	public static int Lightning_lifetimeOfFire = 3;
	public static int Lightning_DistanceToPlayerForEffects = 256;

	public static boolean Lightning_StartsFires = false;

	public static int Storm_Deflector_RadiusOfStormRemoval = 150;

    public static int Storm_Deflector_MinStageRemove = 1;
    public static boolean Storm_Deflector_RemoveRainstorms = false;
    public static boolean Storm_Deflector_RemoveSandstorms = true;

    public static double Storm_Rain_Overcast_Amount = 0.01D;
	public static int Storm_Rain_Overcast_OddsTo1 = 50;

	public static int Storm_Rain_OddsTo1 = 150;

	public static int Storm_Rain_TrackAndExtinguishEntitiesRate = 200;

    public String getName() {
        return "Storm";
    }

    public String getRegistryName() {
        return Weather.modID + getName();
    }

    public String getConfigFileName() {
        return "Weather2" + File.separator + getName();
    }

    public String getCategory() {
        return "Weather2: " + getName();
    }

    public void hookUpdatedValues() {

    }
}
