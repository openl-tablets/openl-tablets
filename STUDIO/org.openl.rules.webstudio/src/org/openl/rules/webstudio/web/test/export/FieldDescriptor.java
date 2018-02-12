package org.openl.rules.webstudio.web.test.export;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

class FieldDescriptor {
    private final IOpenField field;
    private final List<FieldDescriptor> children;

    static List<FieldDescriptor> nonEmptyFields(IOpenClass type, List<Object> values) {
        if (type.isArray()) {
            type = type.getComponentClass();
        }

        if (type.isSimple()) {
            return null;
        }

        List<FieldDescriptor> result = new ArrayList<>();

        Map<String, IOpenField> fields = type.getFields();

        for (Map.Entry<String, IOpenField> entry : fields.entrySet()) {
            IOpenField field = entry.getValue();
            IOpenClass fieldType = field.getType();

            values = ExportUtils.flatten(values);
            for (Object value : values) {
                Object fieldValue = value == null ? null : field.get(value, null);
                if (fieldValue != null && (!field.getType().isArray() || Array.getLength(fieldValue) > 0)) {

                    List<FieldDescriptor> children = nonEmptyFields(fieldType, ExportUtils.fieldValues(values, field));

                    result.add(new FieldDescriptor(field, children));
                    break;
                }
            }
        }
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

    /**
     * Returns leaf node count
     *
     * @return the count of leaf node or 1 if the node doesn't contain children
     */
    public int getWidth() {
        if (children == null) {
            return 1;
        }

        int width = 0;
        for (FieldDescriptor child : children) {
            width += child.getWidth();
        }

        return width;
    }
}
