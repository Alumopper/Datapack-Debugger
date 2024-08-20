package top.mcfpp.mod.debugger;

import net.fabricmc.api.ClientModInitializer;
import top.mcfpp.mod.debugger.command.EditCommand;
import top.mcfpp.mod.debugger.gui.EditScreenKeyBinding;

public class DatapackBreakpointClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		EditScreenKeyBinding.onInitialize();
		EditCommand.onInitialize();
	}
}
