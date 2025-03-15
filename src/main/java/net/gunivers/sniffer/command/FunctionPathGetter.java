package net.gunivers.sniffer.command;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

/**
 * Resource loader that captures the ResourceManager instance during resource reload.
 * This class is used to obtain a reference to the server's ResourceManager
 * which is needed to access datapack function resources during debugging.
 */
public class FunctionPathGetter implements SimpleSynchronousResourceReloadListener {

    /**
     * Stored reference to the server's resource manager.
     * This is populated during resource reloads and can be used to access datapack resources.
     */
    public static ResourceManager MANAGER;

    /**
     * Returns the identifier for this resource reload listener.
     * @return The identifier for this resource reload listener
     */
    @Override
    public Identifier getFabricId() {
        return Identifier.of("datapack-debug-loader");
    }

    /**
     * Called during resource reload to store the ResourceManager instance.
     * @param manager The server's resource manager instance
     */
    @Override
    public void reload(ResourceManager manager) {
        MANAGER = manager;
    }
}
