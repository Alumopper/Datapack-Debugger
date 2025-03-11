package top.mcfpp.mod.debugger.mixin;

import com.google.common.collect.Queues;
import net.minecraft.command.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.ExpandedMacro;
import net.minecraft.server.function.Procedure;
import net.minecraft.server.function.Tracer;
import net.minecraft.util.Pair;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.mcfpp.mod.debugger.EncapsulationBreaker;
import top.mcfpp.mod.debugger.command.FunctionInAction;
import top.mcfpp.mod.debugger.dap.DebuggerState;

import java.lang.reflect.Field;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import static top.mcfpp.mod.debugger.command.BreakPointCommand.*;

/**
 * Mixin class that extends CommandExecutionContext to add debugging capabilities.
 * This class provides functionality for:
 * - Stepping through command execution
 * - Inspecting variables and NBT data
 * - Managing command queue during debugging
 * - Handling function calls and returns
 *
 * @param <T> The type of command source being used
 */
@Mixin(CommandExecutionContext.class)
abstract public class CommandExecutionContextMixin<T> {

    /** Queue for storing command entries during debugging */
    @Unique
    private final Deque<CommandQueueEntry<T>> storedCommandQueue = Queues.newArrayDeque();

    // Shadowed fields from the original class
    @Shadow @Final private static int MAX_COMMAND_QUEUE_LENGTH;
    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private int maxCommandChainLength;
    @Shadow @Final private int forkLimit;
    @Shadow @Final private Profiler profiler;
    @Shadow private Tracer tracer;
    @Shadow private int commandsRemaining;
    @Shadow private boolean queueOverflowed;
    @Shadow @Final private Deque<CommandQueueEntry<T>> commandQueue;
    @Shadow @Final private List<CommandQueueEntry<T>> pendingCommands;
    @Shadow private int currentDepth;
    @Shadow protected abstract void queuePendingCommands();

    @Shadow private static <T extends AbstractServerCommandSource<T>> Frame frame(CommandExecutionContext<T> context, ReturnValueConsumer returnValueConsumer){return null;}

    /**
     * Injects code to handle procedure calls during debugging.
     * This method ensures proper frame setup for function calls.
     *
     * @param context The command execution context
     * @param procedure The procedure being called
     * @param source The command source
     * @param returnValueConsumer The consumer for return values
     * @param ci The callback info
     */
    @Inject(method = "enqueueProcedureCall", at = @At("HEAD"), cancellable = true)
    private static <T extends AbstractServerCommandSource<T>> void enqueueProcedureCall(
            CommandExecutionContext<T> context, Procedure<T> procedure, T source, ReturnValueConsumer returnValueConsumer, CallbackInfo ci
    ) {
        // Create a new frame for the procedure call
        Frame frame = frame(context, returnValueConsumer);
        try {
            // Use EncapsulationBreaker instead of reflection
            EncapsulationBreaker.getAttribute(frame, "function")
                    .ifPresent(ignored -> EncapsulationBreaker.callFunction(frame, "setFunction", procedure));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Add the command to the queue with the modified frame
        context.enqueueCommand(
                new CommandQueueEntry<>(frame, new CommandFunctionAction<>(procedure, source.getReturnValueConsumer(), false).bind(source))
        );
        ci.cancel();
    }

    /**
     * Injects code to handle command execution during debugging.
     * This method manages the command queue and handles breakpoints.
     *
     * @param ci The callback info
     */
    @Inject(method = "run()V", at = @At("HEAD"), cancellable = true)
    private void onRun(CallbackInfo ci){
        final var THIS = (CommandExecutionContext<T>) (Object) this;

        // Process pending commands before starting the main loop
        this.queuePendingCommands();

        while (true) {
            // Check if we've hit the command execution limit
            if (this.commandsRemaining <= 0) {
                LOGGER.info("Command execution stopped due to limit (executed {} commands)", this.maxCommandChainLength);
                break;
            }

            // Get the next command from the queue
            CommandQueueEntry<T> commandQueueEntry = this.commandQueue.pollFirst();

            if (commandQueueEntry == null) {
                ci.cancel();
                return;
            }

            boolean isDapBreakpoint = processBreakpointForEntry(commandQueueEntry);

            // If we're debugging and the command is inside a function (depth > 0)
            // and we're not stepping through code, pause execution
            if(isDebugging && (commandQueueEntry.frame().depth() != 0 && moveSteps == 0 || isDapBreakpoint)) {
                pauseExecution(commandQueueEntry, THIS);
                ci.cancel();
                return;
            }

            // Update the current depth and execute the command
            executeCommandEntry(commandQueueEntry, THIS);
            
            // Check for queue overflow
            if (this.queueOverflowed) {
                LOGGER.error("Command execution stopped due to command queue overflow (max {})", 10000000);
                break;
            }

            // Process any new commands that were added during execution
            this.queuePendingCommands();
        }

        this.currentDepth = 0;
        ci.cancel();
    }

    /**
     * Handles stepping through command execution.
     * This method manages the command queue and handles breakpoints during step-by-step execution.
     */
    @Unique
    private void onStep(){
        final var THIS = (CommandExecutionContext<T>) (Object) this;

        // Process pending commands before starting the step
        this.queuePendingCommands();

        while (true) {
            // If we are in step over mode, update the stepOverDepth if needed
            updateStepOverDepthIfNeeded();

            // Check command execution limits
            if (this.commandsRemaining <= 0) {
                LOGGER.info("Command execution stopped due to limit (executed {} commands)", this.maxCommandChainLength);
                break;
            }

            // Get the next command from the queue
            CommandQueueEntry<T> commandQueueEntry = this.commandQueue.pollFirst();

            if (commandQueueEntry == null) {
                return;
            }

            processDebuggerStateForEntry(commandQueueEntry);

            // If we're debugging and the command is inside a function
            // and we're not stepping through code, pause execution
            if(isDebugging && commandQueueEntry.frame().depth() != 0 && moveSteps == 0) {
                pauseExecution(commandQueueEntry, THIS);
                resetStepOverIfNeeded();
                return;
            }

            // Update debugging state based on step over conditions
            updateDebuggingState(commandQueueEntry);

            // Execute the command
            executeCommandEntry(commandQueueEntry, THIS);
            
            // Check for queue overflow
            if (this.queueOverflowed) {
                LOGGER.error("Command execution stopped due to command queue overflow (max {})", 10000000);
                break;
            }

            // Process any new commands that were added during execution
            this.queuePendingCommands();
        }

        this.currentDepth = 0;
    }

    /**
     * Updates the debugging state based on step over conditions.
     * 
     * @param commandQueueEntry The current command entry
     */
    @Unique
    private void updateDebuggingState(CommandQueueEntry<T> commandQueueEntry) {
        // When we are in step over, we only activate the debug mode if we are in the same depth level than the one where the step over has been triggered
        // If the debug mode is already activate and the previous condition is false, then we keep the debug mode since this means we find a nested breakpoint
        isDebugging = (isStepOver && this.currentDepth >= commandQueueEntry.frame().depth() && this.currentDepth == stepOverDepth) 
                     || (!isStepOver && isDebugging);
    }

    /**
     * Updates the step over depth if needed.
     */
    @Unique
    private void updateStepOverDepthIfNeeded() {
        if(isStepOver && (stepOverDepth == -1 || isDebugging)) {
            stepOverDepth = this.currentDepth;
        }
    }

    /**
     * Resets step over state if needed.
     */
    @Unique
    private void resetStepOverIfNeeded() {
        if(isStepOver) {
            isStepOver = false;
            stepOverDepth = -1;
        }
    }

    /**
     * Executes a command entry and updates the current depth.
     * 
     * @param commandQueueEntry The command entry to execute
     * @param context The command execution context
     */
    @Unique
    private void executeCommandEntry(CommandQueueEntry<T> commandQueueEntry, CommandExecutionContext<T> context) {
        this.currentDepth = commandQueueEntry.frame().depth();
        commandQueueEntry.execute(context);
    }

    /**
     * Pauses execution by returning the command to the queue and storing the context.
     * 
     * @param commandQueueEntry The command entry to pause
     * @param context The command execution context
     */
    @Unique
    private void pauseExecution(CommandQueueEntry<T> commandQueueEntry, CommandExecutionContext<T> context) {
        // Put the command back in the queue
        commandQueue.addFirst(commandQueueEntry);
        // Store the current context if not already stored
        if(storedCommandExecutionContext.peekFirst() != context) {
            storedCommandExecutionContext.addFirst(context);
        }
    }

    /**
     * Processes breakpoint information for a command entry.
     * 
     * @param commandQueueEntry The command entry to process
     * @return true if a breakpoint was triggered, false otherwise
     */
    @Unique
    private boolean processBreakpointForEntry(CommandQueueEntry<T> commandQueueEntry) {
        if(commandQueueEntry.frame().depth() <= 0) {
            return false;
        }
        
        var function = getExpandedMacroFromFrame(commandQueueEntry.frame());
        if(function == null) {
            return false;
        }
        
        var funcId = function.id();

        if (!(commandQueueEntry.action() instanceof SteppedCommandAction<?, ?> steppedAction)) {
            return false;
        }
        
        var index = (int) EncapsulationBreaker.getAttribute(steppedAction, "nextActionIndex").get();
        if(index < 0) {
            return false;
        }
        
        var lineOpt = (Optional<?>) EncapsulationBreaker.getAttribute(function.entries().get(index), "sourceLine");
        if (!lineOpt.isPresent()) {
            if(function.entries().get(index) instanceof FunctionInAction<?> functionInAction) {
                functionInAction.setCallerContext(DebuggerState.get().getCurrentFile(), DebuggerState.get().getCurrentLine());
            }
            return false;
        }
        
        var line = (int) lineOpt.get();
        boolean isDapBreakpoint = DebuggerState.get().mustStop(funcId.toString(), line);
        DebuggerState.get().setCurrentLine(funcId.toString(), line);
        
        if (isDapBreakpoint) {
            DebuggerState.get().triggerBreakpoint(DebuggerState.get().getServer().getCommandSource());
        }
        
        return isDapBreakpoint;
    }

    /**
     * Processes debugger state information for a command entry.
     * 
     * @param commandQueueEntry The command entry to process
     */
    @Unique
    private void processDebuggerStateForEntry(CommandQueueEntry<T> commandQueueEntry) {
        if(commandQueueEntry.frame().depth() <= 0) {
            return;
        }
        
        var function = getExpandedMacroFromFrame(commandQueueEntry.frame());
        if(function == null) {
            return;
        }
        
        var funcId = function.id();

        if (!(commandQueueEntry.action() instanceof SteppedCommandAction<?, ?> steppedAction)) {
            return;
        }
        
        var index = (int) EncapsulationBreaker.getAttribute(steppedAction, "nextActionIndex").get();
        if(index < 0) {
            return;
        }
        
        var lineOpt = (Optional<?>) EncapsulationBreaker.getAttribute(function.entries().get(index), "sourceLine");
        if (lineOpt.isPresent()) {
            var line = (int) lineOpt.get();
            DebuggerState.get().setCurrentLine(funcId.toString(), line);
            DebuggerState.get().stop("step");
        } else if(function.entries().get(index) instanceof FunctionInAction<?> functionInAction) {
            functionInAction.setCallerContext(DebuggerState.get().getCurrentFile(), DebuggerState.get().getCurrentLine());
        }
    }

    /**
     * Gets the expanded macro from a frame using reflection.
     * 
     * @param frame The frame to get the macro from
     * @return The expanded macro, or null if an error occurred
     */
    @Unique
    private ExpandedMacro<?> getExpandedMacroFromFrame(Frame frame) {
        try {
            return (ExpandedMacro<?>) EncapsulationBreaker.getAttribute(frame, "function").get();
        } catch (Exception e) {
            LOGGER.error("Failed to get expanded macro from frame: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves the expanded macro and its arguments from a command queue entry.
     * 
     * @param commandQueueEntry The command queue entry
     * @return A pair containing the expanded macro and its arguments, or null if not available
     */
    @Unique
    private Pair<ExpandedMacro<T>, NbtCompound> getMacroAndArgsFromEntry(CommandQueueEntry<T> commandQueueEntry) {
        if (commandQueueEntry == null) {
            return null;
        }

        var frame = commandQueueEntry.frame();
        try {
            // Use EncapsulationBreaker instead of reflection
            var function = EncapsulationBreaker.getAttribute(frame, "function")
                    .map(obj -> (ExpandedMacro<T>) obj)
                    .orElse(null);
            
            if (function == null) {
                return null;
            }

            // Get the arguments using EncapsulationBreaker
            var args = EncapsulationBreaker.getAttribute(function, "arguments")
                    .map(obj -> (NbtCompound) obj)
                    .orElse(null);

            return new Pair<>(function, args);
        } catch (Exception e) {
            LOGGER.error("Failed to get macro and args: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves the NBT value for a given key from the current command context.
     * @param key The key to look up
     * @return A pair containing the NBT element and whether it's a macro, or null if not found
     */
    @Unique
    private Pair<NbtElement, Boolean> getKey(String key) {
        CommandQueueEntry<T> commandQueueEntry = this.commandQueue.peekFirst();
        
        var macroAndArgs = getMacroAndArgsFromEntry(commandQueueEntry);
        if (macroAndArgs == null) {
            return null;
        }
        
        var args = macroAndArgs.getRight();
        // Return null if no arguments are available
        if (args == null) {
            return new Pair<>(null, false);
        }
        
        // Return the requested NBT value and indicate it's from a macro
        return new Pair<>(args.get(key), true);
    }

    /**
     * Retrieves all NBT values from the current command context.
     * @return The NBT compound containing all values, or null if not available
     */
    @Unique
    private NbtElement getAllNBT() {
        CommandQueueEntry<T> commandQueueEntry = this.commandQueue.peekFirst();
        
        var macroAndArgs = getMacroAndArgsFromEntry(commandQueueEntry);
        if (macroAndArgs == null) {
            return null;
        }
        
        return macroAndArgs.getRight();
    }

    /**
     * Retrieves all available keys from the current command context.
     * @return A list of all available keys, or null if no NBT data is available
     */
    @Unique
    private List<String> getKeys() {
        CommandQueueEntry<T> commandQueueEntry = this.commandQueue.peekFirst();
        
        var macroAndArgs = getMacroAndArgsFromEntry(commandQueueEntry);
        if (macroAndArgs == null) {
            return null;
        }
        
        var args = macroAndArgs.getRight();
        if (args != null) {
            return args.getKeys().stream().toList();
        } else {
            return null;
        }
    }

    /**
     * Checks if a command action is a fixed command action.
     * @param action The command action to check
     * @return true if the action is a fixed command action, false otherwise
     */
    @Unique
    private boolean isFixCommandAction(@NotNull CommandAction<T> action) {
        // Only check sourced command actions
        if (!(action instanceof SourcedCommandAction)) return false;
        try {
            // Use reflection to check all fields of the action
            Class<?> actionClass = action.getClass();
            Field[] fields = actionClass.getDeclaredFields();

            // Look for a field that contains a FixedCommandAction
            for (Field field : fields) {
                field.setAccessible(true);
                Object fieldValue = field.get(action);
                if (fieldValue instanceof FixedCommandAction) {
                    return true;
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if the current command context contains a command action.
     * @return true if a command action is present, false otherwise
     */
    @Unique
    private boolean ifContainsCommandAction() {
        // Simply check if there are any commands in the queue
        return this.commandQueue.peekFirst() != null;
    }
}