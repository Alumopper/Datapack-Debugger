package net.gunivers.sniffer.mixin;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.FunctionLoader;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(FunctionLoader.class)
public interface FunctionLoaderAccessors {
    @Accessor("functions")
    Map<Identifier, CommandFunction<ServerCommandSource>> getFunctions();

    @Accessor("functions")
    void setFunctions(Map<Identifier, CommandFunction<ServerCommandSource>> functions);

    @Accessor("commandDispatcher")
    CommandDispatcher<ServerCommandSource> getCommandDispatcher();

    @Accessor("level")
    int getLevel();
}
