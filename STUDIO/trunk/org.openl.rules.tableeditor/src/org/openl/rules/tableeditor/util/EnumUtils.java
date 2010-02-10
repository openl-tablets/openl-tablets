package org.openl.rules.tableeditor.util;

import java.util.ArrayList;
import java.util.List;

public class EnumUtils {

    public static String getName(Enum<?> constant) {
        return constant.name();
    }
    
    public static String[] getNames(Object[] constants) {

        List<String> namesList = new ArrayList<String>();

        for (Object constant : constants) {
            namesList.add(getName((Enum<?>) constant));
        }

        return namesList.toArray(new String[namesList.size()]);
    }

    public static String[] getNames(Class<?> enumClass) {

        Object[] constants = getEnumConstants(enumClass);

       return getNames(constants);
    }

    public static String[] getValues(Class<?> enumClass) {

        Object[] constants = getEnumConstants(enumClass);

        List<String> valuesList = new ArrayList<String>();

        for (Object constant : constants) {
            valuesList.add(((Enum<?>) constant).toString());
        }

        return valuesList.toArray(new String[valuesList.size()]);
    }

    public static Object[] getEnumConstants(Class<?> enumClass) {

        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException("The Class must be a enum");
        }

        return enumClass.getEnumConstants();
    }

}
