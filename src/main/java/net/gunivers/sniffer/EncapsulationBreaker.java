package net.gunivers.sniffer;

import java.util.Arrays;
import java.util.Optional;

/**
 * Utility class that provides methods to break encapsulation.
 *
 * @author theogiraudet
 */
public class EncapsulationBreaker {

    /**
     * Invokes a method on an object by name using reflection, even if the method is private.
     * 
     * @param o The target object on which to call the method
     * @param functionName The name of the method to call
     * @param params The parameters to pass to the method (can be null if no parameters)
     * @return An Optional containing the result of the method call, or empty if the call failed
     */
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

    /**
     * Retrieves the value of a field from an object by name using reflection, even if the field is private.
     * 
     * @param o The target object from which to get the field value
     * @param attributeName The name of the field to access
     * @return An Optional containing the value of the field, or empty if access failed
     */
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
