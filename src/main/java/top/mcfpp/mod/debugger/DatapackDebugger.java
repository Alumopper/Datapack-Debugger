package top.mcfpp.mod.debugger;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.mcfpp.mod.debugger.command.BreakPointCommand;

public class DatapackDebugger implements ModInitializer {
	private static final Logger logger = LoggerFactory.getLogger("datapack-debugger");

	@Override
	public void onInitialize() {

		

		BreakPointCommand.onInitialize();
	}

	public static Logger getLogger(){
		return logger;
	}
}
