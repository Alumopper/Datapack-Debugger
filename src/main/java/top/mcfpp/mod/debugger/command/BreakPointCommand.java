package top.mcfpp.mod.debugger.command;

import com.google.common.collect.Queues;
import com.mojang.brigadier.arguments.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandExecutionContext;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mcfpp.mod.debugger.DatapackDebugger;

import java.util.Deque;
import java.util.Objects;

public class BreakPointCommand {

    public static boolean isDebugCommand = false;
    public static boolean isDebugging = false;
    public static int moveSteps = 0;
    public static final Deque<CommandExecutionContext<?>> storedCommandExecutionContext = Queues.newArrayDeque();
    private static final org.slf4j.Logger LOGGER = DatapackDebugger.getLogger();

    public static void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("breakpoint")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        context.getSource().sendFeedback(() -> Text.translatable("commands.breakpoint.set"), false);
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
                                context.getSource().sendFeedback(() -> Text.translatable("commands.breakpoint.move"), false);
                                moveOn(context.getSource());
                                return 1;
                            })
                    )
                    .then(literal("get")
                            .then(argument("key", StringArgumentType.string())
                                    .suggests(BreakpointSuggestionProvider.INSTANCE)
                                    .executes(context -> {
                                        final String key = StringArgumentType.getString(context, "key");
                                        var nbt = getNBT(key, context.getSource());
                                        if(nbt != null){
                                            if(nbt.getRight()){
                                                context.getSource().sendFeedback(() -> Text.translatable("commands.breakpoint.get", key, NbtHelper.toPrettyPrintedText(nbt.getLeft())), false);
                                            }else {
                                                context.getSource().sendError(Text.translatable("commands.breakpoint.get.fail.not_macro"));
                                            }
                                        }
                                        return 1;
                                    })
                            )
                            .executes(context -> {
                                final var args = getAllNBT(context.getSource());
                                if(args == null){
                                    context.getSource().sendError(Text.translatable("commands.breakpoint.get.fail.not_macro"));
                                }else {
                                    context.getSource().sendFeedback(() -> (NbtHelper.toPrettyPrintedText(args)), false);
                                }
                                return 1;
                            })
                    )
                    .then(literal("stack")
                            .executes(context -> {
                                MutableText text = Text.empty();
                                var stacks = FunctionStackManager.getStack();
                                for (String stack : stacks) {
                                    var t = Text.literal(stack);
                                    var style = t.getStyle();
                                    if(stacks.indexOf(stack) == 0){
                                        style = style.withBold(true);
                                    }else {
                                        style = style.withBold(false);
                                    }
                                    t.setStyle(style);
                                    text = text.append(t);
                                    text.append("\n");
                                }
                                final MutableText finalText = text;
                                context.getSource().sendFeedback(() -> finalText, false);
                                return 1;
                            })
                    )
                    .then(literal("run")
                            .redirect(dispatcher.getRoot(), context -> (ServerCommandSource) FunctionStackManager.source.peek())
                    )
                    .then(literal("clear")
                            .executes(context -> {
                                FunctionStackManager.functionStack.clear();
                                FunctionStackManager.source.clear();
                                return 1;
                            })
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
            source.sendError(Text.translatable("commands.breakpoint.step.fail"));
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
                    }else {
                        var method1 = cls.getDeclaredMethod("ifContainsCommandAction");
                        method1.setAccessible(true);
                        boolean result =  (boolean) method1.invoke(context);
                        if(!result){
                            storedCommandExecutionContext.pollFirst().close();
                        }
                        break;
                    }
                } else {
                    source.sendFeedback(() -> Text.translatable("commands.breakpoint.step.over"), false);
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
        if(!isDebugging){
            source.sendError(Text.translatable("commands.breakpoint.move.not_debugging"));
            return;
        }
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

    private static @Nullable Pair<NbtElement, Boolean> getNBT(String key, ServerCommandSource source){
        var context = storedCommandExecutionContext.peekFirst();
        if(context == null){
            return null;
        }
        try {
            var cls = context.getClass();
            var method = cls.getDeclaredMethod("getKey", String.class);
            method.setAccessible(true);
            return (Pair<NbtElement, Boolean>) method.invoke(context, key);
        }catch (Exception e){
            LOGGER.error(e.toString());
            source.sendError(Text.translatable("commands.breakpoint.get.fail.error", e.toString()));
            return null;
        }
    }

    private static @Nullable NbtElement getAllNBT(ServerCommandSource source){
        var context = storedCommandExecutionContext.peekFirst();
        if(context == null){
            return null;
        }
        try {
            var cls = context.getClass();
            var method = cls.getDeclaredMethod("getAllNBT");
            method.setAccessible(true);
            return (NbtElement) method.invoke(context);
        }catch (Exception e){
            LOGGER.error(e.toString());
            source.sendError(Text.translatable("commands.breakpoint.get.fail.error", e.toString()));
            return null;
        }
    }
}
