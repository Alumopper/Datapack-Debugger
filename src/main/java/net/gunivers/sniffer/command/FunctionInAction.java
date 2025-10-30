package net.gunivers.sniffer.command;

import net.gunivers.sniffer.EncapsulationBreaker;
import net.minecraft.command.CommandExecutionContext;
import net.minecraft.command.Frame;
import net.minecraft.command.SourcedCommandAction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.gunivers.sniffer.dap.ScopeManager;
import net.minecraft.server.function.ExpandedMacro;

import static net.gunivers.sniffer.Utils.getId;
import static net.gunivers.sniffer.command.StepType.isStepOut;

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

    /** 
     * The function being entered.
     * This reference stores the Minecraft function that is about to be executed
     * and is used to create a new debugging scope with the proper function ID.
     */
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
        // We must do it here since the decrementation in FixCommandActionMixin is not called when a mcfunction is called
        if(BreakPointCommand.moveSteps > 0 && !isStepOut()) BreakPointCommand.moveSteps --;
        var id = function.id();
        if(function instanceof ExpandedMacro<T> macro) {
            id = getId(macro);
        }

        var macroVariables = EncapsulationBreaker.getAttribute(frame, "function").flatMap(fun -> EncapsulationBreaker.getAttribute(fun, "arguments"));
        ScopeManager.get().newScope(id.toString(), source, (NbtCompound) macroVariables.orElse(null));
    }
}
