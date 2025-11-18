package net.gunivers.sniffer.debugcmd

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.gunivers.sniffer.command.BreakPointCommand
import net.minecraft.nbt.NbtByte
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtHelper
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Colors

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
                                val text = Text.literal("Assert failed. Result is not a byte: ").styled { style -> style.withColor(Colors.RED) }
                                when (result) {
                                    is NbtElement -> text.append(NbtHelper.toPrettyPrintedText(result))
                                    is Text -> text.append(result)
                                    else -> text.append(result.toString())
                                }
                                text.append("\n")
                                text.append("Stack trace:").append("\n")
                                text.append(BreakPointCommand.getStack(10))
                                it.source.server.playerManager.broadcast(text, false)
                                return@executes 0
                            }
                            if(result.value.toInt() == 0){
                                val text = Text.literal("Assert failed: result is 0").styled { style -> style.withColor(Colors.RED) },
                                text.append("\n")
                                text.append("Stack trace:").append("\n")
                                text.append(BreakPointCommand.getStack(10))
                                it.source.server.playerManager.broadcast(text, false)
                                return@executes 0
                            }
                            it.source.sendFeedback({ Text.literal("Assert passed") }, false)
                            return@executes 1
                        }
                    )
            )
        }
    }
}