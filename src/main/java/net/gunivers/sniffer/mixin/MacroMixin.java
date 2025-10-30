package net.gunivers.sniffer.mixin;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.ExpandedMacro;
import net.minecraft.server.function.Macro;
import net.minecraft.server.function.Procedure;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Mixin for the Macro class to enhance debugging capabilities for macros.
 * This mixin captures and preserves macro arguments during expansion,
 * allowing the debugger to display macro parameter values during debugging.
 *
 * @param <T> The type of command source being used
 *
 * @author Alumopper
 */
@Mixin(Macro.class)
public abstract class MacroMixin<T extends AbstractServerCommandSource<T>> {

    @Shadow public abstract Identifier id();

    /** Stores the NBT compound containing macro arguments */
    @Unique private NbtCompound arguments;

    /**
     * Captures the macro arguments when a macro is being expanded.
     * This method is injected at the start of the withMacroReplaced method
     * to store the arguments for later use.
     *
     * @param arguments The NBT compound containing macro arguments
     * @param dispatcher The command dispatcher
     * @param cir The callback info returnable
     */
    @Inject(method = "withMacroReplaced(Lnet/minecraft/nbt/NbtCompound;Lcom/mojang/brigadier/CommandDispatcher;)Lnet/minecraft/server/function/Procedure;", at = @At("HEAD"))
    private void OnWithMacroReplaced(NbtCompound arguments, CommandDispatcher<T> dispatcher, CallbackInfoReturnable<Procedure<T>> cir){
        this.arguments = arguments;
    }

    /**
     * Injects the captured arguments into the expanded macro.
     * This method is injected at the end of the withMacroReplaced method
     * to ensure the expanded macro has access to the original arguments.
     *
     * @param varNames The list of variable names
     * @param arguments The list of argument values
     * @param dispatcher The command dispatcher
     * @param cir The callback info returnable
     */
    @Inject(method = "withMacroReplaced(Ljava/util/List;Ljava/util/List;Lcom/mojang/brigadier/CommandDispatcher;)Lnet/minecraft/server/function/Procedure;", at = @At("RETURN"), cancellable = true)
    private void OnWithMacroReplaced(List<String> varNames, List<String> arguments, CommandDispatcher<T> dispatcher, CallbackInfoReturnable<Procedure<T>> cir){
        ExpandedMacro<T> function = (ExpandedMacro<T>) cir.getReturnValue();
        try {
            Field field = function.getClass().getDeclaredField("arguments");
            field.setAccessible(true);
            field.set(function, this.arguments);
            field = function.getClass().getDeclaredField("functionIdentifier");
            field.setAccessible(true);
            field.set(function, this.id());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        cir.setReturnValue(function);
    }

}
