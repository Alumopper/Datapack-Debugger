package net.gunivers.sniffer.mixin;

import net.minecraft.command.SteppedCommandAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SteppedCommandAction.class)
public interface SteppedCommandActionAccessors {

    @Accessor("nextActionIndex")
    int getNextActionIndex();
}
