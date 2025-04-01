package com.example.examplemod.agent;

import java.lang.reflect.Method;
import java.util.List;

public class DynamicInvoker {

    // Method to get the correct Method object based on method name and argument types
    public static Method getMethod(Object toCallOn, String methodName, List<Object> arguments) throws NoSuchMethodException {
        // Assuming the method resides in a class where the method is defined
        Class<?> clazz = toCallOn.getClass(); // Replace with your actual class name

        // Prepare argument types for method matching
        Class<?>[] parameterTypes = new Class[arguments.size()];
        for (int i = 0; i < arguments.size(); i++) {
            parameterTypes[i] = arguments.get(i).getClass();
        }


        // Find and return the matching method
        return clazz.getDeclaredMethod(methodName, parameterTypes);
    }

    // Helper method to convert arguments to the correct type if needed
    public static Object convertArgument(Object argument, Class<?> targetType) {
        if (targetType.isAssignableFrom(argument.getClass())) {
            return argument;
        }

        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(argument.toString());
        } else if (targetType == String.class) {
            return argument.toString();
        }
        // Add other type conversions as needed
        return argument;
    }
}
