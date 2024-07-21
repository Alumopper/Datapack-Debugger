package top.mcfpp.mod.debugger.command;

import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.Procedure;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class FunctionStackManager {

    public static Stack<AbstractServerCommandSource<?>> source = new Stack<>();

    static Stack<String> functionStack = new Stack<>();

    public static void push(String function){
        functionStack.push(function);
    }

    public static String pop(){
        if(functionStack.isEmpty()){
            return null;
        }else {
            if(!source.isEmpty()) source.pop();
            return functionStack.pop();
        }
    }

    public static List<String> getStack(){
        return new ArrayList<>(functionStack);
    }

}
