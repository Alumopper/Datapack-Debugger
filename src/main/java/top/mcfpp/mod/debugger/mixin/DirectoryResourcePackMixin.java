package top.mcfpp.mod.debugger.mixin;

import com.google.common.base.Joiner;
import com.mojang.logging.LogUtils;
import net.minecraft.resource.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.PathUtil;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import top.mcfpp.mod.debugger.dap.ScopeManager;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Mixin for the DirectoryResourcePack class to enhance resource discovery for debugging.
 * This mixin overwrites the findResources method to capture file paths for datapack functions,
 * allowing the debugger to map function identifiers to their actual file locations.
 */
@Mixin(DirectoryResourcePack.class)
public class DirectoryResourcePackMixin {

    @Shadow
    private static final Logger LOGGER = LogUtils.getLogger();

    @Shadow
    private static final Joiner SEPARATOR_JOINER = Joiner.on("/");

    /**
     * Overwrites the findResources method to track function file paths.
     * This method enhances the original by capturing the physical file paths
     * of .mcfunction files and associating them with their identifiers,
     * which is essential for source mapping in the debugger.
     *
     * @author theogiraudet
     * @reason to access to the identifier as well as the foundPath
     * @param namespace The resource namespace
     * @param path The base path to search in
     * @param prefixSegments The path prefix segments
     * @param consumer The consumer to process found resources
     */
    @Overwrite
    public static void findResources(String namespace, Path path, List<String> prefixSegments, ResourcePack.ResultConsumer consumer) {
        Path path2 = PathUtil.getPath(path, prefixSegments);

        try (Stream<Path> stream = Files.find(path2, Integer.MAX_VALUE, (path2x, attributes) -> attributes.isRegularFile(), new FileVisitOption[0])) {
            stream.forEach((foundPath) -> {
                String string2 = SEPARATOR_JOINER.join(path.relativize(foundPath));
                Identifier identifier = Identifier.tryParse(namespace, string2);
                if (identifier == null) {
                    Util.logErrorOrPause(String.format(Locale.ROOT, "Invalid path in pack: %s:%s, ignoring", namespace, string2));
                } else {
                    if(identifier.getPath().endsWith(".mcfunction")) {
                        ScopeManager.get().savePath(foundPath, identifier);
                    }
                    consumer.accept(identifier, InputSupplier.create(foundPath));
                }

            });
        } catch (NotDirectoryException | NoSuchFileException var10) {
        } catch (IOException iOException) {
            LOGGER.error("Failed to list path {}", path2, iOException);
        }

    }

}
