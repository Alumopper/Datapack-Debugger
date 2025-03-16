package net.gunivers.sniffer;

import net.minecraft.server.function.ExpandedMacro;
import net.minecraft.util.Identifier;

/**
 * Utility class providing helper methods for the Datapack Debugger.
 * Contains various utility functions to work with Minecraft's internal classes
 * and handle encapsulation breaking where necessary.
 */
public class Utils {

    /**
     * Retrieves the identifier from an ExpandedMacro function.
     * This method uses EncapsulationBreaker to access the private field
     * that contains the function's identifier.
     *
     * @param function The ExpandedMacro function to get the ID from
     * @return The Identifier of the function, or a fallback identifier if not found
     */
    public static Identifier getId(ExpandedMacro<?> function) {
        return (Identifier) EncapsulationBreaker.getAttribute(function, "functionIdentifier").orElse(Identifier.of("foo:bar"));
    }

}
