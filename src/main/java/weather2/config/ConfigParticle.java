package weather2.config;

import java.io.File;

import weather2.Weather;


public class ConfigParticle {



    //particles
	public static boolean Wind_Particle_leafs = true;
	public static double Wind_Particle_effect_rate = 0.7D;
	public static boolean Wind_Particle_waterfall = true;
	//public static boolean Wind_Particle_snow = false;
	public static boolean Wind_Particle_fire = true;
	public static boolean Particle_RainSnow = true;
    public static boolean Particle_Rain_GroundSplash = true;
    public static boolean Particle_Rain_DownfallSheet = true;
	public static boolean Particle_VanillaAndWeatherOnly = false;
	public static double Precipitation_Particle_effect_rate = 0.7D;
	public static double Sandstorm_Particle_Debris_effect_rate = 0.6D;
	public static double Sandstorm_Particle_Dust_effect_rate = 0.6D;

    public String getName() {
        return "Particle";
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
