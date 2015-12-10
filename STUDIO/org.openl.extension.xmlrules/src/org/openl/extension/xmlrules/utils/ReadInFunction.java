package org.openl.extension.xmlrules.utils;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.openl.extension.xmlrules.ProjectData;
import org.openl.extension.xmlrules.model.Type;
import org.openl.extension.xmlrules.model.single.FieldImpl;
import org.openl.util.StringTool;

public class ReadInFunction {
    public static Object[] readIn(String typeName, Object[][] array) {
        Set<Type> types = ProjectData.getCurrentInstance().getTypes();
        Type type = null;
        for (Type t : types) {
            if (t.getName().equalsIgnoreCase(typeName)) {
                type = t;
                break;
            }
        }

        if (type == null) {
            throw new IllegalArgumentException("Type '" + typeName + "' can't be found");
        }
        if (array.length <= 1) {
            return new Object[0];
        }

        array = transposeIfNeeded(array, type);

        List<Object> objects = new ArrayList<Object>();

        Class clazz;
        Object instance;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            clazz = Class.forName("org.openl.generated.beans." + type.getName(), true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("The type '" + typeName + "' can't be found");
        }

        for (int i = 1; i < array.length; i++) {
            try {
                instance = clazz.newInstance();
            } catch (InstantiationException e) {
                throw new IllegalArgumentException("Can't instantiate the type '" + typeName + "'");
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Can't access the type '" + typeName + "'");
            }
            for (int j = 0; j < array[0].length; j++) {
                FieldImpl field = findField(type, (String) array[0][j]);
                if (field == null) {
                    continue;
                }

                String setterName = StringTool.getSetterName(field.getName());
                Method method = null;
                for (Method m : clazz.getMethods()) {
                    if (m.getName().equals(setterName)) {
                        method = m;
                        break;
                    }
                }
                if (method == null) {
                    throw new IllegalArgumentException("Can't find a setter for the field'" + field.getName() + "' in the type '" + typeName + "'");
                }
                try {
                    method.invoke(instance, array[i][j]);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Can't access the field '" + field.getName() + "' in the type '" + typeName + "'");
                } catch (InvocationTargetException e) {
                    throw new IllegalArgumentException("Setter for the field '" + field.getName() + "' in the type '" + typeName + "' throwed an error ", e);
                }
            }

            objects.add(instance);
        }

        return objects.toArray((Object[]) Array.newInstance(clazz, objects.size()));
    }

    public static FieldImpl findField(Type type, String fieldName) {
        FieldImpl field = null;
        for (FieldImpl f : type.getFields()) {
            if (f.getName().equalsIgnoreCase(fieldName)) {
                field = f;
                break;
            }
        }
        return field;
    }

    private static Object[][] transposeIfNeeded(Object[][] array, Type type) {
        int horizontalFieldNames = 0;
        int verticalFieldNames = 0;
        for (FieldImpl field : type.getFields()) {
            for (int i = 0; i < array[0].length; i++) {
                if (field.getName().equalsIgnoreCase((String) array[0][i])) {
                    horizontalFieldNames++;
                    break;
                }
            }

            for (Object[] row : array) {
                if (field.getName().equalsIgnoreCase((String) row[0])) {
                    verticalFieldNames++;
                    break;
                }
            }
        }

        if (verticalFieldNames > horizontalFieldNames) {
            array = HelperFunctions.transpose(array);
        }
        return array;
    }
}
