package top.mcfpp.mod.breakpoint.command

import com.google.common.collect.Queues
import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandExecutionContext
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.server.command.CommandManager.RegistrationEnvironment
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import top.mcfpp.mod.breakpoint.DatapackBreakpoint
import java.util.*

object BreakPointCommand {

    var isDebugCommand = false

    /**
     * 是否正在Debug断点调试中
     */
    var isDebugging = false

    var moveSteps = 0

    val storedCommandExecutionContext: Deque<CommandExecutionContext<*>> = Queues.newArrayDeque()

    private val LOGGER = DatapackBreakpoint.logger

    fun onInitialize() {
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher: CommandDispatcher<ServerCommandSource?>, registryAccess: CommandRegistryAccess?, environment: RegistrationEnvironment? ->
            dispatcher.register(literal("breakpoint")
                .requires { it.hasPermissionLevel(2) }
                .executes { context ->
                    context.source.sendFeedback({Text.literal("已触发断点")}, false)
                    breakPoint(context.source)
                    1
                }
                .then(literal("step")
                    .executes{context ->
                        context.source.sendFeedback({Text.literal("已触发单步")}, false)
                        step(1, context.source)
                        1
                    }
                )
                .then(literal("move")
                    .executes{context ->
                        context.source.sendFeedback({Text.literal("已恢复断点")}, false)
                        moveOn(context.source)
                        1
                    }
                )
            )
        })

    }

    private fun breakPoint(source: ServerCommandSource) {
        val serverTickManager = source.server.tickManager
        serverTickManager.isFrozen = true
        isDebugging = true
    }

    private fun step(steps: Int, source: ServerCommandSource) {
        if(!isDebugging){
            source.sendError(Text.literal("只能在断点模式下使用step指令"))
            return
        }
        isDebugCommand = true
        moveSteps = steps
        var context: CommandExecutionContext<*>? = null
        try {
            while (moveSteps > 0) {
                context = storedCommandExecutionContext.peekFirst()
                if (context != null) {
                    // 执行相关操作
                    context.run()
                    if(moveSteps != 0){
                        //执行完毕了一个函数
                        storedCommandExecutionContext.pollFirst().close()
                    }
                } else {
                    source.sendFeedback({Text.literal("当前刻已执行完毕，退出调试模式")}, false)
                    moveOn(source)
                }
            }
        } catch (e: Exception){
            LOGGER.error(e.message)
        } finally {
            isDebugCommand = false
            if (context != null) {
                try {
                    context.close()
                } catch (e: Exception) {
                    // 处理关闭时的异常
                    LOGGER.error(e.message)
                }
            }
        }
    }

    private fun moveOn(source: ServerCommandSource) {
        val serverTickManager = source.server.tickManager
        serverTickManager.isFrozen = false
        isDebugging = false
        moveSteps = 0
        for (context in storedCommandExecutionContext) {
            try {
                context.run()
                context.close()
            } catch (e: Exception) {
                // 处理关闭时的异常
                LOGGER.error(e.message)
            }
        }
    }
}