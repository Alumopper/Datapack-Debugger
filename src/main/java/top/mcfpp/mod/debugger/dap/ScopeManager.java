package top.mcfpp.mod.debugger.dap;

import net.minecraft.server.command.AbstractServerCommandSource;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Manager for debug scopes in the debugger.
 * Handles scope lifecycle, variable access, and scope stacking during debugging.
 * Implemented as a singleton to ensure a single instance is used throughout the application.
 */
public class ScopeManager {

    private static ScopeManager singleton;

    /**
     * Private constructor for singleton pattern.
     */
    private ScopeManager() {}

    /**
     * Gets the singleton instance of the ScopeManager.
     * Thread-safe implementation with double-checked locking.
     * 
     * @return The ScopeManager singleton instance
     */
    public static ScopeManager get() {
        if (singleton == null) {
            synchronized (ScopeManager.class) {
                if (singleton == null) {
                    singleton = new ScopeManager();
                }
            }
        }
        return singleton;
    }

    private static int ID = 1;

    /**
     * Represents a debug scope during execution.
     * A scope contains information about the currently executing function,
     * its variables, and its position in the call stack.
     */
    public static class DebugScope {
        private final String function;
        private final AbstractServerCommandSource<?> executor;
        private int line = -1;
        private final Map<Integer, DebuggerVariable> variables;
        private final int id = ID++;
        private final DebugScope parent;

        /**
         * Creates a new debug scope.
         * 
         * @param parent The parent scope, or null if this is the root scope
         * @param currentFile The function/file path being executed
         * @param executor The command source executing the function
         */
        protected DebugScope(@Nullable DebugScope parent, String currentFile, AbstractServerCommandSource<?> executor) {
            this.parent = parent;
            this.function = currentFile;
            this.executor = executor;
            this.variables = VariableManager.convertCommandSource(executor, ID);
            ID += variables.size();
        }

        /**
         * Gets the function path for this scope.
         * 
         * @return The function path
         */
        public String getFunction() {
            return function;
        }

        /**
         * Gets the current line number in the function.
         * 
         * @return The line number, or -1 if not set
         */
        public int getLine() {
            return line;
        }

        /**
         * Gets the command source executing this scope.
         * 
         * @return The command source
         */
        public AbstractServerCommandSource<?> getExecutor() {
            return executor;
        }

        /**
         * Gets the unique ID for this scope.
         * 
         * @return The scope ID
         */
        public int getId() {
            return id;
        }

        /**
         * Gets all variables in this scope.
         * 
         * @return A list of all variables in this scope
         */
        public List<DebuggerVariable> getVariables() {
            return List.copyOf(variables.values());
        }

        /**
         * Gets the function name of the caller (parent scope).
         * 
         * @return An Optional containing the caller function, or empty if this is the root scope
         */
        public Optional<String> getCallerFunction() {
            return Optional.ofNullable(parent).map(DebugScope::getFunction);
        }

        /**
         * Gets the line number in the caller where this function was called.
         * 
         * @return An Optional containing the caller line, or empty if this is the root scope
         */
        public Optional<Integer> getCallerLine() {
            return Optional.ofNullable(parent).map(DebugScope::getLine);
        }

        /**
         * Gets all root-level variables in this scope.
         * 
         * @return A list of root variables
         */
        public List<DebuggerVariable> getRootVariables() {
            return this.variables.values().stream().filter(DebuggerVariable::isRoot).toList();
        }

        /**
         * Sets the current line number for this scope.
         * 
         * @param currentLine The line number to set
         */
        public void setLine(int currentLine) {
            this.line = currentLine;
        }
    }

    /** Stack of debug scopes representing the call hierarchy */
    private final Stack<DebugScope> debugScopeStack = new Stack<>();
    /** The currently active scope */
    private DebugScope currentScope = null;
    /** Set of known scope IDs for quick lookups */
    private final Set<Integer> scopeIds = new HashSet<>();

    /**
     * Creates a new scope and pushes it onto the scope stack.
     * 
     * @param function The function mcpath
     * @param executor The command source executing the function
     */
    public void newScope(String function, AbstractServerCommandSource<?> executor) {
        var scope = new DebugScope(this.currentScope, function, executor);
        this.scopeIds.add(scope.id);
        this.debugScopeStack.push(scope);
        this.currentScope = scope;
    }

    /**
     * Removes the current scope from the stack and sets the parent as current.
     * Call this when exiting a function.
     */
    public void unscope() {
        if(this.debugScopeStack.isEmpty()) {
            return;
        }
        debugScopeStack.pop();
        this.scopeIds.remove(currentScope.id);
        currentScope = debugScopeStack.isEmpty() ? null : debugScopeStack.peek();
    }

    /**
     * Gets the number of active scopes.
     * 
     * @return The number of scopes in the stack
     */
    public int count() {
        return this.debugScopeStack.size();
    }

    /**
     * Clears all scopes and resets the state.
     */
    public void clear() {
        this.debugScopeStack.clear();
        this.scopeIds.clear();
        ID = 1;
    }

    /**
     * Retrieves a debug scope by its ID.
     * 
     * @param id The ID of the scope to find
     * @return An Optional containing the scope if found, or empty if no scope with the given ID exists
     */
    public Optional<DebugScope> getScope(int id) {
        return this.debugScopeStack.stream()
                .filter(scope -> scope.id == id)
                .findFirst();
    }

    /**
     * Gets all root-level variables from a given scope.
     * A root variable is one that should be displayed directly in the scope view.
     * 
     * @param scope The scope to get variables from
     * @return A list of root-level variables in this scope
     */
    private List<DebuggerVariable> getRootVariables(DebugScope scope) {
        return scope.variables.values().stream().filter(DebuggerVariable::isRoot).toList();
    }

    /**
     * Gets variables associated with a given ID.
     * If the ID matches a scope, returns root variables of that scope.
     * If the ID matches a variable, returns its children.
     * 
     * @param id The ID of the scope or variable
     * @return An Optional containing the list of variables, or empty if no match was found
     */
    public Optional<List<DebuggerVariable>> getVariables(int id) {
        if(this.scopeIds.contains(id)) {
            return debugScopeStack.stream().filter(scope -> scope.id == id).findFirst().map(this::getRootVariables);
        } else {
            return this.debugScopeStack.stream()
                    .flatMap(scope -> scope.getVariables().stream())
                    .filter(var -> var.id() == id)
                    .findFirst()
                    .map(DebuggerVariable::children);
        }
    }

    /**
     * Gets all active debug scopes in the call stack.
     * 
     * @return The list of all debug scopes
     */
    public List<DebugScope> getDebugScopes() {
        return this.debugScopeStack;
    }

    /**
     * Gets the currently active scope.
     * 
     * @return An Optional containing the current scope, or empty if no scope is active
     */
    public Optional<DebugScope> getCurrentScope() {
        return Optional.ofNullable(currentScope);
    }
}
