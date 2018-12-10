package org.openl.codegen.tools;

import java.lang.reflect.Array;

import org.openl.types.java.JavaOpenClass;

public class CodeGenTools {

    public static String getClassSourcePathInRulesModule(Class<?> clazz) {
        return CodeGenConstants.RULES_SOURCE_LOCATION + clazz.getName().replace('.', '/') + ".java";
    }

    public static JavaOpenClass getJavaOpenClass(String name, boolean isArray) {

        Class<?> enumClass;

        try {
            enumClass = Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        if (isArray) {
            Object array = Array.newInstance(enumClass, 1);
            return JavaOpenClass.getOpenClass(array.getClass());
        }

        return JavaOpenClass.getOpenClass(enumClass);
    }

}
