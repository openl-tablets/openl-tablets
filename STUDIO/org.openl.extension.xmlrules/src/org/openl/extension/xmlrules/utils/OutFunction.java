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
            arr = HelperFunctions.transpose(arr);
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
            Class type = getElementType(objects);

            if (type == null) {
                return null;
            }

            if (type.isArray()) {
                for (Object object : objects) {
                    Object[] row = (Object[]) object;
                    Class elementType = getElementType(row);
                    if (elementType == null) {
                        continue;
                    }
                    if (elementType.isArray()) {
                        for (Object innerRow : row) {
                            addArrayElements(result, showColumnNames, (Object[]) innerRow);
                        }
                    } else {
                        if (isBasicType(elementType)) {
                            addArrayElements(result, showColumnNames, row);
                        } else {
                            List<List<String>> subResult = new ArrayList<List<String>>();
                            outComplexTypes(row, showColumnNames, subResult, elementType);
                            result.addAll(subResult);
                        }
                    }
                }
            } else if (isBasicType(type)) {
                addArrayElements(result, showColumnNames, objects);
            } else {
                outComplexTypes(o, showColumnNames, result, type);
            }
        } else {
            Class<?> type = o.getClass();
            outComplexTypes(o, showColumnNames, result, type);
        }
        return result;
    }

    private static void outComplexTypes(Object o, boolean showColumnNames, List<List<String>> result, Class<?> elementType) {
        String typeName = elementType.getName();
        typeName = typeName.replace("org.openl.generated.beans.", "");
        LinkedHashMap<String, Integer> fieldRows = new LinkedHashMap<String, Integer>();
        Type xmlRulesType = getType(typeName);
        if (xmlRulesType == null) {
            throw new IllegalArgumentException("Unsupported type '" + typeName + "'");
        }

        fillFieldCoordinates(xmlRulesType, fieldRows, 0, "");
        outObject(result, showColumnNames, fieldRows, o, xmlRulesType,  0, 0, "");
    }

    private static Class getElementType(Object[] objects) {
        for (Object object : objects) {
            if (object == null) {
                continue;
            }

            return object.getClass();
        }
        return null;
    }

    private static boolean isBasicType(Class type) {
        return type.isPrimitive()
                || String.class.isAssignableFrom(type)
                || Number.class.isAssignableFrom(type)
                || Boolean.class.isAssignableFrom(type)
                || Date.class.isAssignableFrom(type);
    }

    private static int outObject(List<List<String>> result,
            boolean showColumnNames,
            LinkedHashMap<String, Integer> fieldRows,
            Object o, Type xmlRulesType, int currentRow, int currentColumn, String fieldPrefix) {

        if (result.isEmpty()) {
            int size = fieldRows.keySet().size();
            for (int i = 0; i < size; i++) {
                result.add(new ArrayList<String>());
            }

            if (showColumnNames) {
                for (Map.Entry<String, Integer> entry : fieldRows.entrySet()) {
                    result.get(entry.getValue()).add(entry.getKey());
                }
                currentColumn++;
            }
        }
        List<String> resultRow = result.get(currentRow);
        while (resultRow.size() <= currentColumn) {
            resultRow.add(null);
        }
        if (o == null) {
            resultRow.set(currentColumn, null);
            return currentColumn + 1;
        }

        Class<?> type = o.getClass();
        if (isBasicType(type)) {
            resultRow.set(currentColumn, String.valueOf(o));
            return currentColumn + 1;
        } else if (type.isArray()) {
            Object[] objects = (Object[]) o;
            int column = currentColumn;
            for (Object object : objects) {
                column = outObject(result, false, fieldRows, object, xmlRulesType, currentRow, column, fieldPrefix);
            }
            align(result);
            return column;
        }

        if (xmlRulesType == null) {
            throw new IllegalArgumentException("Type could be omitted only in basic types (String, Integer etc)");
        }

        int nextColumn = currentColumn;
        for (FieldImpl field : xmlRulesType.getFields()) {
            String fieldName = field.getName();

            JavaOpenClass javaOpenClass = JavaOpenClass.getOpenClass(type);
            IOpenField openField = javaOpenClass.getField(fieldName);
            Object fieldValue = openField.get(o, null);

            Type fieldType = getType(field.getTypeName());
            String newPrefix = fieldPrefix + fieldName;
            Integer row = fieldRows.get(newPrefix);
            if (row == null) {
                int r = Integer.MAX_VALUE;
                for (Map.Entry<String, Integer> entry : fieldRows.entrySet()) {
                    if (entry.getKey().startsWith(newPrefix)) {
                        r = Math.min(r, entry.getValue());
                        row = r;
                    }
                }
                if (row == null) {
                    throw new IllegalArgumentException("Can't out the field " + newPrefix);
                }
            }
            int c = outObject(result, false, fieldRows, fieldValue, fieldType, row, currentColumn, newPrefix + ".");
            if (c > nextColumn) {
                nextColumn = c;
            }
        }

        return nextColumn;
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
        if (typeName == null) {
            return null;
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
                String value;
                if (elem == null) {
                    value = null;
                } else {
                    value = getString(elem);
                }
                row.add(value);
            }
        }
    }

    private static String getString(Object elem) {
        if (elem == null) {
            return null;
        }

        Class type = elem.getClass();
        if (type.isArray() && ((Object[]) elem).length == 1) {
            return getString(((Object[]) elem)[0]);
        } else {
            return String.valueOf(elem);
        }
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
