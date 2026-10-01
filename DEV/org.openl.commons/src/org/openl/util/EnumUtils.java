package org.openl.util;

import java.util.Arrays;
import java.util.Objects;


public final class EnumUtils {

    private EnumUtils() {
    }

    public static String getName(Enum<?> constant) {
        return constant.name();
    }

    /**
     * The constant of an enumeration a text names, matched ignoring case.
     *
     * <p>Case is ignored because a value is written by hand as often as it is chosen from a list. Text naming
     * no constant of the enumeration, and text holding nothing, stand for no value.
     *
     * @param enumClass    the enumeration the constant belongs to
     * @param constantName the text naming it
     * @return the constant it names, or {@code null} when it names none
     */
    public static Object valueOf(Class<?> enumClass, String constantName) {
        return StringUtils.isEmpty(constantName) ? null : valueOf(getEnumConstants(enumClass), constantName);
    }

    /**
     * The same, over constants read once.
     *
     * <p>Reading them costs a copy of the array, so a reader that answers many texts of one enumeration keeps
     * them rather than asking the class each time.
     *
     * @param constants    the constants of the enumeration
     * @param constantName the text naming one of them
     * @return the constant it names, or {@code null} when it names none
     */
    public static Object valueOf(Object[] constants, String constantName) {
        if (StringUtils.isEmpty(constantName)) {
            return null;
        }
        for (Object constant : constants) {
            if (constantName.equalsIgnoreCase(getName((Enum<?>) constant))) {
                return constant;
            }
        }
        return null;
    }

    public static String[] getNames(Object[] constants) {
        return Arrays.stream(constants)
                .filter(Objects::nonNull)
                .map(constant -> getName((Enum<?>) constant))
                .toArray(String[]::new);
    }

    public static String[] getValues(Object[] constants) {
        return Arrays.stream(constants).map(Object::toString).toArray(String[]::new);
    }

    public static String[] getNames(Class<?> enumClass) {
        Object[] constants = getEnumConstants(enumClass);
        return getNames(constants);
    }

    public static String[] getValues(Class<?> enumClass) {
        return getValues(getEnumConstants(enumClass));
    }

    public static Object[] getEnumConstants(Class<?> enumClass) {
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException(
                    "The class '%s' must be an enum.".formatted(enumClass.getTypeName()));
        }
        return enumClass.getEnumConstants();
    }

    public static boolean isEnum(Object value) {
        return value != null && value.getClass().isEnum();
    }

    public static boolean isEnumArray(Object value) {
        return value != null && value.getClass().isArray() && value.getClass().getComponentType().isEnum();
    }

}
