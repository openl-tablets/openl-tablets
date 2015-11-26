package org.openl.extension.xmlrules.utils;

import java.util.*;

import org.openl.extension.xmlrules.ProjectData;
import org.openl.extension.xmlrules.model.Type;
import org.openl.extension.xmlrules.model.single.FieldImpl;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;

public class OutFunction {
    public static String[][] run(Object o, boolean horizontalRowValues, boolean showColumnNames) {
        if (o == null) {
            return null;
        }

        List<List<String>> result = convertToArray(o, showColumnNames);
        if (result == null)
            return null;

        String[][] arr = toArray(result);

        if (!horizontalRowValues) {
            arr = transpose(arr);
        }
        return arr;
    }

    private static List<List<String>> convertToArray(Object o, boolean showColumnNames) {
        List<List<String>> result = new ArrayList<List<String>>();

        if (o == null || isBasicType(o.getClass())) {
            ArrayList<String> row = new ArrayList<String>();
            result.add(row);
            row.add(o == null ? null : String.valueOf(o));
            return result;
        }

        if (o.getClass().isArray()) {
            Object[] objects = (Object[]) o;
            Class type = null;
            for (Object object : objects) {
                if (object == null) {
                    continue;
                }

                type = object.getClass();
                break;
            }

            if (type == null) {
                return null;
            }

            if (type.isArray()) {
                for (Object object : objects) {
                    addArrayElements(result, showColumnNames, (Object[]) object);
                }
            } else if (isBasicType(type)) {
                addArrayElements(result, showColumnNames, objects);
            } else {
                String typeName = type.getName();
                typeName = typeName.replace("org.openl.generated.beans.", "");
                LinkedHashMap<String, Integer> fieldRows = new LinkedHashMap<String, Integer>();
                Type xmlRulesType = getType(typeName);
                if (xmlRulesType == null) {
                    throw new IllegalArgumentException("Unsupported type '" + typeName + "'");
                }

                fillFieldCoordinates(xmlRulesType, fieldRows, 0, "");
                outObject(result, showColumnNames, fieldRows, o, xmlRulesType,  0, "");
            }
        } else {
            String typeName = o.getClass().getName();
            typeName = typeName.replace("org.openl.generated.beans.", "");
            LinkedHashMap<String, Integer> fieldRows = new LinkedHashMap<String, Integer>();
            Type xmlRulesType = getType(typeName);
            if (xmlRulesType == null) {
                throw new IllegalArgumentException("Unsupported type");
            }

            fillFieldCoordinates(xmlRulesType, fieldRows, 0, "");
            outObject(result, showColumnNames, fieldRows, o, xmlRulesType,  0, "");
        }
        return result;
    }

    private static boolean isBasicType(Class type) {
        return type.isPrimitive() || String.class.isAssignableFrom(type) || Double.class.isAssignableFrom(type);
    }

    private static void outObject(List<List<String>> result,
            boolean showColumnNames,
            LinkedHashMap<String, Integer> fieldRows,
            Object o, Type xmlRulesType, int currentRow, String fieldPrefix) {

        if (result.isEmpty()) {
            int size = fieldRows.keySet().size();
            for (int i = 0; i < size; i++) {
                result.add(new ArrayList<String>());
            }

            if (showColumnNames) {
                for (Map.Entry<String, Integer> entry : fieldRows.entrySet()) {
                    result.get(entry.getValue()).add(entry.getKey());
                }
            }
        }
        if (o == null) {
            result.get(currentRow).add(null);
            return;
        }

        Class<?> type = o.getClass();
        if (isBasicType(type)) {
            result.get(currentRow).add(String.valueOf(o));
            return;
        } else if (type.isArray()) {
            Object[] objects = (Object[]) o;
            for (Object object : objects) {
                outObject(result, false, fieldRows, object, xmlRulesType, currentRow, fieldPrefix);
            }
            align(result);
            return;
        }

        for (FieldImpl field : xmlRulesType.getFields()) {
            String fieldName = field.getName();

            JavaOpenClass javaOpenClass = JavaOpenClass.getOpenClass(type);
            IOpenField openField = javaOpenClass.getField(fieldName);
            Object fieldValue = openField.get(o, null);

            Type fieldType = getType(field.getTypeName());
            String newPrefix = fieldPrefix + fieldName;
            int row = fieldRows.get(newPrefix);
            outObject(result, false, fieldRows, fieldValue, fieldType, row, newPrefix);
        }
    }

    private static void align(List<List<String>> result) {
        int maxLength = 0;
        for (List<String> strings : result) {
            if (strings.size() > maxLength) {
                maxLength = strings.size();
            }
        }
        for (List<String> strings : result) {
            while (strings.size() < maxLength) {
                strings.add(null);
            }
        }
    }

    private static Type getType(String typeName) {
        if (typeName.endsWith("[]")) {
            typeName = typeName.replace("[]", "");
        }
        Set<Type> types = ProjectData.getCurrentInstance().getTypes();
        Type xmlRulesType = null;
        for (Type t : types) {
            if (typeName.equals(t.getName())) {
                xmlRulesType = t;
                break;
            }
        }
        return xmlRulesType;
    }

    private static int fillFieldCoordinates(Type xmlRulesType,
            Map<String, Integer> fieldRows,
            int currentRow,
            String fieldPrefix) {
        ProjectData projectData = ProjectData.getCurrentInstance();
        for (FieldImpl field : xmlRulesType.getFields()) {
            String fieldName = field.getName();
            String fieldTypeName = field.getTypeName();
            if (fieldTypeName.endsWith("[]")) {
                fieldTypeName = fieldTypeName.replace("[]", "");
            }
            if (!projectData.getTypeNames().contains(fieldTypeName)) {
                fieldRows.put(fieldPrefix + fieldName, currentRow);
                currentRow++;
            } else {
                Type fieldType = getType(fieldTypeName);
                if (fieldType == null) {
                    throw new IllegalArgumentException("Unsupported type");
                }
                currentRow = fillFieldCoordinates(fieldType, fieldRows, currentRow, fieldPrefix + fieldName + ".");
            }
        }

        return currentRow;
    }

    private static void addArrayElements(List<List<String>> result, boolean showColumnNames, Object[] objectRow) {
        ArrayList<String> row = new ArrayList<String>();
        result.add(row);
        if (showColumnNames) {
            row.add("Values");
        }
        if (objectRow != null) {
            for (Object elem : objectRow) {
                row.add(elem == null ? null : String.valueOf(elem));
            }
        }
    }

    private static String[][] transpose(String[][] arr) {
        if (arr.length == 0) {
            return arr;
        }

        String[][] newArr = new String[arr[0].length][arr.length];
        for (int i = 0; i < arr.length; i++) {
            String[] row = arr[i];
            for (int j = 0; j < row.length; j++) {
                newArr[j][i] = row[j];
            }
        }
        return newArr;
    }

    private static String[][] toArray(List<List<String>> result) {
        String[][] arr = new String[result.size()][];
        int max = 0;
        for (List<String> strings : result) {
            if (max < strings.size()) {
                max = strings.size();
            }
        }

        for (int i = 0; i < arr.length; i++) {
            arr[i] = result.get(i).toArray(new String[max]);
        }
        return arr;
    }
}
