package top.mcfpp.mod.breakpoint.mixin;

import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandExecutionContext;
import net.minecraft.command.FixedCommandAction;
import net.minecraft.command.Frame;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.Tracer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.mcfpp.mod.breakpoint.command.BreakPointCommand;

import java.util.function.Supplier;

@Mixin(FixedCommandAction.class)
public class FixCommandActionMixin<T extends AbstractServerCommandSource<T>> {

    @Inject(method = "execute(Lnet/minecraft/server/command/AbstractServerCommandSource;Lnet/minecraft/command/CommandExecutionContext;Lnet/minecraft/command/Frame;)V", at = @At("HEAD"))
    private void execute(T abstractServerCommandSource, CommandExecutionContext<T> commandExecutionContext, Frame frame, CallbackInfo ci) {
        if(BreakPointCommand.moveSteps > 0) BreakPointCommand.moveSteps --;
    }

}
