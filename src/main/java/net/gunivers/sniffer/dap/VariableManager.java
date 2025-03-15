package net.gunivers.sniffer.dap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.command.AbstractServerCommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages variable conversion and representation for the debugger.
 * This class provides utility methods to convert Minecraft objects into debugger variables
 * that can be displayed in the debugging client.
 */
public class VariableManager {

    /**
     * Converts a command source into a map of debugger variables.
     * This method extracts relevant information from the command source such as
     * the executor entity, position, and rotation.
     *
     * @param source The command source to convert
     * @param startIndex The starting index for variable IDs
     * @return A map of variable IDs to debugger variables
     */
    public static Map<Integer, DebuggerVariable> convertCommandSource(AbstractServerCommandSource<?> source, int startIndex) {
        if(source instanceof ServerCommandSource commandSource) {
            var executorVariable = convertEntityVariables(commandSource.getEntity(), startIndex, true);
            var currentIndex = executorVariable.getRight();

            var posVariable = convertPos(commandSource.getPosition(), currentIndex, true);
            currentIndex = posVariable.getRight();

            var rotVariable = convertRotation(commandSource.getRotation(), currentIndex, true);
            currentIndex = rotVariable.getRight();

            var worldVariable = convertWorld(commandSource.getWorld(), currentIndex, true);

            var result = new ArrayList<DebuggerVariable>(currentIndex);
            result.add(executorVariable.getLeft());
            result.add(posVariable.getLeft());
            result.add(rotVariable.getLeft());
            result.add(worldVariable);

            return flattenToMap(result);
        } else {
            throw new IllegalStateException("AbstractServerCommandSource is not a ServerCommandSource but a " + source.getClass().getSimpleName());
        }
    }

    /**
     * Converts an entity into a debugger variable with its properties.
     * This method extracts entity information such as type, name, UUID, position, rotation, and dimension.
     *
     * @param entity The entity to convert, can be null (representing the server)
     * @param startIndex The starting index for variable IDs
     * @param isRoot Whether this variable is a root-level variable
     * @return A pair containing the created variable and the next available ID
     */
    private static Pair<DebuggerVariable, Integer> convertEntityVariables(@Nullable Entity entity, int startIndex, boolean isRoot) {
        if(entity == null) {
            return new Pair<>(new DebuggerVariable(startIndex, "executor", "server", List.of(), isRoot), ++startIndex);
        }

        var id = startIndex + 1;

        var objectType = new DebuggerVariable(id++, "type", typeToString(entity.getType()), List.of(), false);
        var objectName = new DebuggerVariable(id++, "name", entity.getName().getLiteralString(), List.of(), false);

        var objectUuid = new DebuggerVariable(id++, "uuid", entity.getUuidAsString(), List.of(), false);

        var pos = convertPos(entity.getPos(), id, false);
        id = pos.getRight();

        var rot = convertRotation(entity.getRotationClient(), id, false);
        id = rot.getRight();

        var objectDimension = convertWorld(entity.getEntityWorld(), id++, false);

        var displayName = objectName.value() != null ? objectName.value() : objectType.value();


        var children = List.of(objectType, objectName, objectUuid, pos.getLeft(), rot.getLeft(), objectDimension);
        return new Pair<>(new DebuggerVariable(startIndex, "executor", displayName, children, isRoot), id);
    }

    /**
     * Converts an entity type to a readable string representation.
     *
     * @param type The entity type to convert
     * @return A string representation of the entity type
     */
    private static String typeToString(EntityType<?> type) {
        var str = type.toString(); // Formatted as translationKey
        return str.substring(str.lastIndexOf(".") + 1);
    }

    /**
     * Converts a 3D position vector into a debugger variable.
     * This method creates a variable representing the position with x, y, and z components.
     *
     * @param vec The position vector to convert
     * @param id The starting ID for the variable
     * @param isRoot Whether this variable is a root-level variable
     * @return A pair containing the created variable and the next available ID
     */
    private static Pair<DebuggerVariable, Integer> convertPos(Vec3d vec, int id, boolean isRoot) {
        var posId = id++;
        var objectPosX = new DebuggerVariable(id++, "x", Double.toString(vec.x), List.of(), false);
        var objectPosY = new DebuggerVariable(id++, "y", Double.toString(vec.y), List.of(), false);
        var objectPosZ = new DebuggerVariable(id++, "z", Double.toString(vec.z), List.of(), false);
        var posStr = MessageFormat.format("[{0}, {1}, {2}]", vec.x, vec.y, vec.z);
        var objectPosition = new DebuggerVariable(posId, "position", posStr, List.of(objectPosX, objectPosY, objectPosZ), isRoot);
        return new Pair<>(objectPosition, id);
    }

    /**
     * Converts a 2D rotation vector into a debugger variable.
     * This method creates a variable representing the rotation with yaw and pitch components.
     *
     * @param vec The rotation vector to convert
     * @param id The starting ID for the variable
     * @param isRoot Whether this variable is a root-level variable
     * @return A pair containing the created variable and the next available ID
     */
    private static Pair<DebuggerVariable, Integer> convertRotation(Vec2f vec, int id, boolean isRoot) {
        var posId = id++;
        var objectPosX = new DebuggerVariable(id++, "yaw", Double.toString(vec.x), List.of(), false);
        var objectPosY = new DebuggerVariable(id++, "pitch", Double.toString(vec.y), List.of(), false);
        var posStr = MessageFormat.format("[{0}, {1}]", vec.x, vec.y);
        var objectPosition = new DebuggerVariable(posId, "rotation", posStr, List.of(objectPosX, objectPosY), isRoot);
        return new Pair<>(objectPosition, id);
    }

    /**
     * Converts a Minecraft world into a debugger variable.
     * This method creates a variable representing the world/dimension.
     *
     * @param world The world to convert
     * @param id The ID for the variable
     * @param isRoot Whether this variable is a root-level variable
     * @return The created debugger variable
     */
    private static DebuggerVariable convertWorld(World world, int id, boolean isRoot) {
        return new DebuggerVariable(id, "world", world.getDimension().effects().getPath(), List.of(), isRoot);
    }

    /**
     * Flattens a list of debugger variables into a map indexed by variable ID.
     * This method processes the hierarchical structure of variables and their children
     * into a flat map for easier lookup.
     *
     * @param variables The list of variables to flatten
     * @return A map of variable IDs to debugger variables
     */
    private static Map<Integer, DebuggerVariable> flattenToMap(List<DebuggerVariable> variables) {
        var map = new HashMap<Integer, DebuggerVariable>();
        variables.forEach(variable -> flattenToMap(variable, map));
        return map;
    }

    /**
     * Helper method to recursively flatten a variable and its children into a map.
     * This method adds the variable and all its children to the provided map.
     *
     * @param variable The variable to flatten
     * @param variables The map to add the flattened variables to
     */
    private static void flattenToMap(DebuggerVariable variable, Map<Integer, DebuggerVariable> variables) {
        variables.put(variable.id(), variable);
        variable.children().forEach(child -> flattenToMap(child, variables));
    }

}
