package top.mcfpp.mod.breakpoint.command;

import com.google.common.collect.Queues;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandExecutionContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager.RegistrationEnvironment;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.spongepowered.asm.launch.MixinBootstrap;
import top.mcfpp.mod.breakpoint.DatapackBreakpoint;
import java.util.Deque;
import java.util.Locale;

public class BreakPointCommand {

    public static boolean isDebugCommand = false;
    public static boolean isDebugging = false;
    public static int moveSteps = 0;
    public static final Deque<CommandExecutionContext<?>> storedCommandExecutionContext = Queues.newArrayDeque();
    private static final org.slf4j.Logger LOGGER = DatapackBreakpoint.getLogger();

    public static void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("breakpoint")
                .requires(source -> source.hasPermissionLevel(2))
            .executes(context -> {
            context.getSource().sendFeedback(() -> Text.literal("已触发断点"), false);
            breakPoint(context.getSource());
            return 1;
        })
            .then(CommandManager.literal("step")
                .executes(context -> {
                    context.getSource().sendFeedback(() -> Text.literal("已触发单步"), false);
                    step(1, context.getSource());
                    return 1;
                })
            )
            .then(CommandManager.literal("move")
            .executes(context -> {
            context.getSource().sendFeedback(() -> Text.literal("已恢复断点"), false);
            moveOn(context.getSource());
            return 1;
        })
            )
            );
        });
    }

    private static void breakPoint(ServerCommandSource source) {
        source.getServer().getTickManager().setFrozen(true);
        isDebugging = true;
    }

    private static void step(int steps, ServerCommandSource source) {
        if (!isDebugging) {
            source.sendError(Text.literal("只能在断点模式下使用step指令"));
            return;
        }
        isDebugCommand = true;
        moveSteps = steps;
        CommandExecutionContext<?> context = null;
        try {
            while (moveSteps > 0) {
                context = storedCommandExecutionContext.peekFirst();
                if (context != null) {
                    LOGGER.info("before mod invokes run()");
                    var cls = context.getClass();
                    var method = cls.getDeclaredMethod("onStep");
                    method.setAccessible(true);
                    method.invoke(context);
                    if (moveSteps != 0) {
                        storedCommandExecutionContext.pollFirst().close();
                    }
                } else {
                    source.sendFeedback(() -> Text.literal("当前刻已执行完毕，退出调试模式"), false);
                    moveOn(source);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        } finally {
            isDebugCommand = false;
            if (context != null) {
                try {
                    context.close();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                }
            }
        }
    }

    private static void moveOn(ServerCommandSource source) {
        source.getServer().getTickManager().setFrozen(false);
        isDebugging = false;
        moveSteps = 0;
        for (CommandExecutionContext<?> context : storedCommandExecutionContext) {
            try {
                context.run();
                context.close();
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
    }
}
