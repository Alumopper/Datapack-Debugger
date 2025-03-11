package top.mcfpp.mod.debugger;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;

public class EncapsulationBreaker {

    public static Optional<Object> callFunction(Object o, String functionName, Object ...params) {
        try {
            params = Optional.ofNullable(params).orElse(new Object[]{});
            var method = o.getClass().getDeclaredMethod(functionName, Arrays.stream(params).map(Object::getClass).toArray(Class[]::new));
            method.setAccessible(true);
            return Optional.of(method.invoke(o, params));
        } catch(Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<Object> getAttribute(Object o, String attributeName) {
        try {
            var attribute = o.getClass().getDeclaredField(attributeName);
            attribute.setAccessible(true);
            return Optional.of(attribute.get(o));
        } catch(Exception e) {
            return Optional.empty();
        }
    }

}
