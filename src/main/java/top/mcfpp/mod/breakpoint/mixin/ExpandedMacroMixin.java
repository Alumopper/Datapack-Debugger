package top.mcfpp.mod.breakpoint.mixin;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.function.ExpandedMacro;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ExpandedMacro.class)
public class ExpandedMacroMixin {

    @Unique private NbtCompound arguments = null;

}
