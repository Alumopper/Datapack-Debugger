package top.mcfpp.mod.debugger.mixin;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandExecutionContext;
import net.minecraft.command.ExecutionFlags;
import net.minecraft.command.FixedCommandAction;
import net.minecraft.command.Frame;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.mcfpp.mod.debugger.command.BreakPointCommand;
import top.mcfpp.mod.debugger.command.FunctionInAction;
import top.mcfpp.mod.debugger.command.FunctionStackManager;

@Mixin(FixedCommandAction.class)
public class FixCommandActionMixin<T extends AbstractServerCommandSource<T>> {
    @Shadow @Final private String command;
    @Shadow @Final private ExecutionFlags flags;
    @Shadow @Final private CommandContext<T> context;

    @Inject(method = "execute(Lnet/minecraft/server/command/AbstractServerCommandSource;Lnet/minecraft/command/CommandExecutionContext;Lnet/minecraft/command/Frame;)V", at = @At("HEAD"))
    private void execute(T abstractServerCommandSource, CommandExecutionContext<T> commandExecutionContext, Frame frame, CallbackInfo ci) {
        if(frame.depth() == 0)
            return;
        // If we are in debug mode, we execute as many commands as determined by the moveSteps variable, except if we found a breakpoint before
        if(BreakPointCommand.isDebugging){
            if(BreakPointCommand.moveSteps > 0) BreakPointCommand.moveSteps --;
            if(this.command.startsWith("breakpoint")) return;
            if(abstractServerCommandSource instanceof ServerCommandSource serverCommandSource) {
                // We send to each player the executing command
                var players = serverCommandSource.getServer().getPlayerManager().getPlayerList();
                for(var player : players){
                    player.sendMessage(Text.translatable("commands.breakpoint.run", this.command));
                }
            }
        }
    }

}
