package org.openl.extension.xmlrules.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.helpers.RulesUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.StringTool;

public class XmlRules {
    public static void Push(String cell, Object value) {
        LazyCellExecutor cache = LazyCellExecutor.getInstance();
        if (cache == null) {
            throw new IllegalStateException("Cells cache not initialized");
        }
        cache.push(cell, value);
    }

    public static void Pop(String cell) {
        LazyCellExecutor cache = LazyCellExecutor.getInstance();
        if (cache == null) {
            throw new IllegalStateException("Cells cache not initialized");
        }
        cache.pop(cell);
    }

    public static Object Cell(String cell) {
        LazyCellExecutor cache = LazyCellExecutor.getInstance();
        if (cache == null) {
            throw new IllegalStateException("Cells cache not initialized");
        }

        return cache.getCellValue(cell);
    }

    public static Object[][] CellRange(String cell, int rows, int cols) {
        LazyCellExecutor cache = LazyCellExecutor.getInstance();
        if (cache == null) {
            throw new IllegalStateException("Cells cache not initialized");
        }

        return cache.getCellValues(cell, rows, cols);
    }

    public static Object Field(Object target, String fieldName, Object index) {
        Integer num = HelperFunctions.toInteger(index);

        if (target == null) {
            return null;
        }

        Class<?> targetClass = target.getClass();
        if (!targetClass.isArray()) {
            return getArrayElement(getField(target, fieldName), num);
        }

        List<Object> values = new ArrayList<Object>();
        Class<?> type = Void.class;

        int length = Array.getLength(target);
        for (int i = 0; i < length; i++) {
            Object o = Array.get(target, i);
            Object field;
            if (o == null) {
                field = null;
            } else {
                if (o.getClass().isArray()) {
                    field = Field(o, fieldName, index);
                } else {
                    field = getArrayElement(getField(o, fieldName), num);
                }
            }

            if (field == null) {
                values.add(null);
            } else {
                Object[] flatten = RulesUtils.flatten(field);
                values.addAll(Arrays.asList(flatten));
                type = RulesUtils.getCommonSuperClass(type, flatten.getClass().getComponentType());
            }
        }

        if (type == Void.class) {
            type = Object.class;
        }

        return values.toArray((Object[]) Array.newInstance(type, values.size()));
    }

    public static Object Field(Object target, String fieldName) {
        if (target == null) {
            return null;
        }

        Class<?> targetClass = target.getClass();
        if (!targetClass.isArray()) {
            return getField(target, fieldName);
        }

        List<Object> values = new ArrayList<Object>();
        Class<?> type = Void.class;

        int length = Array.getLength(target);
        for (int i = 0; i < length; i++) {
            Object o = Array.get(target, i);
            Object field;
            if (o == null) {
                field = null;
            } else {
                if (o.getClass().isArray()) {
                    field = Field(o, fieldName);
                } else {
                    field = getField(o, fieldName);
                }
            }

            if (field == null) {
                values.add(null);
            } else {
                Object[] flatten = RulesUtils.flatten(field);
                values.addAll(Arrays.asList(flatten));
                type = RulesUtils.getCommonSuperClass(type, flatten.getClass().getComponentType());
            }
        }

        if (type == Void.class) {
            type = Object.class;
        }

        return values.toArray((Object[]) Array.newInstance(type, values.size()));
    }

    // To support array calls correctly
    public static Object Field(Object[][] target, String fieldName) {
        return Field((Object) target, fieldName);
    }

    // To support array calls correctly
    public static Object Field(Object[][] target, String fieldName, Object index) {
        return Field((Object) target, fieldName, index);
    }

    private static Object getArrayElement(Object array, Integer index) {
        if (array == null) {
            return null;
        }

        if (!array.getClass().isArray()) {
            if (index != 1) {
                throw new ArrayIndexOutOfBoundsException(index);
            }

            return array;
        } else {
            return Array.get(array, index - 1);
        }
    }

    private static Object getField(Object target, String fieldName) {
        if (target == null) {
            return null;
        }

        Class<?> targetClass = target.getClass();
        JavaOpenClass openClass = JavaOpenClass.getOpenClass(targetClass);
        IOpenMethod method = openClass.getMethod(StringTool.getGetterName(fieldName), new IOpenClass[0]);
        if (method == null) {
            throw new OpenLRuntimeException("There is no field '" + fieldName + "' in type '" + targetClass.getSimpleName() + "'");
        }
        return method.invoke(target, null, null);
    }
}
