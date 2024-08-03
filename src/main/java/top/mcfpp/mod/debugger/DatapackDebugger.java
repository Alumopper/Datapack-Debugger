package top.mcfpp.mod.debugger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.mcfpp.mod.debugger.command.BreakPointCommand;

public class DatapackDebugger implements ModInitializer {
	private static final Logger logger = LoggerFactory.getLogger("datapack-debugger");

	@Override
	public void onInitialize() {

		ServerLifecycleEvents.SERVER_STARTED.register(server -> BreakPointCommand.clear());

		BreakPointCommand.onInitialize();
	}

	public static Logger getLogger(){
		return logger;
	}
}
