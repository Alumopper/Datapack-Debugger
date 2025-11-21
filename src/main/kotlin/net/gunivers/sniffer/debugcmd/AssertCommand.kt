package net.gunivers.sniffer.debugcmd

import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.logging.LogUtils
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.gunivers.sniffer.DatapackDebugger
import net.gunivers.sniffer.command.BreakPointCommand
import net.gunivers.sniffer.util.Extension.appendLine
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
                                    val text = Text.translatable("sniffer.commands.assert.failed.not_a_byte").styled { style -> style.withColor(Colors.RED) }
                                    when (result) {
                                        is NbtElement -> text.append(NbtHelper.toPrettyPrintedText(result))
                                        is Text -> text.append(result)
                                        else -> text.append(result.toString())
                                    }
                                    text.appendLine()
                                    text.appendLine(Text.translatable("sniffer.commands.assert.failed.expression", expr.content))
                                    text.appendLine(Text.translatable("sniffer.commands.assert.failed.stack"))
                                    text.append(BreakPointCommand.getErrorStack(10))
                                    it.source.server.playerManager.broadcast(text, false)
                                    return@executes 0
                                }
                                if(result.value.toInt() == 0){
                                    val text = Text.translatable("sniffer.commands.assert.failed.result_is_zero").styled { style -> style.withColor(Colors.RED) };
                                    text.appendLine()
                                    text.appendLine(Text.translatable("sniffer.commands.assert.failed.expression", expr.content))
                                    text.appendLine(Text.translatable("sniffer.commands.assert.failed.stack"))
                                    text.append(BreakPointCommand.getErrorStack(10))
                                    it.source.server.playerManager.broadcast(text, false)
                                    return@executes 0
                                }
                                it.source.sendFeedback({ Text.translatable("sniffer.commands.assert.passed") }, false)
                                return@executes 1
                            }catch (ex: CommandSyntaxException){
                                LOGGER.error("Exception while execution command:",ex)
                                val text = Text.translatable("sniffer.commands.assert.failed").styled { style -> style.withColor(Colors.RED) }
                                text.appendLine(ex.message?.let(Text::literal) ?: Text.translatable("sniffer.commands.assert.failed.unknown_error"))
                                text.appendLine(Text.translatable("sniffer.commands.assert.failed.stack"))
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