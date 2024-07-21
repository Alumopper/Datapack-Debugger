package top.mcfpp.mod.debugger.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.FunctionBuilder;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(CommandFunction.class)
public interface CommandFunctionMixin {

    @ModifyVariable(method = "create", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static List<String> create(List<String> value) {
        ArrayList<String> list = new ArrayList<>();
        for (String str : value){
            if(str.equals("#breakpoint")){
                list.add("breakpoint");
            }else {
                list.add(str);
            }
        }
        return list;
    }

}
