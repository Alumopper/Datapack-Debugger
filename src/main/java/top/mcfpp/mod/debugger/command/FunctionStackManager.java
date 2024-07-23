package top.mcfpp.mod.debugger.command;

import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.Procedure;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class FunctionStackManager {

    public static Stack<AbstractServerCommandSource<?>> source = new Stack<>();

    static Stack<String> functionStack = new Stack<>();

    public static String pop(){
        if(!source.isEmpty()) source.pop();
        if(!functionStack.isEmpty()) return functionStack.pop();
        return null;
    }

    public static List<String> getStack(){
        return new ArrayList<>(functionStack);
    }

}
