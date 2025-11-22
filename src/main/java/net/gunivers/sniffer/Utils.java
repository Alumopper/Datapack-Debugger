package net.gunivers.sniffer;

import net.gunivers.sniffer.util.ReflectUtil;
import net.minecraft.server.function.ExpandedMacro;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import static net.minecraft.text.Text.literal;

/**
 * Utility class providing helper methods for the Datapack Debugger.
 * Contains various utility functions to work with Minecraft's internal classes
 * and handle encapsulation breaking where necessary.
 */
public class Utils {

    private static final String MESSAGE_PREFIX = "[Sniffer] ";

    public static Text addSnifferPrefix(Text text) {
        var header = Text.literal(MESSAGE_PREFIX).formatted(Formatting.AQUA);
        return header.append(text);
    }

    public static Text addSnifferPrefix(String text) {
        return addSnifferPrefix(Text.literal(text).formatted(Formatting.WHITE));
    }

}
