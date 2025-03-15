package net.gunivers.sniffer.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.command.CommandExecutionContext;
import net.minecraft.command.CommandFunctionAction;
import net.minecraft.command.Frame;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.Procedure;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

/**
 * Mixin for the CommandFunctionAction class to enhance function execution tracking.
 * This mixin associates the function being executed with its execution frame,
 * which is necessary for proper call stack tracking during debugging.
 *
 * @param <T> The type of command source being used
 */
@Mixin(CommandFunctionAction.class)
public class CommandFunctionActionMixin<T extends AbstractServerCommandSource<T>> {

    /** The function being executed by this action */
    @Shadow @Final private Procedure<T> function;

    /**
     * Injects code to associate the function with its execution frame.
     * This method is called when a function is executed and sets the function
     * reference in the newly created frame, allowing the debugger to track
     * which function is associated with each frame in the call stack.
     *
     * @param abstractServerCommandSource The command source executing the function
     * @param commandExecutionContext The command execution context
     * @param frame The parent execution frame
     * @param ci The callback info
     * @param frame2 The newly created frame for the function execution
     */
    @Inject(method = "execute(Lnet/minecraft/server/command/AbstractServerCommandSource;Lnet/minecraft/command/CommandExecutionContext;Lnet/minecraft/command/Frame;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/SteppedCommandAction;enqueueCommands(Lnet/minecraft/command/CommandExecutionContext;Lnet/minecraft/command/Frame;Ljava/util/List;Lnet/minecraft/command/SteppedCommandAction$ActionWrapper;)V"))
    public void onExecute(T abstractServerCommandSource, CommandExecutionContext<T> commandExecutionContext, Frame frame, CallbackInfo ci, @Local(ordinal = 1) Frame frame2){
        try {
            Field field = frame2.getClass().getDeclaredField("function");
            field.setAccessible(true);
            field.set(frame2, function);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
