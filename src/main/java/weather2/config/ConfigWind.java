package weather2.config;

public class ConfigWind {

    public static boolean Misc_windOn = true;
    public static boolean Wind_LowWindEvents = true;
    public static boolean Wind_HighWindEvents = true;

    public static int lowWindTimerEnableAmountBase = 20*60*2;
    public static int lowWindTimerEnableAmountRnd = 20*60*10;
    public static int lowWindOddsTo1 = 20*200;

    public static int highWindTimerEnableAmountBase = 20*60*2;
    public static int highWindTimerEnableAmountRnd = 20*60*10;
    public static int highWindOddsTo1 = 20*400;

    public static double globalWindChangeAmountRate = 1F;

    public static double windSpeedMin = 0.00001D;
    public static double windSpeedMax = 1D;

    public static double windSpeedMinGlobalOvercastRaining = 0.3D;
}
