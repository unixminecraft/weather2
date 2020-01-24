package weather2.config;

import java.io.File;

import weather2.Weather;

public class ConfigFoliage {

    public static int foliageShaderRange = 40;
    public static int Thread_Foliage_Process_Delay = 1000;
    public static boolean extraGrass = false;

    public String getName() {
        return "Foliage";
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
