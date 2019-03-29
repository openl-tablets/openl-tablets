package org.openl.util;

import java.util.ArrayList;
import java.util.List;

public class EnumUtils {

    public static String getName(Enum<?> constant) {
        return constant.name();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Object valueOf(Class enumClass, String constantName) {
        return StringUtils.isEmpty(constantName) ? null : Enum.valueOf(enumClass, constantName);
    }

    public static String[] getNames(Object[] constants) {
        List<String> names = new ArrayList<>();
        for (Object constant : constants) {
            if (constant != null) {
                names.add(getName((Enum<?>) constant));
            }
        }
        return names.toArray(new String[names.size()]);
    }

    public static String[] getValues(Object[] constants) {
        List<String> values = new ArrayList<>();
        for (Object constant : constants) {
            values.add(((Enum<?>) constant).toString());
        }
        return values.toArray(new String[values.size()]);
    }

    public static String[] getNames(Class<?> enumClass) {
        Object[] constants = getEnumConstants(enumClass);
        return getNames(constants);
    }

    public static String[] getValues(Class<?> enumClass) {
        Object[] constants = getEnumConstants(enumClass);
        List<String> values = new ArrayList<>();
        for (Object constant : constants) {
            values.add(((Enum<?>) constant).toString());
        }
        return values.toArray(new String[values.size()]);
    }

    public static Object[] getEnumConstants(Class<?> enumClass) {
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException("The Class must be a enum");
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
