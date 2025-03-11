package top.mcfpp.mod.debugger.command;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public class FunctionPathGetter implements SimpleSynchronousResourceReloadListener {

    public static ResourceManager MANAGER;

    @Override
    public Identifier getFabricId() {
        return Identifier.of("datapack-debug-loader");
    }

    @Override
    public void reload(ResourceManager manager) {
        MANAGER = manager;
    }
}
