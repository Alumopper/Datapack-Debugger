package top.mcfpp.mod.debugger.command;

import net.minecraft.command.CommandExecutionContext;
import net.minecraft.command.Frame;
import net.minecraft.command.SourcedCommandAction;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import top.mcfpp.mod.debugger.dap.DebuggerState;

/**
 * Action handler for when a function is exited during debugging.
 * This class manages the function call stack by popping the current function
 * and its source from the stack when a function returns.
 *
 * @param <T> The type of command source being used
 */
public class FunctionOutAction<T extends AbstractServerCommandSource<T>> implements SourcedCommandAction<T> {

    /** The function being exited */
    CommandFunction<T> function;

    /**
     * Creates a new function exit action handler.
     * @param function The function that is being exited
     */
    public FunctionOutAction(CommandFunction<T> function){
        this.function = function;
    }

    /**
     * Executes the function exit action.
     * Pops the current function and its source from the stack for debugging purposes.
     *
     * @param source The command source executing the function
     * @param context The command execution context
     * @param frame The current execution frame
     */
    @Override
    public void execute(T source, CommandExecutionContext<T> context, Frame frame){
        FunctionStackManager.pop();
        DebuggerState.get().setCurrentLine(null, -1);
    }
}
