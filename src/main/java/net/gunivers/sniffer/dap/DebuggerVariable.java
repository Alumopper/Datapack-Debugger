package net.gunivers.sniffer.dap;

import java.util.List;

/**
 * Record representing a variable in the debugger.
 * Used to store and display variable information to the debugging client.
 * 
 * @param id The unique identifier for this variable
 * @param name The name of the variable
 * @param value The string representation of the variable's value
 * @param children A list of child variables (for complex objects/structures)
 * @param isRoot Whether this variable is a root-level variable, i.e. a variable that must be displayed
 *               directly in the scope rather than as a child of another variable
 *
 * @author theogiraudet
 */
public record DebuggerVariable(int id, String name, String value, List<DebuggerVariable> children, boolean isRoot) {
}
