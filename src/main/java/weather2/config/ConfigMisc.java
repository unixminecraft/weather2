package weather2.config;

public class ConfigMisc {
	
	// misc
	public static boolean Misc_proxyRenderOverrideEnabled = true;
	
	// cutoff a bit extra, noticed lots of storms being insta killed on creation
	public static int Misc_simBoxRadiusCutoff = 1024 + 100;
	public static int Misc_simBoxRadiusSpawn = 1024;
	public static boolean Misc_ForceVanillaCloudsOff = true;
	public static int Misc_AutoDataSaveIntervalInTicks = 20 * 60 * 30;
	public static boolean consoleDebug = false;
	
	public static boolean radarCloudDebug = false;
	
	// Weather
	public static boolean overcastMode = false;
	public static int lockServerWeatherMode = 0; // is only used if overcastMode is off
	// clouds
	public static int Cloud_ParticleSpawnDelay = 2;
	public static int Cloud_Formation_MinDistBetweenSpawned = 300;
	public static boolean Cloud_Layer1_Enable = false;
	public static int Cloud_Layer0_Height = 200;
	public static int Cloud_Layer1_Height = 350;
	public static int Cloud_Layer2_Height = 500;
	
	public static double Cloud_Coverage_Random_Change_Amount = 0.05D;
	
	public static double Cloud_Coverage_Min_Percent = 0D;
	
	public static double Cloud_Coverage_Max_Percent = 100D;
	
	public static int Thread_Particle_Process_Delay = 400;
	// sound
	public static double volWaterfallScale = 0.5D;
	public static double volWindTreesScale = 0.5D;
	public static double volWindLightningScale = 1D;
	
	// blocks
	public static double sirenActivateDistance = 256D;
	public static double sensorActivateDistance = 256D;
	
	public static boolean Block_SensorNoRecipe = false;
	public static boolean Block_SirenNoRecipe = false;
	public static boolean Block_SirenManualNoRecipe = false;
	public static boolean Block_WindVaneNoRecipe = false;
	public static boolean Block_AnemometerNoRecipe = false;
	public static boolean Block_WeatherForecastNoRecipe = false;
	public static boolean Block_SandLayerNoRecipe = false;
	public static boolean Block_SandNoRecipe = false;
	public static boolean Item_PocketSandNoRecipe = false;
	public static boolean Item_WeatherItemNoRecipe = false;
	
	// dimension settings
	public static String Dimension_List_Weather = "0,-127";
	public static String Dimension_List_Clouds = "0,-127";
	public static String Dimension_List_Storms = "0,-127";
	public static String Dimension_List_WindEffects = "0,-127";
	
	public static boolean Villager_MoveInsideForStorms = true;
	public static int Villager_MoveInsideForStorms_Dist = 256;
	
	public static double shaderParticleRateAmplifier = 3D;
	
	public static boolean blockBreakingInvokesCancellableEvent = false;
	
	public static boolean Global_Overcast_Prevent_Rain_Reset_On_Sleep = true;
	
	public static boolean Client_PotatoPC_Mode = false;
	
	public static boolean Aesthetic_Only_Mode = false;
}
