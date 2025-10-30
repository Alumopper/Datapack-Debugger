package net.gunivers.sniffer.command;

import com.mojang.logging.LogUtils;
import net.gunivers.sniffer.dap.ScopeManager;
import net.gunivers.sniffer.util.ReflectUtil;
import net.minecraft.command.CommandExecutionContext;
import net.minecraft.command.Frame;
import net.minecraft.command.SourcedCommandAction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.ExpandedMacro;
import net.minecraft.server.function.Macro;
import net.minecraft.server.function.Procedure;
import org.slf4j.Logger;

import static net.gunivers.sniffer.command.StepType.isStepOut;
import static net.gunivers.sniffer.util.Utils.getId;

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

    private final static Logger LOGGER = LogUtils.getLogger();

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
        if(BreakPointCommand.isDebugging && BreakPointCommand.moveSteps > 0 && !isStepOut()){
            BreakPointCommand.moveSteps --;
        }
        var id = function.id();
        if(function instanceof ExpandedMacro<T> macro) {
            id = getId(macro);
        }
        if(ReflectUtil.getT(function, "originalMacro", Macro.class).onFailure(LOGGER::error).getDataOrElse(null) != null){
            //if originalMacro is not null, we are in a macro call, so we need to get the macro arguments and the original macro.
            var function = ReflectUtil.getT(frame, "function", Procedure.class).onFailure(LOGGER::error).getDataOrElse(null);
            if(function == null) return;
            var macroVariables = ReflectUtil.getT(function, "arguments", NbtCompound.class).onFailure(LOGGER::error).getDataOrElse(null);
            ScopeManager.get().newScope(id.toString(), source, macroVariables);
        }else{
            //otherwise it is a regular function call, so we just create a new scope with the function id without getting the macro arguments.
            ScopeManager.get().newScope(id.toString(), source);
        }

    }
}
