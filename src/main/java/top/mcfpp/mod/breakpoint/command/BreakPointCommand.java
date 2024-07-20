package top.mcfpp.mod.breakpoint.command;

import com.google.common.collect.Queues;
import net.minecraft.command.argument.*;
import com.mojang.brigadier.arguments.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandExecutionContext;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mcfpp.mod.breakpoint.DatapackBreakpoint;

import java.util.Deque;

public class BreakPointCommand {

    public static boolean isDebugCommand = false;
    public static boolean isDebugging = false;
    public static int moveSteps = 0;
    public static final Deque<CommandExecutionContext<?>> storedCommandExecutionContext = Queues.newArrayDeque();
    private static final org.slf4j.Logger LOGGER = DatapackBreakpoint.getLogger();

    public static void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("breakpoint")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        context.getSource().sendFeedback(() -> Text.literal("已触发断点"), false);
                        breakPoint(context.getSource());
                        return 1;
                    })
                    .then(literal("step")
                            .executes(context -> {
                                step(1, context.getSource());
                                return 1;
                            })
                            .then(argument("lines", IntegerArgumentType.integer())
                                    .executes(context -> {
                                        final int lines = IntegerArgumentType.getInteger(context, "lines");
                                        step(lines, context.getSource());
                                        return 1;
                                    })
                            )
                    )
                    .then(literal("move")
                            .executes(context -> {
                                context.getSource().sendFeedback(() -> Text.literal("已恢复断点"), false);
                                moveOn(context.getSource());
                                return 1;
                            })
                    )
                    .then(literal("get")
                            .then(argument("key", StringArgumentType.string())
                                    .executes(context -> {
                                        final String key = StringArgumentType.getString(context, "key");
                                        NbtElement nbt = getNBT(key);
                                        if(nbt == null){
                                            context.getSource().sendError(Text.literal("无法在当前上下文获取" + key + "的值"));
                                        }else {
                                            context.getSource().sendFeedback(() -> Text.literal(key + "的值是：").append(NbtHelper.toPrettyPrintedText(nbt)), false);
                                        }
                                        return 1;
                                    })
                            )
                    )
            );
        });
    }

    private static void breakPoint(@NotNull ServerCommandSource source) {
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
                    LOGGER.error(e.toString());
                }
            }
        }
    }

    private static void moveOn(@NotNull ServerCommandSource source) {
        source.getServer().getTickManager().setFrozen(false);
        isDebugging = false;
        moveSteps = 0;
        for (CommandExecutionContext<?> context : storedCommandExecutionContext) {
            try {
                context.run();
                context.close();
            } catch (Exception e) {
                LOGGER.error(e.toString());
            }
        }
    }

    private static @Nullable NbtElement getNBT(String key){
        var context = storedCommandExecutionContext.peekFirst();
        if(context == null){
            return null;
        }
        try{
            var cls = context.getClass();
            var method = cls.getDeclaredMethod("getKey", String.class);
            method.setAccessible(true);
            return (NbtElement) method.invoke(context, key);
        }catch (Exception e){
            LOGGER.error(e.toString());
            return null;
        }

    }
}
