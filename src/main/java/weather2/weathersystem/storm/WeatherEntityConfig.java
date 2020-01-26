package weather2.weathersystem.storm;


public class WeatherEntityConfig {
	
    public static int TYPE_SPOUT = 0;
    public int type = 1;
    public float tornadoInitialSpeed;
    public float tornadoPullRate;
    public float tornadoLiftRate;
    public int relTornadoSize;
    public int tornadoBaseSize;
    public float tornadoWidthScale;
    public double grabDist = 120.0D;
    public int tornadoTime = 2500;
    public boolean grabsBlocks = true;
}
