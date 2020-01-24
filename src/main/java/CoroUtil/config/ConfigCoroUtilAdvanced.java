package CoroUtil.config;

import java.io.File;


public class ConfigCoroUtilAdvanced {

	public static boolean repairBlockNextRandomTick = false;

	public static boolean blockBreakingInvokesCancellableEvent = false;

	public static boolean removeInvasionAIWhenInvasionDone = true;

	//TODO: if false, will we be double buffing infernal mobs accidentally?
	public static boolean difficulty_OverrideInfernalMobs = true;

	public static boolean trackChunkData = true;

	public static double difficulty_MaxAttackSpeedLoggable = 10;

	public static double difficulty_MaxDPSRatingAllowed = 5;

	public static double difficulty_MaxDPSLoggable = 500;
    public static boolean chunkCacheOverworldOnly = false;
    public static boolean usePlayerRadiusChunkLoadingForFallback = true;
    public static String chunkCacheDimensionBlacklist_IDs = "";
    public static String chunkCacheDimensionBlacklist_Names = "promised";
    public static boolean trackPlayerData = false;
    public static boolean PFQueueDebug = false;
    public static boolean cleanupStrayMobs = false;
    public static int cleanupStrayMobsDayRate = 5;
    public static int cleanupStrayMobsTimeOfDay = 2000;
    public static boolean desirePathDerp = false;
    public static boolean headshots = false;
    public static boolean useCoroPets = false;
    public static boolean fixBadBiomeEntitySpawnEntries = false;
	public static boolean disableParticleRenderer = false;
	public static boolean disableMipmapFix = false;

	public static int worldTimeDelayBetweenLongDistancePathfindTries = 40;

	public static boolean logging_DPS_Fine = false;
	public static boolean logging_DPS_HighSources = false;

	public static boolean logging_DPS_All = false;

	public static boolean minersPushAwayOtherNonMinerMobsWhileMining = true;
	public static boolean minersPushAwayOnlyOtherBuffedMobs = true;

	public static boolean enableDebugRenderer = false;

	public String getName() {
		return "Advanced";
	}

	public String getRegistryName() {
		return "coroutil_advanced";
	}

	public String getConfigFileName() {
		return "CoroUtil" + File.separator + getName();
	}

	public String getCategory() {
		return getName();
	}

	public void hookUpdatedValues() {

	}

}
