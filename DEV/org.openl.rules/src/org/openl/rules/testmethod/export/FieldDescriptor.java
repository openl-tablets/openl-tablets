package org.openl.rules.testmethod.export;

import static org.openl.types.java.JavaOpenClass.CLASS;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

import org.openl.binding.impl.CastToWiderType;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.util.ClassUtils;
import org.openl.util.OpenClassUtils;

class FieldDescriptor {
    private final IOpenField field;
    private final List<FieldDescriptor> children;

    private static final BiPredicate<IOpenClass, Object> SKIP_EMPTY_PARAMETER_FILTER = (fieldType,
                                                                                        fieldValue) -> fieldValue != null && (!fieldType.isArray() || Array.getLength(fieldValue) > 0);

    /**
     * Find fields from all test results. WARNING: This method is very expensive! Don't invoke it too often.
     *
     * @param type                Type of a checking object
     * @param values              All possible values for a given type. Is got from test result.
     * @param skipEmptyParameters Boolean indication whether to skip empty parameters in result.
     * @return fields from all test results(values) based on boolean flag.
     */
    static List<FieldDescriptor> nonEmptyFields(IOpenClass type, List<?> values, Boolean skipEmptyParameters) {
        Set<String> coveredFields = new HashSet<>();
        return nonEmptyFieldsForFlatten(type, ExportUtils.flatten(values), skipEmptyParameters, coveredFields);
    }

    private static List<FieldDescriptor> nonEmptyFieldsForFlatten(IOpenClass type,
                                                                  List<?> values,
                                                                  Boolean skipEmptyParameters,
                                                                  Set<String> coveredFields) {
        type = OpenClassUtils.getRootComponentClass(type);

        if (type.isSimple() || ClassUtils.isAssignable(type.getInstanceClass(), Map.class)) {
            return null;
        }

        List<FieldDescriptor> result = new ArrayList<>();

        for (IOpenField field : type.getFields()) {
            if (field.getType().equals(CLASS)) {
                continue;
            }
            IOpenClass fieldType = field.getType();
            List<Object> childFieldValues = ExportUtils.flatten(ExportUtils.fieldValues(values, field));

            for (Object value : values) {
                Object fieldValue = value == null ? null : field.get(value, null);
                String fieldName = field.getDeclaringClass().toString() + field.getName() + fieldValue;
                if (!coveredFields.contains(fieldName)) {
                    coveredFields.add(fieldName);
                    if (!skipEmptyParameters || SKIP_EMPTY_PARAMETER_FILTER.test(fieldType, fieldValue)) {
                        if (fieldValue instanceof Collection) {
                            fieldType = CastToWiderType.defineCollectionWiderType((Collection<?>) fieldValue);
                        }
                        List<FieldDescriptor> children = nonEmptyFieldsForFlatten(fieldType,
                                childFieldValues,
                                skipEmptyParameters,
                                coveredFields);
                        result.add(new FieldDescriptor(field, children));

                        break;
                    }
                }
            }
        }

        result.sort(Comparator.comparing(FieldDescriptor::isArray));

        return result;
    }

    private FieldDescriptor(IOpenField field, List<FieldDescriptor> children) {
        this.field = field;
        this.children = children;
    }

    public IOpenField getField() {
        return field;
    }

    public List<FieldDescriptor> getChildren() {
        return children;
    }

    public boolean isArray() {
        return field.getType().isArray();
    }

    /**
     * Returns leaf node count
     *
     * @return the count of leaf node or 1 if the node does not contain children
     */
    public int getLeafNodeCount() {
        if (children == null) {
            return 1;
        }

        int width = 0;
        for (FieldDescriptor child : children) {
            width += child.getLeafNodeCount();
        }

        return width;
    }

    /**
     * Returns maximum array elements of the the field value from a given parent object
     *
     * @param object parent object (contains this field)
     * @return max array size if exist or 1 if there are no arrays
     */
    public int getMaxArraySize(Object object) {
        if (object == null) {
            return 1;
        }

        if (object.getClass().isArray()) {
            int count = Array.getLength(object);
            int height = 0;
            for (int i = 0; i < count; i++) {
                height += getMaxArraySize(Array.get(object, i));
            }
            return height == 0 ? 1 : height;
        }

        Object fieldValue = ExportUtils.fieldValue(object, getField());
        return calcArraySizeForChild(fieldValue);
    }

    private int calcArraySizeForChild(Object fieldValue) {
        if (fieldValue == null) {
            return 1;
        }

        // In excel each element contains at least one cell even if it's empty.
        if (children == null) {
            return 1;
        }

        if (fieldValue.getClass().isArray()) {
            int size = 0;
            int count = Array.getLength(fieldValue);
            for (int i = 0; i < count; i++) {
                size += calcArraySizeForChild(Array.get(fieldValue, i));
            }
            return size == 0 ? 1 : size;
        } else {

            int max = 1;
            for (FieldDescriptor child : children) {
                int childSize = child.getMaxArraySize(fieldValue);
                if (childSize > max) {
                    max = childSize;
                }
            }
            return max;
        }
    }

}
