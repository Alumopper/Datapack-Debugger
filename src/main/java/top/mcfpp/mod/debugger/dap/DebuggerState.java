package top.mcfpp.mod.debugger.dap;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import top.mcfpp.mod.debugger.command.FunctionPathGetter;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static top.mcfpp.mod.debugger.command.BreakPointCommand.isDebugging;

public class DebuggerState {

    private static DebuggerState singleton;
    private final static Pattern PATH_PATTERN = Pattern.compile("data/(?<namespace>\\w+)/function/(?<path>.+)\\.mcfunction");

    private DebuggerState() {}

    public static DebuggerState get() {
        if(singleton == null) {
            singleton = new DebuggerState();
        }
        return singleton;
    }

    private record Breakpoint(int id, int line) {}

    private static class FunctionBreakpoints {
        protected String functionPath;
        protected String functionMcPath;
        // breakpoint line → Breakpoint
        protected Map<Integer, Breakpoint> breakpoints = new HashMap<>();

        public FunctionBreakpoints(String functionMcPath, String functionPath) {
            this.functionMcPath = functionMcPath;
            this.functionPath = functionPath;
        }
    }


    private class AllBreakpoints {
        private final Map<String, FunctionBreakpoints> breakpoints = new HashMap<>();

        public Optional<Integer> addBreakpoint(String filePath, int line) {
            Optional<String> mcpathOpt = fileToMcPath(filePath);
            if(mcpathOpt.isPresent()) {
                String mcpath = mcpathOpt.get();
                var funBreakpoints = breakpoints.getOrDefault(mcpath, new FunctionBreakpoints(mcpath, filePath));
                breakpoints.put(mcpath, funBreakpoints);
                int uuid = breakpointNextId++;
                funBreakpoints.breakpoints.put(line, new Breakpoint(uuid, line));
                return Optional.of(uuid);
            }
            return Optional.empty();
        }

        public void clearBreakpoints(String filePath) {
            fileToMcPath(filePath).ifPresent(breakpoints::remove);
        }

        public boolean contains(String mcpath, int line) {
            return Optional.ofNullable(breakpoints.get(mcpath))
                    .map(funBreakpoints -> funBreakpoints.breakpoints.containsKey(line))
                    .orElse(false);
        }

        public Optional<Integer> getBreakpointId(String filePath, int line) {
            return Optional.ofNullable(this.breakpoints.get(filePath))
                    .flatMap(bps -> Optional.ofNullable(bps.breakpoints.get(line)))
                    .map(Breakpoint::id);
        }
    }


    private String currentFile;
    private int currentLine;
    private MinecraftServer server;
    private final List<BiConsumer<Integer, String>> stopConsumers = new LinkedList<>();
    private final List<Runnable> continueRunnable = new LinkedList<>();
    private final AllBreakpoints breakpoints = new AllBreakpoints();
    private int breakpointNextId = 0;
    public final int THREAD_ID = 1;

    public boolean mustStop(String file, int line) {
        return breakpoints.contains(file, line) && (!file.equals(currentFile) || line != currentLine);
    }

    public void setCurrentLine(String file, int line) {
        this.currentFile = file;
        this.currentLine = line;
    }

    public Optional<Integer> registerBreakpoint(String file, int line) {
        return breakpoints.addBreakpoint(file, line);
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }

    /**
     * Sets a breakpoint at the current execution point.
     * Freezes the server tick manager and enables debugging mode.
     * @param source The command source that triggered the breakpoint
     */
    public void triggerBreakpoint(@NotNull ServerCommandSource source) {
        source.getServer().getTickManager().setFrozen(true);
        this.stopConsumers.forEach(consumer -> this.breakpoints.getBreakpointId(currentFile, currentLine).ifPresent(n -> consumer.accept(n, "breakpoint")));
        isDebugging = true;
    }

    public void stop(String reason) {
        this.stopConsumers.forEach(consumer ->  consumer.accept(-1, reason));
    }

    public void continueExec() {
        this.continueRunnable.forEach(Runnable::run);
    }

    public void clearBreakpoints(String file) {
        breakpoints.clearBreakpoints(file);
    }

    public void onStop(BiConsumer<Integer, String> consumer) {
        this.stopConsumers.add(consumer);
    }
    public void onContinue(Runnable runnable) { this.continueRunnable.add(runnable); }

    public String getCurrentFile() {
        return currentFile;
    }

    public int getCurrentLine() {
        return currentLine;
    }

    private static Optional<String> fileToMcPath(String path) {
        String realPath = FilenameUtils.separatorsToUnix(path);
        Matcher matcher = PATH_PATTERN.matcher(realPath);
        if(matcher.find()) {
            var namespace = matcher.group("namespace");
            var rpath = matcher.group("path");
            if(path != null && namespace != null) {
                return Optional.of(namespace + ":" + rpath);
            }
        }
        return Optional.empty();
    }

    public Optional<String> getRealPath(String mcpath) {
        var manager = FunctionPathGetter.MANAGER;
        if(mcpath == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.breakpoints.breakpoints.get(mcpath).functionPath);
    }
}
