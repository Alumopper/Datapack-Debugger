package top.mcfpp.mod.debugger.command;

import net.minecraft.util.Identifier;

import java.util.*;

public class FunctionTextLoader {
    private static final Map<Identifier, List<String>> FUNCTION_TEXT = new HashMap<>();
    public static Iterable<Identifier> functionIds(){
        return FUNCTION_TEXT.keySet();
    }
    public static void put(Identifier id,List<String> lines){
        FUNCTION_TEXT.put(id,new ArrayList<>(lines));
    }
    public static List<String> get(Identifier id){
        return FUNCTION_TEXT.get(id);
    }
}
