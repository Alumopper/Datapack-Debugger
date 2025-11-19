package net.gunivers.sniffer.debugcmd

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.logging.LogUtils
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.gunivers.sniffer.DatapackDebugger
import net.gunivers.sniffer.command.BreakPointCommand
import net.minecraft.nbt.NbtByte
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtHelper
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import net.minecraft.util.Colors
import org.slf4j.Logger

object AssertCommand {

    private val LOGGER: Logger = LogUtils.getLogger()
    @JvmStatic
    fun onInitialize() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                literal<ServerCommandSource?>("assert")
                    .requires{it.hasPermissionLevel(2)}
                    .then(argument("expr", ExprArgumentType())
                        .executes {
                            try{
                                val expr = ExprArgumentType.getExpr(it, "expr")
                                val result = expr.get(it)
                                //check result
                                if(result !is NbtByte){
                                    val text = Text.literal("Assert failed. Result is not a byte: ").styled { style -> style.withColor(Colors.RED) }
                                    when (result) {
                                        is NbtElement -> text.append(NbtHelper.toPrettyPrintedText(result))
                                        is Text -> text.append(result)
                                        else -> text.append(result.toString())
                                    }
                                    text.append("\n")
                                    text.append("Expression: " + expr.content + "\n")
                                    text.append("Stack trace: \n")
                                    text.append(BreakPointCommand.getErrorStack(10))
                                    it.source.server.playerManager.broadcast(text, false)
                                    return@executes 0
                                }
                                if(result.value.toInt() == 0){
                                    val text = Text.literal("Assert failed: result is 0").styled { style -> style.withColor(Colors.RED) };
                                    text.append("\n")
                                    text.append("Expression: " + expr.content + "\n")
                                    text.append("Stack trace:").append("\n")
                                    text.append(BreakPointCommand.getErrorStack(10))
                                    it.source.server.playerManager.broadcast(text, false)
                                    return@executes 0
                                }
                                it.source.sendFeedback({ Text.literal("Assert passed") }, false)
                                return@executes 1
                            }catch (ex: CommandSyntaxException){
                                LOGGER.error("Exception while execution command:",ex)
                                val text = Text.literal("Assert failed: ").styled { style -> style.withColor(Colors.RED) }
                                text.append(ex.message ?: "Unknown error")
                                text.append("\n")
                                text.append("Stack trace:").append("\n")
                                text.append(BreakPointCommand.getStack(10))
                                it.source.server.playerManager.broadcast(text, false)
                                return@executes 0
                            }

                        }
                    )
            )
        }
    }
}