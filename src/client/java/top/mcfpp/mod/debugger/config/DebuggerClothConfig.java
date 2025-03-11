package top.mcfpp.mod.debugger.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.function.Supplier;

/**
 * Cloth Config integration for the Datapack Debugger mod.
 * Provides a GUI interface for configuring the debugger.
 */
public class DebuggerClothConfig {

    /**
     * Creates a new Cloth Config screen for the Datapack Debugger.
     *
     * @param parent The parent screen
     * @return The config screen
     */
    public static Screen createConfigScreen(Screen parent) {
        // Get current config values
        DebuggerConfig config = DebuggerConfig.getInstance();
        
        // Create config builder
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("datapack-debugger.config.title"))
                .setSavingRunnable(config::save);

        // Create config entry builder
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // Create main category
        ConfigCategory mainCategory = builder.getOrCreateCategory(
                Text.translatable("datapack-debugger.config.category.main"));
        
        // Reference holders for the current port and path (to enable dynamic updates)
        final int[] currentPort = {config.getPort()};
        final String[] currentPath = {config.getPath()};
        
        // Create a supplier to dynamically generate the server address text
        Supplier<Text> addressSupplier = () -> {
            String wsAddress = String.format("ws://localhost:%d/%s", currentPort[0], currentPath[0]);
            return Text.translatable("datapack-debugger.config.server_address", wsAddress);
        };
        
        // Add server address description (will update dynamically)
        mainCategory.addEntry(entryBuilder.startTextDescription(addressSupplier.get())
                .setTooltip(Text.translatable("datapack-debugger.config.server_address.tooltip"))
                .build());

        // Add port entry with dynamic update of the address
        mainCategory.addEntry(entryBuilder.startIntField(
                        Text.translatable("datapack-debugger.config.port"), 
                        config.getPort())
                .setDefaultValue(25599)
                .setTooltip(Text.translatable("datapack-debugger.config.port.tooltip"))
                .setMin(1024)
                .setMax(65535)
                .setSaveConsumer(value -> {
                    config.setPort(value);
                    currentPort[0] = value; // Update reference for address display
                })
                .build());
        
        // Add path entry with dynamic update of the address
        mainCategory.addEntry(entryBuilder.startStrField(
                        Text.translatable("datapack-debugger.config.path"), 
                        config.getPath())
                .setDefaultValue("dap")
                .setTooltip(Text.translatable("datapack-debugger.config.path.tooltip"))
                .setSaveConsumer(value -> {
                    config.setPath(value);
                    currentPath[0] = value; // Update reference for address display
                })
                .build());
        
        // Return the config screen
        return builder.build();
    }
} 