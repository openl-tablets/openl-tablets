package org.openl.codegen.tools;

import static java.lang.reflect.Modifier.isPublic;

import org.openl.rules.helpers.NumberUtils;

/**
 * Class used by Velocity engine as external tools.
 */
public class VelocityTool {

    private static final Class<?>[] EMPTY = new Class<?>[] {};
    private static final Class<?>[] STRING = new Class<?>[] { String.class };
    private static final Class<?>[] STRING_STRING = new Class<?>[] { String.class, String.class };

    public int length(Object[] array) {
        return array.length;
    }

    public String getTypeName(Class<?> clazz) {

        if (clazz.isArray()) {
            return String.format("%s[]", clazz.getComponentType().getName());
        }
        return clazz.getName();
    }

    public String getVarArgTypeName(Class<?> clazz) {

        if (clazz.isArray()) {
            return String.format("%s...", clazz.getComponentType().getName());
        }
        return clazz.getName();
    }

    public boolean hasConstructorWithoutParams(Class<?> clazz) {
        return hasPublicConstructor(clazz, EMPTY);
    }

    public boolean hasConstructorWithPropertyName(Class<?> clazz) {
        return hasPublicConstructor(clazz, STRING);
    }

    public boolean hasConstructorWithConstraintForProperty(Class<?> clazz) {
        return hasPublicConstructor(clazz, STRING_STRING);
    }

    private static boolean hasPublicConstructor(Class<?> clazz, Class<?>[] types) {
        try {
            return isPublic(clazz.getModifiers()) && isPublic(clazz.getConstructor(types).getModifiers());
        } catch (Exception e) {
            return false;
        }
    }

    public String formatAccessorName(String name) {

        StringBuilder builder = new StringBuilder();

        builder.append(name.substring(0, 1).toUpperCase()).append(name.substring(1));

        return builder.toString();
    }

    public static Class<?> getNumericPrimitive(Class<?> wrapperClass) {
        return NumberUtils.getNumericPrimitive(wrapperClass);
    }
}
