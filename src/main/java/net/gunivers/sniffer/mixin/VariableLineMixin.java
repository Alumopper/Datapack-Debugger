package net.gunivers.sniffer.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.brigadier.CommandDispatcher;
import net.gunivers.sniffer.util.ReflectUtil;
import net.minecraft.command.SourcedCommandAction;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.function.MacroException;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

/**
 * Mixin for Minecraft's internal VariableLine class.
 * This mixin adds line number tracking to macro variable lines, which is essential for debugging.
 * It extends the VariableLine class to store source line information and attach it to command actions.
 * 
 * @param <T> The type of command source being used
 * 
 * @author theogiraudet
 */
@Mixin(targets = "net.minecraft.server.function.Macro$VariableLine")
public class VariableLineMixin<T extends AbstractServerCommandSource<T>> {

    /**
     * Stores the line number within the function file.
     * A value of -1 indicates that the line number is not set or unknown.
     */
    @Unique
    private int line = -1;

    /**
     * Sets the line number for this variable line.
     * This method is called during function loading to track the source position.
     * 
     * @param line The line number in the source file (0-indexed)
     */
    @Unique
    void setLine(int line) {
        this.line = line;
    }

    /**
     * Gets the current line number for this variable line.
     * 
     * @return The line number, or -1 if not set
     */
    @Unique
    int getLine() {
        return this.line;
    }

    /**
     * Wraps the instantiate method to attach source information to command actions.
     * This method intercepts the creation of command actions and adds the function ID
     * and line number to enable proper debugging functionality.
     * 
     * @param args The arguments for the command
     * @param dispatcher The command dispatcher
     * @param id The function identifier
     * @param original The original operation being wrapped
     * @return The modified command action with source information attached
     */
    @WrapMethod(method = "instantiate")
    SourcedCommandAction<T> instantiate(List<String> args, CommandDispatcher<T> dispatcher, Identifier id, Operation<SourcedCommandAction<T>> original) throws MacroException {
        var result = original.call(args, dispatcher, id);
        ReflectUtil.invoke(result, "setSourceFunction", id.toString());
        int line = ReflectUtil.getT(this, "line", int.class).getData();
        ReflectUtil.invoke(result, "setSourceLine", line);
        return result;
    }
}
