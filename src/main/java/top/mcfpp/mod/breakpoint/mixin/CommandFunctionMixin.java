package top.mcfpp.mod.breakpoint.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.StringReader;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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
