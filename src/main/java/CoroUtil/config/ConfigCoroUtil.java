package CoroUtil.config;

import java.io.File;

public class ConfigCoroUtil {

	public static String mobSpawnsProfile = "mob_spawns";

	public static String mobSpawnsWaveToForceUse = "";

	public static boolean forceShadersOff = false;

	public static boolean useEntityRenderHookForShaders = true;

	//maybe temp
	public static boolean optimizedCloudRendering = false;

	public static boolean debugShaders = false;

	public static boolean foliageShaders = false;

	public static boolean particleShaders = true;

	public static boolean useLoggingLog = true;

	public static boolean useLoggingDebug = false;

	public static boolean useLoggingError = true;

	public static boolean enableAdvancedDeveloperConfigFiles = false;

	public String getName() {
		return "General";
	}

	public String getRegistryName() {
		return "coroutil_general";
	}

	public String getConfigFileName() {
		return "CoroUtil" + File.separator + getName();
	}

	public String getCategory() {
		return getName();
	}
}
