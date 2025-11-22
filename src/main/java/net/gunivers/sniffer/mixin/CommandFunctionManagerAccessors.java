package net.gunivers.sniffer.mixin;

import net.minecraft.server.function.CommandFunctionManager;
import net.minecraft.server.function.FunctionLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CommandFunctionManager.class)
public interface CommandFunctionManagerAccessors {

    @Accessor("loader")
    FunctionLoader getLoader();
}
