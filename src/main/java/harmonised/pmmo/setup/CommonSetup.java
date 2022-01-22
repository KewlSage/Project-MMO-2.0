package harmonised.pmmo.setup;

import harmonised.pmmo.commands.CmdPmmoRoot;
import harmonised.pmmo.config.readers.CoreParser;
import harmonised.pmmo.config.readers.PerksParser;
import harmonised.pmmo.core.XpUtils;
import harmonised.pmmo.core.perks.PerkRegistration;
import harmonised.pmmo.network.Networking;
import harmonised.pmmo.storage.PmmoSavedData;
import harmonised.pmmo.util.MsLoggy;
import harmonised.pmmo.util.Reference;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid=Reference.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class CommonSetup {
	
	public static void init(final FMLCommonSetupEvent event) {
		System.out.println("Common Setup Fired");
		Networking.registerMessages();
	}
	
	@SubscribeEvent
	public static void onServerStartup(ServerStartingEvent event) {
		MsLoggy.info("Loading PMMO Saved Data");
		PmmoSavedData.init(event.getServer());
		MsLoggy.info("Computing data for cache");
		XpUtils.computeLevelsForCache();
		MsLoggy.info("Loading settings from config jsons");
		CoreParser.init();
		PerksParser.parsePerks();
		MsLoggy.info("Executing Default Registrations");
		PerkRegistration.init();
		MsLoggy.info("PMMO Server loading process complete");
	}
	
	@SubscribeEvent
	public static void onConfigReload(ModConfigEvent.Reloading event) {
		if (event.getConfig().getType().equals(ModConfig.Type.SERVER))
			XpUtils.computeLevelsForCache();
	}
	
	@SubscribeEvent
	public static void onCommandRegister(RegisterCommandsEvent event) {
		CmdPmmoRoot.register(event.getDispatcher());
	}
}
