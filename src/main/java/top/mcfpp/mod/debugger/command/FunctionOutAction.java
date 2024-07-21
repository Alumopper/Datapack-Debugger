package top.mcfpp.mod.debugger.command;

import net.minecraft.command.CommandExecutionContext;
import net.minecraft.command.Frame;
import net.minecraft.command.SourcedCommandAction;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.Procedure;

public class FunctionOutAction<T extends AbstractServerCommandSource<T>> implements SourcedCommandAction<T> {

    Procedure<T> function;

    public FunctionOutAction(Procedure<T> function){
        this.function = function;
    }

    @Override
    public void execute(T source, CommandExecutionContext<T> context, Frame frame){
        FunctionStackManager.pop();
    }

}
