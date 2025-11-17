package net.gunivers.sniffer.debugcmd

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.nbt.NbtByte
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object AssertCommand {
    @JvmStatic
    fun onInitialize() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                literal<ServerCommandSource?>("assert")
                    .requires{it.hasPermissionLevel(2)}
                    .then(argument("expr", ExprArgumentType())
                        .executes {
                            val result = ExprArgumentType.getExpr(it, "expr").get(it)
                            //check result
                            if(result !is NbtByte){
                                it.source.server.playerManager.broadcast(
                                    Text.literal("Assert failed: result is not a byte").styled { style -> style.withColor(0xff0000) },
                                    false
                                )
                                return@executes 0
                            }
                            if(result.value.toInt() == 0){
                                it.source.server.playerManager.broadcast(
                                    Text.literal("Assert failed: result is 0").styled { style -> style.withColor(0xff0000) },
                                    false
                                )
                                return@executes 0
                            }
                            it.source.sendFeedback(
                                { Text.literal("Assert passed") }
                                , false
                            )
                            return@executes 1
                        }
                    )
            )
        }
    }
}