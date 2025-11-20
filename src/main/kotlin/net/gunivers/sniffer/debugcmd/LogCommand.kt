package net.gunivers.sniffer.debugcmd

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object LogCommand {
    @JvmStatic
    fun onInitialize() {
        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
            dispatcher.register(
                literal<ServerCommandSource?>("log")
                    .requires{it.hasPermissionLevel(2)}
                    .then(argument("log", LogArgumentType())
                        .executes {
                            val log = LogArgumentType.getLog(it, "log")
                            //build output text
                            val text = Text.empty()
                            for (l in log.logs){
                                val data = l.get(it)
                                text.append(DebugData.toText(data))
                            }
                            it.source.server.playerManager.broadcast(text, false)
                            return@executes 1
                        }
                    )
            )
        }
    }

}