package top.mcfpp.mod.debugger.dap;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import top.mcfpp.mod.debugger.command.BreakPointCommand;
import top.mcfpp.mod.debugger.command.FunctionStackManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A DAP (Debug Adapter Protocol) server implementation using LSP4J.
 * Provides debugging capabilities for Minecraft datapacks.
 */
public class DapServer implements IDebugProtocolServer {

    private static final Logger LOGGER = Logger.getLogger(DapServer.class.getName());
    private static final String MESSAGE_PREFIX = "[Datapack Debugger]";
    
    // Constants for messages
    private static final String ATTACHED_MESSAGE = " Attached to VSCode!";
    private static final String DISCONNECTED_MESSAGE = " Disconnected from VSCode.";
    private static final String BREAKPOINT_DESCRIPTION = "Breakpoint reached";
    private static final String MAIN_THREAD_NAME = "Main Thread";
    
    // Default values for stack trace parameters
    private static final int DEFAULT_START_FRAME = 0;
    private static final int DEFAULT_MAX_LEVELS = 1000;
    private static final int DEFAULT_EXIT_CODE = 0;
    
    private IDebugProtocolClient client;

    /**
     * Creates a new DAP server instance.
     */
    public DapServer() {
        LOGGER.fine("DapServer instance created");
        
        // Register for shutdown notification
        DebuggerState.get().onShutdown(this::exit);
    }

    // ===== Lifecycle Methods =====

    /**
     * Called during the initialization phase.
     * Sets the supported capabilities and registers event handlers.
     */
    @Override
    public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {
        LOGGER.fine(() -> "DEBUG: Initialize request received with arguments: " + args);
        
        Capabilities capabilities = new Capabilities();
        capabilities.setSupportsConfigurationDoneRequest(true);
        // capabilities.setSupportsBreakpointLocationsRequest(true);
        
        // Register event handlers
        DebuggerState.get().onStop(this::onStop);
        DebuggerState.get().onContinue(this::onContinue);
        
        LOGGER.fine(() -> "DEBUG: Sending capabilities response: " + capabilities);
        return CompletableFuture.completedFuture(capabilities).thenApply(capabilities1 -> {
            LOGGER.fine(() -> "DEBUG: Sending initialized event");
            if (client != null) {
                client.initialized();
            } else {
                LOGGER.warning("Client is null during initialize, couldn't send initialized event");
            }
            return capabilities1;
        });
    }

    /**
     * Handles the launch request.
     * In a real adapter, you would start your debuggee process here.
     */
    @Override
    public CompletableFuture<Void> launch(Map<String, Object> args) {
        LOGGER.fine(() -> "DEBUG: Launch request received with arguments: " + args);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Handles the attach request.
     * Notifies Minecraft players that a debugging session has started.
     */
    @Override
    public CompletableFuture<Void> attach(Map<String, Object> args) {
        LOGGER.fine(() -> "DEBUG: Attach request received with arguments: " + args);
        
        sendMessageToAllPlayers(ATTACHED_MESSAGE);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Handles the disconnect request.
     * Notifies Minecraft players that the debugging session has ended.
     */
    @Override
    public CompletableFuture<Void> disconnect(DisconnectArguments args) {
        LOGGER.fine(() -> "DEBUG: Disconnect request received with arguments: " + args);
        
        sendMessageToAllPlayers(DISCONNECTED_MESSAGE);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Handles configurationDone request.
     * Called when the client signals that configuration is complete.
     */
    @Override
    public CompletableFuture<Void> configurationDone(ConfigurationDoneArguments args) {
        LOGGER.fine(() -> "DEBUG: ConfigurationDone request received with arguments: " + args);
        return CompletableFuture.completedFuture(null);
    }

    // ===== Breakpoint Methods =====

    /**
     * Handles setting breakpoints.
     * Maps VS Code breakpoints to Minecraft function locations.
     */
    @Override
    public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments args) {
        LOGGER.fine(() -> "DEBUG: SetBreakpoints request received with arguments: " + args);
        
        if (args == null || args.getSource() == null || args.getSource().getPath() == null) {
            LOGGER.warning("Received invalid SetBreakpoints request with null arguments");
            return CompletableFuture.completedFuture(new SetBreakpointsResponse());
        }
        
        String path = args.getSource().getPath();
        var state = DebuggerState.get();
        state.clearBreakpoints(path);
        
        List<Breakpoint> breakpoints = new ArrayList<>(args.getBreakpoints().length);
        for(SourceBreakpoint sourceBreakpoint : args.getBreakpoints()) {
            breakpoints.add(createBreakpoint(path, sourceBreakpoint, state));
        }
        
        SetBreakpointsResponse response = new SetBreakpointsResponse();
        response.setBreakpoints(breakpoints.toArray(Breakpoint[]::new));
        
        LOGGER.fine(() -> "DEBUG: Sending SetBreakpoints response: " + response);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Creates a DAP Breakpoint from a source breakpoint.
     */
    private Breakpoint createBreakpoint(String path, SourceBreakpoint sourceBreakpoint, DebuggerState state) {
        // Store the line as 0-indexed (convert from 1-indexed)
        var uuidOpt = state.registerBreakpoint(path, sourceBreakpoint.getLine() - 1);
        
        var dapBreakpoint = new Breakpoint();
        dapBreakpoint.setLine(sourceBreakpoint.getLine());
        dapBreakpoint.setVerified(uuidOpt.isPresent());
        uuidOpt.ifPresent(dapBreakpoint::setId);
        
        if(uuidOpt.isEmpty()) {
            dapBreakpoint.setReason(BreakpointNotVerifiedReason.FAILED);
        }
        
        return dapBreakpoint;
    }

    /**
     * Handles setting instruction breakpoints.
     * Not currently implemented.
     */
    @Override
    public CompletableFuture<SetInstructionBreakpointsResponse> setInstructionBreakpoints(SetInstructionBreakpointsArguments args) {
        LOGGER.fine(() -> "DEBUG: SetInstructionBreakpoints request received with arguments: " + args);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Handles setting exception breakpoints.
     * Not currently implemented.
     */
    @Override
    public CompletableFuture<SetExceptionBreakpointsResponse> setExceptionBreakpoints(SetExceptionBreakpointsArguments args) {
        LOGGER.fine(() -> "DEBUG: SetExceptionBreakpoints request received with arguments: " + args);
        return CompletableFuture.completedFuture(null);
    }

    // ===== Execution Control Methods =====

    /**
     * Handles step over (next) execution command.
     */
    @Override
    public CompletableFuture<Void> next(NextArguments args) {
        LOGGER.fine(() -> "DEBUG: Next request received with arguments: " + args);
        
        try {
            BreakPointCommand.isStepOver = true;
            BreakPointCommand.step(1, getCommandSource());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during step over execution", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Handles step into execution command.
     */
    @Override
    public CompletableFuture<Void> stepIn(StepInArguments args) {
        LOGGER.fine(() -> "DEBUG: StepIn request received with arguments: " + args);
        
        try {
            BreakPointCommand.step(1, getCommandSource());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during step in execution", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Handles continue execution command.
     */
    @Override
    public CompletableFuture<ContinueResponse> continue_(ContinueArguments args) {
        LOGGER.fine(() -> "DEBUG: Continue request received with arguments: " + args);
        
        try {
            BreakPointCommand.continueExec(getCommandSource());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during continue execution", e);
        }
        
        var continueResponse = new ContinueResponse();
        
        LOGGER.fine(() -> "DEBUG: Sending Continue response: " + continueResponse);
        return CompletableFuture.completedFuture(continueResponse);
    }

    /**
     * Handles the pause execution command.
     * Not currently implemented.
     */
    @Override
    public CompletableFuture<Void> pause(PauseArguments args) {
        LOGGER.fine(() -> "DEBUG: Pause request received with arguments: " + args);
        return CompletableFuture.completedFuture(null);
    }

    // ===== Inspection Methods =====

    /**
     * Provides a list of threads.
     * Returns a single thread for the datapack execution.
     */
    @Override
    public CompletableFuture<ThreadsResponse> threads() {
        LOGGER.fine(() -> "DEBUG: Threads request received");
        
        Thread thread = new Thread();
        thread.setId(DebuggerState.get().THREAD_ID);
        thread.setName(MAIN_THREAD_NAME);
        
        ThreadsResponse response = new ThreadsResponse();
        response.setThreads(new Thread[]{thread});
        
        LOGGER.fine(() -> "DEBUG: Sending Threads response: " + response);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Provides a stack trace for a given thread.
     * Maps Minecraft function call stack to DAP stack frames.
     */
    @Override
    public CompletableFuture<StackTraceResponse> stackTrace(StackTraceArguments args) {
        LOGGER.fine(() -> "DEBUG: StackTrace request received with arguments: " + args);
        
        var startFrame = args.getStartFrame() != null ? args.getStartFrame() : DEFAULT_START_FRAME;
        var maxLevels = args.getLevels() != null ? args.getLevels() : DEFAULT_MAX_LEVELS;
        var endFrame = Math.min(startFrame + maxLevels, FunctionStackManager.getStack().size());

        var frames = buildStackFrames(startFrame, endFrame);
        updateHeadFrame(frames);

        StackTraceResponse response = new StackTraceResponse();
        response.setStackFrames(frames.toArray(StackFrame[]::new));
        response.setTotalFrames(frames.size());
        
        LOGGER.fine(() -> "DEBUG: Sending StackTrace response: " + response);
        return CompletableFuture.completedFuture(response);
    }
    
    /**
     * Builds stack frames for the specified range of frames.
     */
    private List<StackFrame> buildStackFrames(int startFrame, int endFrame) {
        var frames = new ArrayList<StackFrame>(endFrame - startFrame);
        
        for(var frame: FunctionStackManager.getStack().subList(startFrame, endFrame)) {
            StackFrame stackFrame = createStackFrame(frame);
            frames.add(stackFrame);
        }
        
        return frames;
    }
    
    /**
     * Creates a DAP StackFrame from a function stack frame.
     */
    private StackFrame createStackFrame(FunctionStackManager.DebugFrame frame) {
        StackFrame stackFrame = new StackFrame();
        stackFrame.setId(frame.id());
        
        // Convert to 1-indexed
        var index = frame.callLine() + 1;
        stackFrame.setName(frame.name());
        stackFrame.setLine(index);
        
        var source = createSource(frame);
        stackFrame.setSource(source);
        
        return stackFrame;
    }
    
    /**
     * Creates a DAP Source for a function stack frame.
     */
    private Source createSource(FunctionStackManager.DebugFrame frame) {
        var source = new Source();
        
        if(frame.source() instanceof ServerCommandSource serverSource) {
            source.setName(serverSource.getDisplayName().getString());
        }
        
        DebuggerState.get().getRealPath(frame.name()).ifPresent(path -> {
            source.setPath(path);
            source.setName(frame.name());
        });
        
        return source;
    }
    
    /**
     * Updates the head frame with current file and line information.
     */
    private void updateHeadFrame(List<StackFrame> frames) {
        if (!frames.isEmpty()) {
            var headLine = DebuggerState.get().getCurrentLine() + 1;
            frames.getFirst().setName(DebuggerState.get().getCurrentFile());
            frames.getFirst().setLine(headLine);
        }
    }

    /**
     * Provides variable scopes for a given stack frame.
     * Currently returns empty scopes.
     */
    @Override
    public CompletableFuture<ScopesResponse> scopes(ScopesArguments args) {
        LOGGER.fine(() -> "DEBUG: Scopes request received with arguments: " + args);
        
        var response = new ScopesResponse();
        // var scope = new Scope();
        // scope.setName("Locals");
        response.setScopes(new Scope[]{});
        
        LOGGER.fine(() -> "DEBUG: Sending Scopes response: " + response);
        return CompletableFuture.completedFuture(response);
    }

    // ===== Event Handlers =====

    /**
     * Sets the DAP client for callback communication.
     *
     * @param client The client to send events to
     * @throws IllegalArgumentException if client is null
     */
    public void setClient(IDebugProtocolClient client) {
        LOGGER.fine(() -> "DEBUG: Setting client: " + client);
        
        if (client == null) {
            LOGGER.warning("Attempt to set null client");
            throw new IllegalArgumentException("Client cannot be null");
        }
        
        this.client = client;
    }

    /**
     * Callback handler for when execution stops at a breakpoint.
     * Notifies the connected DAP client.
     */
    public void onStop(int breakpointId, String reason) {
        LOGGER.fine(() -> "DEBUG: onStop called with breakpointId: " + breakpointId + ", reason: " + reason);
        
        if (client == null) {
            LOGGER.warning("Cannot send stopped event: client is null");
            return;
        }
        
        var stoppedEvent = new StoppedEventArguments();
        stoppedEvent.setReason(reason);
        stoppedEvent.setDescription(BREAKPOINT_DESCRIPTION);
        stoppedEvent.setThreadId(DebuggerState.get().THREAD_ID);
        
        if(breakpointId != -1) {
            stoppedEvent.setHitBreakpointIds(new Integer[]{breakpointId});
        }
        
        LOGGER.fine(() -> "DEBUG: Sending stopped event: " + stoppedEvent);
        this.client.stopped(stoppedEvent);
    }

    /**
     * Callback handler for when execution continues.
     * Notifies the connected DAP client.
     */
    public void onContinue() {
        LOGGER.fine(() -> "DEBUG: onContinue called");
        
        if (client == null) {
            LOGGER.warning("Cannot send continued event: client is null");
            return;
        }
        
        var continuedEvent = new ContinuedEventArguments();
        continuedEvent.setThreadId(DebuggerState.get().THREAD_ID);
        
        LOGGER.fine(() -> "DEBUG: Sending continued event: " + continuedEvent);
        this.client.continued(continuedEvent);
    }

    /**
     * Notifies the client that the debugging session has ended.
     */
    public void exit() {
        LOGGER.fine(() -> "DEBUG: exit called");
        
        if (client == null) {
            LOGGER.warning("Cannot send exited event: client is null");
            return;
        }
        
        try {
            // Send end events to the client
            sendTerminatedEvent();
            sendExitedEvent();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error while sending exit events", e);
        }
    }
    
    /**
     * Sends a 'terminated' event to the client.
     * This event indicates that the debug session has ended.
     */
    private void sendTerminatedEvent() {
        try {
            TerminatedEventArguments terminatedEvent = new TerminatedEventArguments();
            LOGGER.fine(() -> "DEBUG: Sending terminated event: " + terminatedEvent);
            client.terminated(terminatedEvent);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send terminated event", e);
        }
    }
    
    /**
     * Sends an 'exited' event to the client.
     * This event indicates that the debugged process has stopped.
     */
    private void sendExitedEvent() {
        try {
            var exitedEvent = new ExitedEventArguments();
            exitedEvent.setExitCode(DEFAULT_EXIT_CODE);
            
            LOGGER.fine(() -> "DEBUG: Sending exited event: " + exitedEvent);
            client.exited(exitedEvent);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send exited event", e);
        }
    }
    
    // ===== Helper Methods =====
    
    /**
     * Sends a message to all players on the server.
     */
    private void sendMessageToAllPlayers(String message) {
        try {
            DebuggerState.get().getServer().getPlayerManager().getPlayerList().forEach(player -> {
                var header = Text.literal(MESSAGE_PREFIX).formatted(Formatting.AQUA);
                player.sendMessage(header.append(Text.literal(message).formatted(Formatting.WHITE)));
            });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error sending message to players", e);
        }
    }
    
    /**
     * Gets the command source from the server.
     */
    private ServerCommandSource getCommandSource() {
        return DebuggerState.get().getServer().getCommandSource();
    }
}
