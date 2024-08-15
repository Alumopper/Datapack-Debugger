package top.mcfpp.mod.debugger;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class EditScreenKeyBinding {
    private static KeyBinding keyBinding;
    public static void onInitialize(){
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.datapack_debugger.open_screen", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_PAGE_UP, // The keycode of the key
                "category.datapack_debugger.open_screen" // The translation key of the keybinding's category. The category is related to how the keybinding is grouped in the settings page
                ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new EditScreen());
                client.player.sendMessage(Text.literal("Open Screen"), false);
            }
        });

    }
}
