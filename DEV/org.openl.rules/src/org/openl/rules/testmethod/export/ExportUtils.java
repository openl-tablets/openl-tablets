package org.openl.rules.testmethod.export;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.types.IOpenField;

final class ExportUtils {
    private ExportUtils() {
    }

    static Object fieldValue(Object value, IOpenField field) {
        if (value == null) {
            return null;
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).toArray();
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            Object array = Array.newInstance(field.getType().getInstanceClass(), length);
            for (int i = 0; i < length; i++) {
                Array.set(array, i, fieldValue(Array.get(value, i), field));
            }
            return array;
        } else {
            return field.get(value, null);
        }
    }

    static List<Object> fieldValues(List<?> values, IOpenField field) {
        List<Object> result = new ArrayList<>(values.size());
        for (Object value : values) {
            result.add(value == null ? null : field.get(value, null));
        }
        return result;
    }

    static List<Object> flatten(List<?> list) {
        List<Object> result = new ArrayList<>();
        for (Object element : list) {
            if (element == null) {
                result.add(null);
            } else {
                if (element instanceof Collection) {
                    for (Object o : (Collection<?>) element) {
                        result.add(o);
                    }
                } else if (!element.getClass().isArray()) {
                    result.add(element);
                } else {
                    for (int i = 0; i < Array.getLength(element); i++) {
                        result.add(Array.get(element, i));
                    }
                }
            }
        }

        return result;
    }
}
