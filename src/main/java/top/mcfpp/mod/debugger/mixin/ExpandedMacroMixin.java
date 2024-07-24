package top.mcfpp.mod.debugger.mixin;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandExecutionContext;
import net.minecraft.command.SourcedCommandAction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.ExpandedMacro;
import net.minecraft.server.function.Procedure;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.mcfpp.mod.debugger.command.FunctionInAction;
import top.mcfpp.mod.debugger.command.FunctionOutAction;

import java.util.ArrayList;
import java.util.List;

@Mixin(ExpandedMacro.class)
public class ExpandedMacroMixin<T extends AbstractServerCommandSource<T>> {

    @Unique private NbtCompound arguments = null;

    @Shadow @Final private List<SourcedCommandAction<T>> entries;

    @Inject(method = "withMacroReplaced", at = @At("HEAD"), cancellable = true)
    private void OnWithMacroReplaced(@Nullable NbtCompound arguments, CommandDispatcher<T> dispatcher, CallbackInfoReturnable<Procedure<T>> cir){

        final var THIS = (ExpandedMacro<T>) (Object) this;

        var newFunction = new ExpandedMacro<>(THIS.id(), new ArrayList<>(THIS.entries()));

        newFunction.entries().addFirst(new FunctionInAction<>(THIS));
        newFunction.entries().add(new FunctionOutAction<>(THIS));

        cir.setReturnValue(newFunction);

    }

}
