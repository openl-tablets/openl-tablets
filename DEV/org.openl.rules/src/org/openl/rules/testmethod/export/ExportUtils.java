package org.openl.rules.testmethod.export;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.rules.data.PrimaryKeyField;
import org.openl.types.IOpenField;

final class ExportUtils {
    private ExportUtils() {
    }

    static Object fieldValue(Object value, IOpenField field) {
        if (value == null) {
            return null;
        }
        if (value instanceof Collection<?> collection) {
            return collection.toArray();
        }
        if (value.getClass().isArray()) {
            var length = Array.getLength(value);
            Object array = Array.newInstance(field.getType().getInstanceClass(), length);
            for (var i = 0; i < length; i++) {
                Array.set(array, i, fieldValue(Array.get(value, i), field));
            }
            return array;
        } else {
            return field.get(value, null);
        }
    }

    /** Returns the key of a Data table row or the keys of separately referenced collection elements. */
    static Object keyValue(Object value, IOpenField field) {
        if (value instanceof Collection<?> collection) {
            if (field instanceof PrimaryKeyField) {
                var key = field.get(value, null);
                if (key != null) {
                    return key;
                }
            }
            return fieldValue(collection.toArray(), field);
        }
        return fieldValue(value, field);
    }

    static List<Object> fieldValues(List<?> values, IOpenField field) {
        return values.stream().map(value -> value == null ? null : field.get(value, null)).toList();
    }

    /**
     * Lists the elements of the collections and the arrays of a list as one list.
     *
     * <p>An array of arrays gives its innermost elements, the values of the component type the array is
     * described by.
     */
    static List<Object> flatten(List<?> list) {
        var result = new ArrayList<Object>();
        for (Object element : list) {
            if (element instanceof Collection<?> collection) {
                result.addAll(collection);
            } else {
                addElements(result, element);
            }
        }

        return result;
    }

    private static void addElements(List<Object> result, Object element) {
        if (element == null || !element.getClass().isArray()) {
            result.add(element);
            return;
        }
        for (var i = 0; i < Array.getLength(element); i++) {
            addElements(result, Array.get(element, i));
        }
    }
}
