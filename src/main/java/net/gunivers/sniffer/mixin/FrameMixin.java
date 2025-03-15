package net.gunivers.sniffer.mixin;

import net.minecraft.command.Frame;
import net.minecraft.server.function.Procedure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Mixin for the Frame class to add additional functionality for debugging.
 * This mixin adds the capability to track which function a frame is associated with,
 * which is necessary for proper function call tracing during debugging.
 *
 * @author Alumopper
 */
@Mixin(Frame.class)
public class FrameMixin {
    /**
     * Reference to the function/procedure that created this frame.
     * Used by the debugger to track function execution and call hierarchies.
     */
    @Unique
    private Procedure<?> function;
}
