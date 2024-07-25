package top.mcfpp.mod.debugger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.mcfpp.mod.debugger.command.BreakPointCommand;
import top.mcfpp.mod.debugger.items.Break;
import top.mcfpp.mod.debugger.items.Continue;
import top.mcfpp.mod.debugger.items.Stack;
import top.mcfpp.mod.debugger.items.Step;

import java.util.HashMap;
import java.util.Map;

public class DatapackDebugger implements ModInitializer {
    public static final String MOD_ID = "datapack-debugger";
    private static final Logger logger = LoggerFactory.getLogger("datapack-debugger");
    public static final RegistryKey<ItemGroup> DEBUGGER_ITEM_GROUP_KEY = RegistryKey.of(Registries.ITEM_GROUP.getKey(), Identifier.of("mcfpp", "item_group"));
    public static final ItemGroup DEBUGGER_ITEM_GROUP = FabricItemGroup.builder()
            .displayName(Text.translatable("itemGroup." + MOD_ID))
            .icon(() -> new ItemStack(Registries.ITEM.get(Identifier.of(MOD_ID, "break")))
            )
            .build();

    @Override
    public void onInitialize() {
        BreakPointCommand.onInitialize();

        Registry.register(Registries.ITEM_GROUP, DEBUGGER_ITEM_GROUP_KEY, DEBUGGER_ITEM_GROUP);

        var ITEMS = new HashMap<String, Item>();
        ITEMS.put("break", new Break(new Item.Settings()));
        ITEMS.put("continue", new Continue(new Item.Settings()));
        ITEMS.put("step", new Step(new Item.Settings()));
        ITEMS.put("stack", new Stack(new Item.Settings()));

        for (Map.Entry<String, Item> item : ITEMS.entrySet()) {
            Registry.register(Registries.ITEM, Identifier.of(MOD_ID, item.getKey()), item.getValue());
            ItemGroupEvents.modifyEntriesEvent(DEBUGGER_ITEM_GROUP_KEY).register(itemGroup -> itemGroup.add(item.getValue()));
        }
    }

    public static Logger getLogger() {
        return logger;
    }
}
