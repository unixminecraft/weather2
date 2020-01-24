package weather2.config;

import java.io.File;

import weather2.Weather;


public class ConfigSnow {


	//snow
	public static boolean Snow_PerformSnowfall = false;
	//public static boolean Snow_ExtraPileUp = false;
	public static int Snow_RarityOfBuildup = 64;
	//public static int Snow_MaxBlockBuildupHeight = 3;
	//public static boolean Snow_SmoothOutPlacement = false;

    public String getName() {
        return "Snow";
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
