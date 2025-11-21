package net.gunivers.sniffer.debugcmd

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.context.CommandContext
import com.mojang.logging.LogUtils
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.gunivers.sniffer.util.Extension.appendLine
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Colors
import kotlin.math.max
import kotlin.math.min

object JvmtimerCommand {

    private val LOGGER = LogUtils.getLogger()

    val timers = HashMap<String, JvmTimer>()

    @JvmStatic
    fun onInitialize() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                literal<ServerCommandSource?>("jvmtimer")
                    .requires { it.hasPermissionLevel(2) }
                    .then(literal<ServerCommandSource?>("start")
                        .then(argument("id", StringArgumentType.string())
                            .suggests(JvmtimerSuggestionProvider)
                            .executes {
                                val id = StringArgumentType.getString(it, "id")
                                getTimer(id).start()
                                it.source.sendFeedback({ Text.translatable("sniffer.commands.jvmtimer.started", id) }, false)
                                1
                            }
                        )
                    ).then(literal<ServerCommandSource?>("end")
                        .then(argument("id", StringArgumentType.string())
                            .suggests(JvmtimerSuggestionProvider)
                            .executes {
                                val id = StringArgumentType.getString(it ,"id")
                                getTimer(id).end()
                                it.source.sendFeedback({ Text.translatable("sniffer.commands.jvmtimer.stopped", id) }, false)
                                1
                            }
                        )
                    ).then(literal<ServerCommandSource?>("get")
                        .then(argument("id", StringArgumentType.string())
                            .suggests(JvmtimerSuggestionProvider)
                            .executes {
                                val id = StringArgumentType.getString(it ,"id")
                                getTimer(id).get(it)
                                1
                            }
                        )
                    ).then(literal<ServerCommandSource?>("reset")
                        .then(argument("id", StringArgumentType.string())
                            .suggests(JvmtimerSuggestionProvider)
                            .executes {
                                val id = StringArgumentType.getString(it ,"id")
                                getTimer(id).reset()
                                it.source.sendFeedback({ Text.translatable("sniffer.commands.jvmtimer.reset", id) }, false)
                                1
                            }
                        )
                    ).then(literal<ServerCommandSource?>("disable")
                        .then(argument("id", StringArgumentType.string())
                            .suggests(JvmtimerSuggestionProvider)
                            .executes {
                                val id = StringArgumentType.getString(it ,"id")
                                getTimer(id).disable()
                                it.source.sendFeedback({ Text.translatable("sniffer.commands.jvmtimer.disable", id) }, false)
                                1
                            }
                        )
                    )
            )
        }
    }

    fun getTimer(id: String): JvmTimer{
        return timers[id] ?: JvmTimer(id).also { timers[id] = it }
    }

    class JvmTimer(val id: String) {

        private var startTime = -1L

        private var totalTime = 0L

        private var count = 0

        private var enabled = true

        private var minTime = Long.MAX_VALUE

        private var maxTime = Long.MIN_VALUE

        fun start(){
            if(!enabled) return
            if((startTime != -1L)){
                LOGGER.warn("Timer $id is already started! Possible end missing? Timer $id has been disable!")
                enabled = false
                startTime = -1L
            }
            startTime = System.nanoTime()
        }

        fun end(){
            if(!enabled) return
            if((startTime == -1L)){
                LOGGER.warn("Timer $id is not started")
                enabled = false
                return
            }
            val delta = System.nanoTime() - startTime
            totalTime += delta
            minTime = min(minTime, delta)
            maxTime = max(maxTime, delta)
            count++
            startTime = -1L
        }

        fun get(ctx: CommandContext<ServerCommandSource>){
            if(count == 0){
                ctx.source.sendFeedback({ Text.translatable("sniffer.commands.jvmtimer.not_started", id) }, false)
                return
            }
            if(!enabled){
                ctx.source.sendFeedback({ Text.translatable("sniffer.commands.jvmtimer.disable", id) }, false)
                return
            }
            val text = Text.empty()
            text.title("sniffer.commands.jvmtimer.info.id").value(id)
                .desc("sniffer.commands.jvmtimer.info.total").value("${totalTime / 1_000_000.0}ms")
                .desc("sniffer.commands.jvmtimer.info.count").value(count.toString())
                .desc("sniffer.commands.jvmtimer.info.average").value("${totalTime / count / 1_000_000.0}ms")
                .desc("sniffer.commands.jvmtimer.info.max_min").value("${maxTime / 1_000.0}μs/${minTime / 1_000.0}μs")
            ctx.source.sendFeedback({ text }, false)
        }
        fun reset(){
            startTime = -1L
            totalTime = 0L
            count = 0
            enabled = true
            maxTime = Long.MIN_VALUE
            minTime = Long.MAX_VALUE
        }

        fun disable(){
            reset()
            enabled = false
        }

        private fun MutableText.title(str: String): MutableText =
            this.appendLine(Text.translatable(str).styled { it.withColor(Colors.CYAN).withBold(true) })

        private fun MutableText.desc(str: String): MutableText =
            this.appendLine(Text.translatable(str).styled { it.withColor(Colors.WHITE).withBold(false) })

        private fun MutableText.value(str: String): MutableText =
            this.appendLine(Text.literal(str).styled { it.withColor(Colors.CYAN).withBold(false) })
    }
}