package top.mcfpp.mod.breakpoint.mixin;

import net.minecraft.command.Frame;
import net.minecraft.server.function.Procedure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Frame.class)
public class FrameMixin {
    @Unique
    private Procedure<?> function;
}
