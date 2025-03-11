package top.mcfpp.mod.debugger;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import top.mcfpp.mod.debugger.command.BreakPointCommand;;

public class DatapackBreakpointClient implements ClientModInitializer {

	private static KeyBinding stepInto;

	@Override
	public void onInitializeClient() {
		stepInto = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"datapack-debugger.step", // The translation key of the keybinding's name
				InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
				GLFW.GLFW_KEY_F7, // The keycode of the key
				"datapack-debugger.name" // The translation key of the keybinding's category.
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while(stepInto.wasPressed()) {
				if(BreakPointCommand.debugMode) {
					BreakPointCommand.step(1, client.player.getCommandSource(client.getServer().getWorld(client.player.getWorld().getRegistryKey())));
				}
			}
		});
	}

}
