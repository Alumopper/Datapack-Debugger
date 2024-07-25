package top.mcfpp.mod.debugger.mixin;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandExecutionContext;
import net.minecraft.command.ExecutionFlags;
import net.minecraft.command.FixedCommandAction;
import net.minecraft.command.Frame;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static top.mcfpp.mod.debugger.utils.Debugger.*;


@Mixin(FixedCommandAction.class)
public class FixCommandActionMixin<T extends AbstractServerCommandSource<T>> {
    @Shadow @Final private String command;
    @Shadow @Final private ExecutionFlags flags;
    @Shadow @Final private CommandContext<T> context;

    @Inject(method = "execute(Lnet/minecraft/server/command/AbstractServerCommandSource;Lnet/minecraft/command/CommandExecutionContext;Lnet/minecraft/command/Frame;)V", at = @At("HEAD"))
    private void execute(T abstractServerCommandSource, CommandExecutionContext<T> commandExecutionContext, Frame frame, CallbackInfo ci) {
        if(frame.depth() == 0) return;
        if(isDebugging){
            if(moveSteps > 0) moveSteps --;
            if(this.command.startsWith("breakpoint")) return;
            if(abstractServerCommandSource instanceof ServerCommandSource serverCommandSource){
                var players = serverCommandSource.getServer().getPlayerManager().getPlayerList();
                for(var player : players){
                    player.sendMessage(Text.translatable("commands.breakpoint.run", this.command));
                }
            }
        }
    }

}
