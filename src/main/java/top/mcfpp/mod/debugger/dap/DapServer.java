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

/**
 * A minimal working DAP server implemented using LSP4J.
 * This example handles initialize, launch, configurationDone, threads,
 * stackTrace, and disconnect requests with stubbed implementations.
 */
public class DapServer implements IDebugProtocolServer {


    private IDebugProtocolClient client;

    /**
     * Called during the initialization phase.
     * Sets the supported capabilities (here, supporting configurationDone).
     */
    @Override
    public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {
        Capabilities capabilities = new Capabilities();
        capabilities.setSupportsConfigurationDoneRequest(true);
//        capabilities.setSupportsBreakpointLocationsRequest(true);
        DebuggerState.get().onStop(this::onStop);
        DebuggerState.get().onContinue(this::onContinue);
        return CompletableFuture.completedFuture(capabilities).thenApply(capabilities1 -> {
            client.initialized();
            return capabilities1;
        });
    }

    /**
     * Handles the launch request.
     * In a real adapter, you would start your debuggee process here.
     */
    @Override
    public CompletableFuture<Void> launch(Map<String, Object> args) {
//        System.out.println("Launch request received with arguments: " + args);
        // Minimal example: just acknowledge the launch.
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> attach(Map<String, Object> args) {
//        System.out.println("Attach request received with arguments: " + args);
        DebuggerState.get().getServer().getPlayerManager().getPlayerList().forEach(player -> {
            var header = Text.literal("[Datapack Debugger]").formatted(Formatting.AQUA);
            player.sendMessage(header.append(Text.literal(" Attached to VSCode!").formatted(Formatting.WHITE)));
        });
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments args) {
//        System.out.println(Arrays.toString(args.getBreakpoints()));
        String path = args.getSource().getPath();
        var state = DebuggerState.get();
        state.clearBreakpoints(path);
        List<Breakpoint> breakpoints = new ArrayList<>(args.getBreakpoints().length);
        for(var breakpoint : args.getBreakpoints()) {
            var uuidOpt = state.registerBreakpoint(path, breakpoint.getLine() - 1); // Store the line as 0-indexed
            var dapBreakpoint = new Breakpoint();
            dapBreakpoint.setLine(breakpoint.getLine());
            dapBreakpoint.setVerified(uuidOpt.isPresent());
//            dapBreakpoint.setSource(args.getSource());
            uuidOpt.ifPresent(dapBreakpoint::setId);
            if(uuidOpt.isEmpty()) {
                dapBreakpoint.setReason(BreakpointNotVerifiedReason.FAILED);
            }
            breakpoints.add(dapBreakpoint);
        }
        SetBreakpointsResponse response = new SetBreakpointsResponse();
        response.setBreakpoints(breakpoints.toArray(Breakpoint[]::new));
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<SetInstructionBreakpointsResponse> setInstructionBreakpoints(SetInstructionBreakpointsArguments args) {
//        System.out.println(Arrays.toString(args.getBreakpoints()));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> pause(PauseArguments args) {
//        System.out.println(args.toString());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<SetExceptionBreakpointsResponse> setExceptionBreakpoints(SetExceptionBreakpointsArguments args) {
//        System.out.println(args.toString());
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Handles configurationDone.
     * This method is called when the client signals that configuration is complete.
     */
    @Override
    public CompletableFuture<Void> configurationDone(ConfigurationDoneArguments args) {
//        System.out.println("Configuration done received with arguments: " + args);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Provides a list of threads.
     * Returns a single dummy thread (id=1) for demonstration purposes.
     */
    @Override
    public CompletableFuture<ThreadsResponse> threads() {
        Thread thread = new Thread();
        thread.setId(DebuggerState.get().THREAD_ID);
        thread.setName("Main Thread");
        ThreadsResponse response = new ThreadsResponse();
        response.setThreads(new Thread[]{thread});
//        System.out.println("Threads request received. Returning one thread.");
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Provides a stack trace for a given thread.
     * Returns a dummy stack frame for demonstration.
     */
    @Override
    public CompletableFuture<StackTraceResponse> stackTrace(StackTraceArguments args) {
        var startFrame = args.getStartFrame() != null ? args.getStartFrame() : 0;
        var maxLevels = args.getLevels() != null ? args.getLevels() : 1000;
        var endFrame = startFrame + maxLevels;

        var frames = new ArrayList<StackFrame>(FunctionStackManager.getStack().size());

        endFrame = Math.min(endFrame, FunctionStackManager.getStack().size());

        for(var frame: FunctionStackManager.getStack().subList(startFrame, endFrame)) {
            StackFrame stackFrame = new StackFrame();
            stackFrame.setId(frame.id());
            var index = frame.callLine() + 1; // To 1-indexed
            stackFrame.setName(frame.name());
            stackFrame.setLine(index);
            var realPathOpt = DebuggerState.get().getRealPath(frame.name());
            var source = new Source();
            if(frame.source() instanceof ServerCommandSource serverSource) {
                source.setName(serverSource.getDisplayName().getString());
            }
            realPathOpt.ifPresent(path -> {
                source.setPath(path);
                source.setName(frame.name());
                stackFrame.setSource(source);
            });
            stackFrame.setSource(source);
            frames.add(stackFrame);
        }
        var headLine = DebuggerState.get().getCurrentLine() + 1;
        frames.getFirst().setName(DebuggerState.get().getCurrentFile());
        frames.getFirst().setLine(headLine);

        StackTraceResponse response = new StackTraceResponse();
        response.setStackFrames(frames.toArray(StackFrame[]::new));
        response.setTotalFrames(frames.size());
        System.out.println(response);
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<Void> next(NextArguments args) {
        BreakPointCommand.isStepOver = true;
        BreakPointCommand.step(1, DebuggerState.get().getServer().getCommandSource());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stepIn(StepInArguments args) {
        BreakPointCommand.step(1, DebuggerState.get().getServer().getCommandSource());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ContinueResponse> continue_(ContinueArguments args) {
        BreakPointCommand.continueExec(DebuggerState.get().getServer().getCommandSource());
        var continueResponse = new ContinueResponse();
        return CompletableFuture.completedFuture(continueResponse);
    }

    @Override
    public CompletableFuture<ScopesResponse> scopes(ScopesArguments args) {
        var response = new ScopesResponse();
//        var scope = new Scope();
//        scope.setName("Locals");
//        scope.set
        response.setScopes(new Scope[]{});
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Handles the disconnect request.
     * Performs any necessary cleanup before shutting down.
     */
    @Override
    public CompletableFuture<Void> disconnect(DisconnectArguments args) {
//        System.out.println("Disconnect request received with arguments: " + args);
        DebuggerState.get().getServer().getPlayerManager().getPlayerList().forEach(player -> {
            var header = Text.literal("[Datapack Debugger]").formatted(Formatting.AQUA);
            player.sendMessage(header.append(Text.literal(" Disconnected from VSCode.").formatted(Formatting.WHITE)));
        });
        return CompletableFuture.completedFuture(null);
    }

    public void setClient(IDebugProtocolClient client) {
        this.client = client;
    }

    public void onStop(int breakpointId, String reason) {
        var stoppedEvent = new StoppedEventArguments();
        stoppedEvent.setReason(reason);
        stoppedEvent.setDescription("Breakpoint reached");
        stoppedEvent.setThreadId(DebuggerState.get().THREAD_ID);
        if(breakpointId != -1) {
            stoppedEvent.setHitBreakpointIds(new Integer[]{breakpointId});
        }
        System.out.println(stoppedEvent);
        this.client.stopped(stoppedEvent);
    }

    public void onContinue() {
        var continuedEvent = new ContinuedEventArguments();
        continuedEvent.setThreadId(DebuggerState.get().THREAD_ID);
        this.client.continued(continuedEvent);
    }

    public void exit() {
        var exitedEvent = new ExitedEventArguments();
        exitedEvent.setExitCode(0);
        this.client.exited(exitedEvent);
    }
}
