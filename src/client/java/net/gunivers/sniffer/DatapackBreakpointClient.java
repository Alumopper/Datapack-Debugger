package net.gunivers.sniffer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import net.gunivers.sniffer.command.BreakPointCommand;;

/**
 * @author Alumopper
 * @author theogiraudet
 */
public class DatapackBreakpointClient implements ClientModInitializer {

	private static KeyBinding stepInto;

	@Override
	public void onInitializeClient() {
		stepInto = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"sniffer.step", // The translation key of the keybinding's name
				InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
				GLFW.GLFW_KEY_F7, // The keycode of the key
				KeyBinding.Category.create(Identifier.of("sniffer.name")) // The translation key of the keybinding's category.
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while(stepInto.wasPressed()) {
				if(BreakPointCommand.debugMode) {
					BreakPointCommand.step(1, client.player.getCommandSource(client.getServer().getWorld(client.player.getEntityWorld().getRegistryKey())));
				}
			}
		});

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if(MinecraftClient.getInstance().isIntegratedServerRunning() && DatapackDebugger.getWebSocketServer() != null){
				var port = DatapackDebugger.getWebSocketServer().getPort();
				client.player.sendMessage(
						Text.literal("DAP Server is running on port: ")
								.append(Text.literal("[" + port + "]").styled(style ->
												style.withColor(Colors.GREEN)
														.withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to copy")))
														.withClickEvent(new ClickEvent.CopyToClipboard(String.valueOf(port)))
										)
								),
						false
				);
			}
		});
	}

}
