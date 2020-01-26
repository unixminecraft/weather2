package weather2.config;

public class ConfigSand {


    public static boolean Storm_NoSandstorms = false;

	//sandstorm settings
	public static boolean Sandstorm_UseGlobalServerRate = false;
	public static int Sandstorm_OddsTo1 = 30;
	public static int Sandstorm_TimeBetweenInTicks = 20*60*20*3;

    public static int Sandstorm_Sand_Buildup_TickRate = 40;

    public static int Sandstorm_Sand_Buildup_LoopAmountBase = 800;

    public static boolean Sandstorm_Sand_Buildup_AllowOutsideDesert = true;

    public static boolean Sandstorm_Siren_PleaseNoDarude = false;
}
