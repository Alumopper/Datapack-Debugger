package net.gunivers.sniffer.command;

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
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.gunivers.sniffer.DatapackDebugger;
import net.gunivers.sniffer.dap.DebuggerState;
import net.gunivers.sniffer.dap.ScopeManager;

import java.util.Deque;

/**
 * Main command handler for the datapack debugging system.
 * Provides commands for setting breakpoints, stepping through code, and inspecting variables.
 *
 * @author Alumopper
 * @author theogiraudet
 */
public class BreakPointCommand {

    /** Indicates if a debug command is currently being executed */
    public static boolean isDebugCommand = false;
    /** Indicates if the debugger is currently active */
    public static boolean isDebugging = false;
    /** Controls whether debug mode is enabled */
    public static boolean debugMode = true;
    /** 
     * Number of steps to execute in step mode.
     * When this value is greater than 0, the debugger will continue execution
     * for that many steps before pausing again. This is decremented
     * as each command is executed.
     */
    public static int moveSteps = 0;
    /** 
     * The current stepping mode for the debugger.
     * Controls how the debugger behaves when stepping through code:
     * - STEP_IN: Steps into function calls
     * - STEP_OVER: Executes function calls as a single step
     * - STEP_OUT: Continues execution until returning from the current function
     * @see StepType
     */
    public static StepType stepType = StepType.STEP_IN;
    /** Queue storing command execution contexts for debugging */
    public static final Deque<CommandExecutionContext<?>> storedCommandExecutionContext = Queues.newArrayDeque();
    /** Logger instance for this class */
    private static final org.slf4j.Logger LOGGER = DatapackDebugger.getLogger();

    /**
     * Tracks the depth level at which a step operation was initiated.
     * Used by stepOver and stepOut operations to determine when to stop execution.
     * A value of -1 indicates that no depth tracking is active.
     */
    public static int stepDepth = -1;

    /**
     * Initializes the breakpoint command system.
     * Registers all subcommands including:
     * - breakpoint: Sets a breakpoint
     * - continue: Continues execution
     * - step: Steps through code
     * - get: Retrieves variable values
     * - stack: Shows function call stack
     * - run: Executes commands
     * - clear: Clears debug state
     * - on/off: Toggles debug mode
     */
    public static void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("breakpoint")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        if(!debugMode) return 1;
                        var server = context.getSource().getServer();
                        if(server != null){
                            var players = server.getPlayerManager().getPlayerList();
                            for (var player : players){
                                player.sendMessage(Text.translatable("sniffer.commands.breakpoint.set"));
                            }
                        }
                        DebuggerState.get().triggerBreakpoint(context.getSource());
                        return 1;
                    })
                    .then(literal("step")
                            .executes(context -> {
                                stepType = StepType.STEP_IN;
                                step(1, context.getSource());
                                return 1;
                            })
                            .then(argument("lines", IntegerArgumentType.integer())
                                    .executes(context -> {
                                        final int lines = IntegerArgumentType.getInteger(context, "lines");
                                        stepType = StepType.STEP_IN;
                                        step(lines, context.getSource());
                                        return 1;
                                    })
                            )
                    )
                    .then(literal("step_over")
                            .executes(context -> {
                                stepType = StepType.STEP_OVER;
                                step(1, context.getSource());
                                return 1;
                            })
                            .then(argument("lines", IntegerArgumentType.integer())
                                    .executes(context -> {
                                        stepType = StepType.STEP_OVER;
                                        final int lines = IntegerArgumentType.getInteger(context, "lines");
                                        step(lines, context.getSource());
                                        return 1;
                                    })
                            )
                    )
                    .then(literal("step_out")
                            .executes(context -> {
                                stepType = StepType.STEP_OUT;
                                step(1, context.getSource());
                                return 1;
                            })
                    )
                    .then(literal("continue")
                            .executes(context -> {
                                context.getSource().sendFeedback(() -> Text.translatable("sniffer.commands.breakpoint.move"), false);
                                continueExec(context.getSource());
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
                                                context.getSource().sendFeedback(() -> Text.translatable("sniffer.commands.breakpoint.get", key, NbtHelper.toPrettyPrintedText(nbt.getLeft())), false);
                                            }else {
                                                context.getSource().sendError(Text.translatable("sniffer.commands.breakpoint.get.fail.not_macro"));
                                            }
                                        }
                                        return 1;
                                    })
                            )
                            .executes(context -> {
                                final var args = getAllNBT(context.getSource());
                                if(args == null){
                                    context.getSource().sendError(Text.translatable("sniffer.commands.breakpoint.get.fail.not_macro"));
                                }else {
                                    context.getSource().sendFeedback(() -> (NbtHelper.toPrettyPrintedText(args)), false);
                                }
                                return 1;
                            })
                    )
                    .then(literal("stack")
                            .executes(context -> {
                                MutableText text = Text.empty();
                                var stacks = ScopeManager.get().getDebugScopes();
                                for (var stack : stacks) {
                                    var t = Text.literal(stack.getFunction());
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
                            .redirect(dispatcher.getRoot(), context -> (ServerCommandSource) ScopeManager.get().getCurrentScope().map(ScopeManager.DebugScope::getExecutor).orElse(null))
                    )
                    .then(literal("clear")
                            .executes(context -> {
                                clear();
                                return 1;
                            })
                    )
                    .then(literal("on")
                            .executes(context -> {
                                context.getSource().sendFeedback(() -> Text.translatable("sniffer.commands.breakpoint.on"), false);
                                debugMode = true;
                                return 1;
                            })
                    )
                    .then(literal("off")
                            .executes(context -> {
                                context.getSource().sendFeedback(() -> Text.translatable("sniffer.commands.breakpoint.off"), false);
                                debugMode = false;
                                return 1;
                            })
                    )
            );
        });
    }

    /**
     * Clears all debugging state and resets the system.
     * This includes clearing breakpoints, command contexts, and function stacks.
     */
    public static void clear(){
        isDebugCommand = false;
        isDebugging = false;
        debugMode = true;
        moveSteps = 0;
        stepType = StepType.STEP_IN;
        stepDepth = -1;
        storedCommandExecutionContext.clear();
    }

    /**
     * Steps through the code execution for a specified number of steps.
     * @param steps Number of steps to execute
     * @param source The command source that triggered the step
     */
    public static void step(int steps, ServerCommandSource source) {
        if (!isDebugging) {
            source.sendError(Text.translatable("sniffer.commands.breakpoint.step.fail"));
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
                        boolean result = (boolean) method1.invoke(context);
                        if(!result){
                            storedCommandExecutionContext.pollFirst().close();
                        }
                        break;
                    }
                } else {
                    source.sendFeedback(() -> Text.translatable("sniffer.commands.breakpoint.step.over"), false);
                    continueExec(source);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
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

    /**
     * Continues execution from the current breakpoint.
     * @param source The command source that triggered the continue
     */
    public static void continueExec(@NotNull ServerCommandSource source) {
        if(!isDebugging){
            source.sendError(Text.translatable("sniffer.commands.breakpoint.move.not_debugging"));
            return;
        }
        source.getServer().getTickManager().setFrozen(false);
        DebuggerState.get().continueExec();
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

    /**
     * Retrieves the NBT value for a given key from the current context.
     * @param key The key to look up
     * @param source The command source requesting the value
     * @return A pair containing the NBT element and whether it's a macro, or null if not found
     */
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
            source.sendError(Text.translatable("sniffer.commands.breakpoint.get.fail.error", e.toString()));
            return null;
        }
    }

    /**
     * Retrieves all NBT values from the current context.
     * @param source The command source requesting the values
     * @return The NBT element containing all values, or null if not available
     */
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
            source.sendError(Text.translatable("sniffer.commands.breakpoint.get.fail.error", e.toString()));
            return null;
        }
    }
}
