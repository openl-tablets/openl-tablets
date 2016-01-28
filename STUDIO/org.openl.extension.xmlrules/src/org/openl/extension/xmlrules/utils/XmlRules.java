package org.openl.extension.xmlrules.utils;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openl.exception.OpenLRuntimeException;
import org.openl.extension.xmlrules.java.api.FilteredValue;
import org.openl.rules.helpers.RulesUtils;
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

    public static FilteredValue Field(Object target, String fieldName) {
        if (target instanceof FilteredValue) {
            target = ((FilteredValue) target).getValue();
        }

        if (target == null) {
            return null;
        }

        Class<?> targetClass = target.getClass();
        if (!targetClass.isArray()) {
            return getField(target, fieldName);
        }

        List<Object> values = new ArrayList<Object>();
        Class<?> type = Void.class;

        for (int i = 0; i < Array.getLength(target); i++) {
            Object o = Array.get(target, i);
            Object field = getField(o, fieldName);

            if (field == null) {
                values.add(null);
            } else {
                Object[] flatten = RulesUtils.flatten(field);
                values.addAll(Arrays.asList(flatten));
                type = RulesUtils.getCommonSuperClass(type, flatten.getClass().getComponentType());
            }
        }

        return new FilteredValue(values.toArray((Object[]) Array.newInstance(type, values.size())));
    }

    private static FilteredValue getField(Object target, String fieldName) {
        if (target == null) {
            return null;
        }

        Class<?> targetClass = target.getClass();
        Method method;
        try {
            method = targetClass.getMethod(StringTool.getGetterName(fieldName));
            return new FilteredValue(method.invoke(target));
        } catch (NoSuchMethodException e1) {
            throw new OpenLRuntimeException("There is no field '" + fieldName + "'");
        } catch (IllegalAccessException e) {
            throw new OpenLRuntimeException("Can't access the field '" + fieldName + "'", e);
        } catch (InvocationTargetException e) {
            throw new OpenLRuntimeException("Can't get the field '" + fieldName + "'", e);
        }
    }
}
