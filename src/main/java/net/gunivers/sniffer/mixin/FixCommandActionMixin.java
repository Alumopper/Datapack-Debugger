package net.gunivers.sniffer.mixin;

import net.minecraft.command.CommandExecutionContext;
import net.minecraft.command.FixedCommandAction;
import net.minecraft.command.Frame;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.gunivers.sniffer.command.BreakPointCommand;

import static net.gunivers.sniffer.util.Utils.addSnifferPrefix;
import static net.gunivers.sniffer.command.StepType.isStepOut;

/**
 * Mixin for the FixedCommandAction class to enhance command execution during debugging.
 * This mixin adds debugging capabilities to individual command execution,
 * allowing the debugger to track and control command execution flow.
 *
 * @param <T> The type of command source being used
 *
 * @author Alumopper
 */
@Mixin(FixedCommandAction.class)
public class FixCommandActionMixin<T extends AbstractServerCommandSource<T>> {
    /** The command string being executed */
    @Shadow @Final private String command;

    /**
     * Injects code at the start of command execution to handle debugging.
     * This method manages step execution in debug mode and notifies players
     * about the currently executing command for better debugging visibility.
     *
     * @param abstractServerCommandSource The command source executing the command
     * @param commandExecutionContext The command execution context
     * @param frame The execution frame
     * @param ci The callback info
     */
    @Inject(method = "execute(Lnet/minecraft/server/command/AbstractServerCommandSource;Lnet/minecraft/command/CommandExecutionContext;Lnet/minecraft/command/Frame;)V",
            at = @At("HEAD"))
    private void execute(T abstractServerCommandSource, CommandExecutionContext<T> commandExecutionContext, Frame frame, CallbackInfo ci) {
        if(frame.depth() == 0)
            return;
        // If we are in debug mode, we execute as many commands as determined by the moveSteps variable, except if we found a breakpoint before
        if(BreakPointCommand.isDebugging) {
            if(BreakPointCommand.moveSteps > 0 && !isStepOut()) BreakPointCommand.moveSteps --;
            if(this.command.startsWith("breakpoint")) return;
            if(abstractServerCommandSource instanceof ServerCommandSource serverCommandSource) {
                // We send to each player the executing command
                var players = serverCommandSource.getServer().getPlayerManager().getPlayerList();
                for(var player : players) {
                    player.sendMessage(addSnifferPrefix(Text.translatable("sniffer.commands.breakpoint.run", this.command).formatted(Formatting.WHITE)));
                }
            }
        }
    }

}
