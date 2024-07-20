package top.mcfpp.mod.breakpoint.mixin;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.ExpandedMacro;
import net.minecraft.server.function.Macro;
import net.minecraft.server.function.Procedure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.List;

@Mixin(Macro.class)
public class MacroMixin<T extends AbstractServerCommandSource<T>> {

    @Unique private NbtCompound arguments;

    @Inject(method = "withMacroReplaced(Lnet/minecraft/nbt/NbtCompound;Lcom/mojang/brigadier/CommandDispatcher;)Lnet/minecraft/server/function/Procedure;", at = @At("HEAD"))
    private void OnWithMacroReplaced(NbtCompound arguments, CommandDispatcher<T> dispatcher, CallbackInfoReturnable<Procedure<T>> cir){
        this.arguments = arguments;
    }

    @Inject(method = "withMacroReplaced(Ljava/util/List;Ljava/util/List;Lcom/mojang/brigadier/CommandDispatcher;)Lnet/minecraft/server/function/Procedure;", at = @At("RETURN"), cancellable = true)
    private void OnWithMacroReplaced(List<String> varNames, List<String> arguments, CommandDispatcher<T> dispatcher, CallbackInfoReturnable<Procedure<T>> cir){
        ExpandedMacro<T> function = (ExpandedMacro<T>) cir.getReturnValue();
        try {
            Field field = function.getClass().getDeclaredField("arguments");
            field.setAccessible(true);
            field.set(function, this.arguments);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        cir.setReturnValue(function);
    }

}
