package org.openl.rules.webstudio.web.test;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.openl.base.INameSpacedThing;
import org.openl.rules.testmethod.ExecutionParamDescription;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

public class ComplexFieldNode extends FieldDescriptionTreeNode {
    public static final String COMPLEX_TYPE = "complex";

    public ComplexFieldNode(String fieldName, Object value, IOpenClass fieldType, FieldDescriptionTreeNode parent) {
        super(fieldName, value, fieldType, parent);
    }

    public ComplexFieldNode(ExecutionParamDescription paramDescription, FieldDescriptionTreeNode parent) {
        super(paramDescription, parent);
    }

    @Override
    public String getDisplayedValue() {
        return getFieldType().getDisplayName(INameSpacedThing.SHORT);
    }

    @Override
    public String getNodeType() {
        return COMPLEX_TYPE;
    }

    @Override
    protected LinkedHashMap<Object, FieldDescriptionTreeNode> initChildernMap() {
        if (isValueNull()) {
            return new LinkedHashMap<Object, FieldDescriptionTreeNode>();
        } else {
            LinkedHashMap<Object, FieldDescriptionTreeNode> fields = new LinkedHashMap<Object, FieldDescriptionTreeNode>();
            IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
            for (Entry<String, IOpenField> fieldEntry : getFieldType().getFields().entrySet()) {
                IOpenField field = fieldEntry.getValue();
                if (!field.isConst()) {
                    String fieldName = fieldEntry.getKey();
                    fields.put(fieldName,
                        TestTreeBuilder.createNode(field.getType(), field.get(getValue(), env), fieldName, this));
                }
            }
            return fields;
        }
    }

    @Override
    protected Object constructValueInternal() {
        Object value = getValue();

        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        for (Entry<Object, FieldDescriptionTreeNode> fieldEntry : getChildernMap().entrySet()) {
            String fieldName = (String) fieldEntry.getKey();
            IOpenField field = getFieldType().getField(fieldName);
            field.set(value, fieldEntry.getValue().getValueForced(), env);
        }
        return value;
    }
}
