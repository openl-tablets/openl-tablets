package org.openl.rules.webstudio.web.test;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.openl.base.INameSpacedThing;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

public class ComplexParameterTreeNode extends ParameterDeclarationTreeNode {
    public static final String COMPLEX_TYPE = "complex";

    public ComplexParameterTreeNode(String fieldName, Object value, IOpenClass fieldType, ParameterDeclarationTreeNode parent) {
        super(fieldName, value, fieldType, parent);
    }

    public ComplexParameterTreeNode(ParameterWithValueDeclaration paramDescription, ParameterDeclarationTreeNode parent) {
        super(paramDescription, parent);
    }

    @Override
    public String getDisplayedValue() {
        return getType().getDisplayName(INameSpacedThing.SHORT);
    }

    @Override
    public String getNodeType() {
        return COMPLEX_TYPE;
    }

    @Override
    protected LinkedHashMap<Object, ParameterDeclarationTreeNode> initChildernMap() {
        if (isValueNull()) {
            return new LinkedHashMap<Object, ParameterDeclarationTreeNode>();
        } else {
            LinkedHashMap<Object, ParameterDeclarationTreeNode> fields = new LinkedHashMap<Object, ParameterDeclarationTreeNode>();
            IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
            for (Entry<String, IOpenField> fieldEntry : getType().getFields().entrySet()) {
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
        for (Entry<Object, ParameterDeclarationTreeNode> fieldEntry : getChildernMap().entrySet()) {
            String fieldName = (String) fieldEntry.getKey();
            IOpenField field = getType().getField(fieldName);
            field.set(value, fieldEntry.getValue().getValueForced(), env);
        }
        return value;
    }
}
