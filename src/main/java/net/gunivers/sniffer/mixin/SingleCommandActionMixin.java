package net.gunivers.sniffer.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.gunivers.sniffer.command.BreakPointCommand;
import net.minecraft.command.*;
import net.minecraft.server.command.AbstractServerCommandSource;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static net.gunivers.sniffer.command.StepType.isStepOut;

@Mixin(SingleCommandAction.class)
public class SingleCommandActionMixin<T extends AbstractServerCommandSource<T>> {

    @Inject(method = "execute", at = @At(value = "JUMP", opcode = Opcodes.IFEQ, ordinal = 4))
    private void onExecute(
            T baseSource,
            List<T> sources,
            CommandExecutionContext<T> context,
            Frame frame,
            ExecutionFlags flags,
            CallbackInfo ci,
            @Local(ordinal = 1) ExecutionFlags executionFlags
    ){
        if (!executionFlags.isInsideReturnRun()) {
            if(BreakPointCommand.isDebugging && BreakPointCommand.moveSteps > 0 && !isStepOut()) {
                BreakPointCommand.moveSteps --;
            }
        }
    }
}
