package top.mcfpp.mod.debugger.command;

import net.minecraft.server.command.AbstractServerCommandSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Manages the function call stack during debugging.
 * This class maintains two stacks:
 * 1. A stack of command sources for tracking the execution context
 * 2. A stack of function names for tracking the call hierarchy
 */
public class FunctionStackManager {

    public record DebugFrame(int id, String name, String caller, int callLine, AbstractServerCommandSource<?> source) {}

    /** Stack of command sources for tracking execution context */
    public static Stack<AbstractServerCommandSource<?>> source = new Stack<>();

    /** Stack of function names for tracking call hierarchy */
    static Stack<DebugFrame> functionStack = new Stack<>();

    private static int id = 0;

    /**
     * Pops both the source and function stacks.
     * @return The popped function name, or null if the function stack is empty
     */
    public static DebugFrame pop(){
        if(!source.isEmpty()) source.pop();
        if(!functionStack.isEmpty()) return functionStack.pop();
        return null;
    }

    public static void push(String name, String caller, int callLine, AbstractServerCommandSource<?> source) {
        functionStack.push(new DebugFrame(id++, name, caller, callLine, source));
    }

    /**
     * Returns a copy of the current function stack.
     * @return A list containing all function names in the current call hierarchy
     */
    public static List<DebugFrame> getStack(){
        return new ArrayList<>(functionStack);
    }

}
