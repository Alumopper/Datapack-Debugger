package net.gunivers.sniffer.util;

import com.mojang.logging.LogUtils;
import net.minecraft.server.function.ExpandedMacro;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

/**
 * Utility class providing helper methods for the Datapack Debugger.
 * Contains various utility functions to work with Minecraft's internal classes
 * and handle encapsulation breaking where necessary.
 */
public class Utils {

    final static Logger LOGGER = LogUtils.getLogger();

    /**
     * Retrieves the identifier from an ExpandedMacro function.
     * This method uses EncapsulationBreaker to access the private field
     * that contains the function's identifier.
     *
     * @param function The ExpandedMacro function to get the ID from
     * @return The Identifier of the function, or a fallback identifier if not found
     */
    public static Identifier getId(ExpandedMacro<?> function) {
        return ReflectUtil.getT(function, "functionIdentifier", Identifier.class).onFailure(LOGGER::error).getDataOrElse(Identifier.of("foo:bar"));
    }

    private static final String MESSAGE_PREFIX = "[Sniffer] ";

    public static Text addSnifferPrefix(Text text) {
        var header = Text.literal(MESSAGE_PREFIX).formatted(Formatting.AQUA);
        return header.append(text);
    }

    public static Text addSnifferPrefix(String text) {
        return addSnifferPrefix(Text.literal(text).formatted(Formatting.WHITE));
    }

}
