package top.mcfpp.mod.debugger.mixin;

import net.minecraft.command.SourcedCommandAction;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.ExpandedMacro;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.mcfpp.mod.debugger.command.FunctionInAction;
import top.mcfpp.mod.debugger.command.FunctionOutAction;

import java.util.List;

@Mixin(ExpandedMacro.class)
public class ExpandedMacroMixin<T extends AbstractServerCommandSource<T>> {

    @Shadow @Final private List<SourcedCommandAction<T>> entries;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        final var THIS = (ExpandedMacro<T>) (Object) this;
        entries.addFirst(new FunctionInAction<>(THIS));
        entries.add(new FunctionOutAction<>(THIS));
    }

}
