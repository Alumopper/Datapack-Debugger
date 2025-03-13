package top.mcfpp.mod.debugger.dap;

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

public class VariableManager {

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

    private static String typeToString(EntityType<?> type) {
        var str = type.toString(); // Formatted as translationKey
        return str.substring(str.lastIndexOf(".") + 1);
    }

    private static Pair<DebuggerVariable, Integer> convertPos(Vec3d vec, int id, boolean isRoot) {
        var posId = id++;
        var objectPosX = new DebuggerVariable(id++, "x", Double.toString(vec.x), List.of(), false);
        var objectPosY = new DebuggerVariable(id++, "y", Double.toString(vec.y), List.of(), false);
        var objectPosZ = new DebuggerVariable(id++, "z", Double.toString(vec.z), List.of(), false);
        var posStr = MessageFormat.format("[{0}, {1}, {2}]", vec.x, vec.y, vec.z);
        var objectPosition = new DebuggerVariable(posId, "position", posStr, List.of(objectPosX, objectPosY, objectPosZ), isRoot);
        return new Pair<>(objectPosition, id);
    }

    private static Pair<DebuggerVariable, Integer> convertRotation(Vec2f vec, int id, boolean isRoot) {
        var posId = id++;
        var objectPosX = new DebuggerVariable(id++, "yaw", Double.toString(vec.x), List.of(), false);
        var objectPosY = new DebuggerVariable(id++, "pitch", Double.toString(vec.y), List.of(), false);
        var posStr = MessageFormat.format("[{0}, {1}]", vec.x, vec.y);
        var objectPosition = new DebuggerVariable(posId, "rotation", posStr, List.of(objectPosX, objectPosY), isRoot);
        return new Pair<>(objectPosition, id);
    }

    private static DebuggerVariable convertWorld(World world, int id, boolean isRoot) {
        return new DebuggerVariable(id, "world", world.getDimension().effects().getPath(), List.of(), isRoot);
    }

    private static Map<Integer, DebuggerVariable> flattenToMap(List<DebuggerVariable> variables) {
        var map = new HashMap<Integer, DebuggerVariable>();
        variables.forEach(variable -> flattenToMap(variable, map));
        return map;
    }

    private static void flattenToMap(DebuggerVariable variable, Map<Integer, DebuggerVariable> variables) {
        variables.put(variable.id(), variable);
        variable.children().forEach(child -> flattenToMap(child, variables));
    }

}
