package weather2.config;

public class ConfigTornado {



	public static boolean Storm_Tornado_RefinedGrabRules = true;
	public static boolean Storm_NoTornadosOrCyclones = false;
	//tornado
	public static boolean Storm_Tornado_grabPlayer = true;
	public static boolean Storm_Tornado_grabPlayersOnly = false;
	public static boolean Storm_Tornado_grabMobs = true;
	public static boolean Storm_Tornado_grabAnimals = true;
	public static boolean Storm_Tornado_grabVillagers = true;
	public static boolean Storm_Tornado_grabBlocks = true;
	public static boolean Storm_Tornado_grabItems = false;
	public static boolean Storm_Tornado_GrabCond_StrengthGrabbing = true;
	public static boolean Storm_Tornado_GrabCond_List = false;
	public static boolean Storm_Tornado_GrabCond_List_PartialMatches = false;
	public static boolean Storm_Tornado_GrabListBlacklistMode = false;
	public static String Storm_Tornado_GrabList = "planks, leaves";
	public static int Storm_Tornado_maxFlyingEntityBlocks = 200;
	public static int Storm_Tornado_maxBlocksGrabbedPerTick = 5;
	public static int Storm_Tornado_rarityOfDisintegrate = 15;
	public static int Storm_Tornado_rarityOfBreakOnFall = 5;
	public static boolean Storm_Tornado_aimAtPlayerOnSpawn = true;
	public static int Storm_Tornado_aimAtPlayerAngleVariance = 5;

	public static boolean Storm_Tornado_grabbedBlocksRepairOverTime = false;

	public static int Storm_Tornado_TicksToRepairBlock = 20*60*5;
}
