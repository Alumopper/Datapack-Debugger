package top.mcfpp.mod.breakpoint;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.mcfpp.mod.breakpoint.command.BreakPointCommand;

public class DatapackBreakpoint implements ModInitializer {
	private static final Logger logger = LoggerFactory.getLogger("datapack-breakpoint");

	@Override
	public void onInitialize() {
		BreakPointCommand.onInitialize();
	}

	public static Logger getLogger(){
		return logger;
	}
}
