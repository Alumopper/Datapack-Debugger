package net.gunivers.sniffer.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.function.CommandFunctionManager;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandFunctionManager.class)
public class CommandFunctionManagerMixin {

    @Shadow @Final private static Logger LOGGER;

    @Shadow @Final private MinecraftServer server;

    @Inject(method = "tick", at = @At("HEAD"))
    public void beforeTick(CallbackInfo ci){
        //TODO at every start of a tick, the stack should be empty
//        if(this.server.getTickManager().shouldTick()){
//            ScopeManager.get().clear();
//        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void afterTick(CallbackInfo ci) {
        //TODO at the end of a tick, the stack should be empty too, or a leak has occurred
//        if(this.server.getTickManager().shouldTick()){
//            if(!ScopeManager.get().isEmpty()){
//                var scopes = new StringBuilder();
//                for (var scope: ScopeManager.get().getDebugScopes()){
//                    scopes.append(scope.getFunction()).append('\n');
//                }
//                LOGGER.warn("A leak occurred! \n Current scopes: \n {}", scopes);
//                ScopeManager.get().clear();
//            }
//        }
    }
}
