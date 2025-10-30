package net.gunivers.sniffer.mixin;

import net.minecraft.command.SingleCommandAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Mixin to add source-file information to each command: the source function and the line
 *
 * @author theogiraudet
 */
@Mixin(SingleCommandAction.Sourced.class)
public class SingleCommandActionSourcedMixin {

    @Unique
    private String sourceFunction;

    @Unique
    private int sourceLine;

    @Unique
    public String getSourceFunction() {
        return sourceFunction;
    }

    @Unique
    public void setSourceFunction(String sourceFunction) {
        this.sourceFunction = sourceFunction;
    }

    @Unique
    public int getSourceLine() {
        return sourceLine;
    }

    @Unique
    public void setSourceLine(Integer sourceLine) {
        this.sourceLine = sourceLine;
    }
}
