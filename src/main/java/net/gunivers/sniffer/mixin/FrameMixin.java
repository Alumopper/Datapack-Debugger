package net.gunivers.sniffer.mixin;

import net.gunivers.sniffer.command.BreakPointCommand;
import net.gunivers.sniffer.dap.ScopeManager;
import net.gunivers.sniffer.util.ReflectUtil;
import net.minecraft.command.Frame;
import net.minecraft.server.function.Procedure;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.gunivers.sniffer.command.StepType.isStepOut;

/**
 * Mixin for the Frame class to add additional functionality for debugging.
 * This mixin adds the capability to track which function a frame is associated with,
 * which is necessary for proper function call tracing during debugging.
 *
 * @author Alumopper
 */
@Mixin(Frame.class)
public class FrameMixin {
    /**
     * Reference to the function/procedure that created this frame.
     * Used by the debugger to track function execution and call hierarchies.
     */
    @Unique
    private Procedure<?> function;

    @Shadow @Final private int depth;

    @Inject(method = "doReturn", at = @At("HEAD"))
    private void beforeReturn(CallbackInfo ci) {
        // when a function is returned by a return command, the FunctionOutAction will not execute, so we need to execute it here manually
        ScopeManager.get().unscope();
        // BreakPointCommand.stepDepth - 1 because we only want to decrement if we go higher than the stepDepth
        if(BreakPointCommand.moveSteps > 0 && isStepOut() && depth - 1 <= BreakPointCommand.stepDepth - 1) BreakPointCommand.moveSteps --;
    }
}
