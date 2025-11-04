package net.gunivers.sniffer.mixin;

import net.gunivers.sniffer.command.BreakPointCommand;
import net.minecraft.command.CommandExecutionContext;
import net.minecraft.command.FallthroughCommandAction;
import net.minecraft.command.Frame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.gunivers.sniffer.command.StepType.isStepOut;

@Mixin(FallthroughCommandAction.class)
public class FallthroughCommandActionMixin<T> {
    @Inject(method = "execute", at = @At("HEAD"))
    private void onExecute(CommandExecutionContext<T> commandExecutionContext, Frame frame, CallbackInfo ci){
        if(BreakPointCommand.isDebugging && BreakPointCommand.moveSteps > 0 && !isStepOut()) {
            BreakPointCommand.moveSteps --;
        }
    }
}
