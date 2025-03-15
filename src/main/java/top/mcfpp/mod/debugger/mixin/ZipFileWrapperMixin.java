package top.mcfpp.mod.debugger.mixin;

import net.minecraft.resource.ZipResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.zip.ZipFile;

/**
 * Mixin for the ZipFileWrapper inner class of ZipResourcePack.
 * This mixin allows access to the ZipFile instance for debugging purposes.
 * 
 * Note: This class is made accessible via AccessWidener, so we don't need to use
 * reflection to access it. We still need this mixin to shadow the open method.
 */
@Mixin(targets = "net.minecraft.resource.ZipResourcePack$ZipFileWrapper")
public interface ZipFileWrapperMixin {
    
    /**
     * Shadow method to access the open method of ZipFileWrapper.
     * This method returns the ZipFile instance which is needed for resource access.
     * 
     * @return The ZipFile instance
     */
    @Shadow
    ZipFile open();
} 