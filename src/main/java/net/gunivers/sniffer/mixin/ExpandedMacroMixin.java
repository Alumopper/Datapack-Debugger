package net.gunivers.sniffer.mixin;

import net.minecraft.command.SourcedCommandAction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.ExpandedMacro;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.gunivers.sniffer.command.FunctionInAction;
import net.gunivers.sniffer.command.FunctionOutAction;

import java.util.List;

/**
 * Mixin for the ExpandedMacro class to add debugging capabilities to macros.
 * This mixin injects function entry and exit actions into macro execution,
 * allowing the debugger to track macro execution in the same way as functions.
 *
 * @param <T> The type of command source being used
 *
 * @author Alumopper
 * @author Wenz-jam
 */
@Mixin(ExpandedMacro.class)
public class ExpandedMacroMixin<T extends AbstractServerCommandSource<T>> {

    /** The list of command actions in this expanded macro */
    @Shadow @Final private List<SourcedCommandAction<T>> entries;

    /** Stores the NBT compound containing macro arguments */
    @Unique
    private NbtCompound arguments;

    @Unique
    private Identifier functionIdentifier;

    /**
     * Injects function entry and exit actions into the macro's command list.
     * This allows the debugger to track when a macro is entered and exited,
     * similar to how it tracks function calls.
     *
     * @param ci Callback info for the injection
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        final var THIS = (ExpandedMacro<T>) (Object) this;
        entries.addFirst(new FunctionInAction<>(THIS));
        entries.add(new FunctionOutAction<>(THIS));
        this.functionIdentifier = THIS.id();
    }

}
