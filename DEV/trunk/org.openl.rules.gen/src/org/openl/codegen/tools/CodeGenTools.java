package org.openl.codegen.tools;

import java.lang.reflect.Array;

import org.openl.types.java.JavaOpenClass;
import org.openl.util.StringTool;

public class CodeGenTools {

    public static String getClassSourcePathInRulesModule(Class<?> clazz) {
        return getClassSourcePath(CodeGenConstants.RULES_SOURCE_LOCATION, clazz);
    }
    
    public static String getClassSourcePathInCoreModule(Class<?> clazz) {
        return getClassSourcePath(CodeGenConstants.CORE_SOURCE_LOCATION, clazz);
    }
    
    public static String getClassSourcePathInCommonsModule(Class<?> clazz) {
        return getClassSourcePath(CodeGenConstants.COMMONS_SOURCE_LOCATION, clazz);
    }
    
    private static String getClassSourcePath(String modulePath, Class<?> clazz) {
        return modulePath + StringTool.getFileNameOfJavaClass(clazz);
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
