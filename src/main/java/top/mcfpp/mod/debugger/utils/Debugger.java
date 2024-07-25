package top.mcfpp.mod.debugger.utils;


import com.google.common.collect.Queues;
import net.minecraft.command.CommandExecutionContext;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mcfpp.mod.debugger.DatapackDebugger;
import top.mcfpp.mod.debugger.command.FunctionStackManager;

import java.util.Deque;
import java.util.Objects;

public class Debugger {
    public static boolean isDebugCommand = false;
    public static boolean isDebugging = false;
    public static int moveSteps = 0;
    public static final Deque<CommandExecutionContext<?>> storedCommandExecutionContext = Queues.newArrayDeque();
    private static final org.slf4j.Logger LOGGER = DatapackDebugger.getLogger();

    private static void sendError(@NotNull CommandOutput output, @NotNull Text text) {
        output.sendMessage(text.copy().formatted(Formatting.RED));
    }

    private static void sendFeedback(@NotNull CommandOutput output, @NotNull Text text) {
        output.sendMessage(text);
    }

    public static void breakPoint(@NotNull CommandOutput output, @NotNull MinecraftServer server) {
        server.getTickManager().setFrozen(true);
        isDebugging = true;
        sendFeedback(output, Text.translatable("commands.breakpoint.set"));
    }

    public static void step(int steps, @NotNull CommandOutput output, @NotNull MinecraftServer server) {
        if (!isDebugging) {
            breakPoint(output, server);
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
                    } else {
                        var method1 = cls.getDeclaredMethod("ifContainsCommandAction");
                        method1.setAccessible(true);
                        boolean result = (boolean) method1.invoke(context);
                        if (!result) {
                            storedCommandExecutionContext.pollFirst().close();
                        }
                        break;
                    }
                } else {
                    sendFeedback(output, Text.translatable("commands.breakpoint.step.over"));
                    moveOn(output, server);
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

    public static void moveOn(@NotNull CommandOutput output, @NotNull MinecraftServer server) {
        if (!isDebugging) {
            sendError(output, Text.translatable("commands.breakpoint.move.not_debugging"));
            return;
        }
        server.getTickManager().setFrozen(false);
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

    public static @Nullable Pair<NbtElement, Boolean> getNBT(String key, @NotNull CommandOutput output, @NotNull MinecraftServer server) {
        var context = storedCommandExecutionContext.peekFirst();
        if (context == null) {
            return null;
        }
        try {
            var cls = context.getClass();
            var method = cls.getDeclaredMethod("getKey", String.class);
            method.setAccessible(true);
            return (Pair<NbtElement, Boolean>) method.invoke(context, key);
        } catch (Exception e) {
            LOGGER.error(e.toString());
            sendError(output, Text.translatable("commands.breakpoint.get.fail.error", e.toString()));
            return null;
        }
    }

    public static @Nullable NbtElement getAllNBT(@NotNull CommandOutput output, @NotNull MinecraftServer server) {
        var context = storedCommandExecutionContext.peekFirst();
        if (context == null) {
            return null;
        }
        try {
            var cls = context.getClass();
            var method = cls.getDeclaredMethod("getAllNBT");
            method.setAccessible(true);
            return (NbtElement) method.invoke(context);
        } catch (Exception e) {
            LOGGER.error(e.toString());
            sendError(output, Text.translatable("commands.breakpoint.get.fail.error", e.toString()));
            return null;
        }
    }

    public static void printStack(@NotNull CommandOutput output, @NotNull MinecraftServer server) {
        var stacks = FunctionStackManager.getStack();
        for (String stack : stacks) {
            var t = Text.literal(stack);
            var style = t.getStyle();
            if (stacks.indexOf(stack) == 0) {
                style = style.withBold(true);
            } else {
                style = style.withBold(false);
            }
            t.setStyle(style);
            sendFeedback(output, t);
        }
    }

    public static void breakPoint(@NotNull ServerCommandSource source) {
        breakPoint(Objects.requireNonNull(source.getPlayer()), source.getServer());
    }

    public static void step(int i, @NotNull ServerCommandSource source) {
        step(i, Objects.requireNonNull(source.getPlayer()), source.getServer());
    }

    public static @Nullable NbtElement getAllNBT(@NotNull ServerCommandSource source) {
        return getAllNBT(Objects.requireNonNull(source.getPlayer()), source.getServer());
    }

    public static @Nullable Pair<NbtElement, Boolean> getNBT(String key, @NotNull ServerCommandSource source) {
        return getNBT(key, Objects.requireNonNull(source.getPlayer()), source.getServer());
    }

    public static void moveOn(@NotNull ServerCommandSource source) {
        moveOn(Objects.requireNonNull(source.getPlayer()), source.getServer());
    }

    public static void printStack(@NotNull ServerCommandSource source) {
        printStack(Objects.requireNonNull(source.getPlayer()), source.getServer());
    }
}