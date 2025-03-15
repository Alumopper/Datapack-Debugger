package net.gunivers.sniffer.command;

import net.minecraft.command.CommandExecutionContext;
import net.minecraft.command.Frame;
import net.minecraft.command.SourcedCommandAction;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.gunivers.sniffer.dap.ScopeManager;

/**
 * Action handler for when a function is entered during debugging.
 * This class manages the function call stack by pushing the current function
 * and its source to the stack when a function is called.
 *
 * @param <T> The type of command source being used
 *
 * @author Alumopper
 * @author theogiraudet
 */
public class FunctionInAction<T extends AbstractServerCommandSource<T>> implements SourcedCommandAction<T> {

    /** The function being entered */
    CommandFunction<T> function;

    /**
     * Creates a new function entry action handler.
     * @param function The function that is being entered
     */
    public FunctionInAction(CommandFunction<T> function){
        this.function = function;
    }

    /**
     * Executes the function entry action.
     * Pushes the current function and its source to the stack for debugging purposes.
     *
     * @param source The command source executing the function
     * @param context The command execution context
     * @param frame The current execution frame
     */
    @Override
    public void execute(T source, CommandExecutionContext<T> context, Frame frame){
        // Each time we are going into a deeper scope, we want to decrement of one to not skip the mustStop evaluation at the first command
        if(BreakPointCommand.moveSteps > 0) BreakPointCommand.moveSteps --;
        ScopeManager.get().newScope(function.id().toString(), source);
    }
}
