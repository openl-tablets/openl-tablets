package org.openl.rules.webstudio.web.test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openl.base.INamedThing;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComplexParameterTreeNode extends ParameterDeclarationTreeNode {
    private final Logger log = LoggerFactory.getLogger(ComplexParameterTreeNode.class);
    private static final String COMPLEX_TYPE = "complex";
    private final String valueKey;
    private IOpenClass typeToCreate;
    private final ParameterRenderConfig config;

    public ComplexParameterTreeNode(ParameterRenderConfig config) {
        super(config.getFieldNameInParent(), config.getValue(), config.getType(), config.getParent(), config.getKeyField());
        this.config = config;

        Object key = null;
        if (config.getValue() != null) {
            IOpenField keyField = config.getKeyField();
            if (keyField != null) {
                key = keyField.get(config.getValue(), null);
            }
        }

        if (key == null) {
            this.valueKey = null;
        } else {
            ParameterRenderConfig childConfig = new ParameterRenderConfig.Builder(config.getType(), key).build();
            this.valueKey = ParameterTreeBuilder.createSimpleNode(childConfig).getDisplayedValue();
        }
    }

    @Override
    public String getDisplayedValue() {
        String typeName = getType().getDisplayName(INamedThing.SHORT);
        return valueKey == null ? typeName : typeName + " (" + valueKey + ")";
    }

    @Override
    public String getNodeType() {
        return COMPLEX_TYPE;
    }

    @Override
    protected LinkedHashMap<Object, ParameterDeclarationTreeNode> initChildrenMap() {
        if (isValueNull()) {
            return new LinkedHashMap<>();
        } else {
            LinkedHashMap<Object, ParameterDeclarationTreeNode> fields = new LinkedHashMap<>();
            IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
            Map<String, IOpenField> fieldMap;
            try {
                fieldMap = getType().getFields();
            } catch (LinkageError e) {
                return fields;
            }

            for (Entry<String, IOpenField> fieldEntry : fieldMap.entrySet()) {
                IOpenField field = fieldEntry.getValue();
                if (!field.isConst() && field.isReadable()) {
                    String fieldName = fieldEntry.getKey();
                    Object fieldValue;
                    IOpenClass fieldType = field.getType();

                    try {
                        fieldValue = field.get(getValue(), env);
                    } catch (RuntimeException e) {
                        // Usually this can happen only in cases when TestResult is a OpenLRuntimeException.
                        // So this field usually doesn't have any useful information.
                        // For example, it can be NotSupportedOperationException in not implemented getters.
                        log.debug("Exception while trying to get a value of a field:", e);
                        fieldType = JavaOpenClass.getOpenClass(String.class);
                        fieldValue = "Exception while trying to get a value of a field: " + e;
                    }

                    if (fieldType == JavaOpenClass.OBJECT && fieldValue != null) {
                        fieldType = JavaOpenClass.getOpenClass(fieldValue.getClass());
                    }

                    String reference = getReferenceNameToParent(fieldValue, this, "this");
                    if (reference != null) {
                        // Avoid infinite loop because of cyclic references
                        fieldType = JavaOpenClass.getOpenClass(String.class);
                        fieldValue = reference;
                    }

                    ParameterRenderConfig childConfig = new ParameterRenderConfig.Builder(fieldType, fieldValue)
                        .fieldNameInParent(fieldName)
                        .parent(this)
                        .hasExplainLinks(config.isHasExplainLinks())
                        .requestId(config.getRequestId())
                        .build();

                    fields.put(fieldName, ParameterTreeBuilder.createNode(childConfig));
                }
            }
            return fields;
        }
    }

    /**
     * Finds a reference of a field's value to any of it's parents or object
     * itself. If field value is not referenced to any of it's parents,
     * function will return null.
     *
     * @param fieldValue    field value
     * @param parentObject  object that contains a field
     * @param referenceName reference
     * @return reference name to a parent or null
     */
    private String getReferenceNameToParent(Object fieldValue, ParameterDeclarationTreeNode parentObject,
                                            String referenceName) {
        // Check reference, not value - that's why "==" instead of "equals".
        if (parentObject.getValue() == fieldValue) {
            return referenceName;
        }

        if (parentObject.getParent() == null) {
            return null;
        }

        return getReferenceNameToParent(fieldValue, parentObject.getParent(), referenceName + ".parent");
    }

    @Override
    protected Object constructValueInternal() {
        Object value = getValue();

        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        for (Entry<Object, ParameterDeclarationTreeNode> fieldEntry : getChildrenMap().entrySet()) {
            if (!(fieldEntry.getValue() instanceof UnmodifiableParameterTreeNode)) {
                String fieldName = (String) fieldEntry.getKey();
                IOpenField field = getType().getField(fieldName);
                if (field.isWritable()) {
                    try {
                        field.set(value, fieldEntry.getValue().getValueForced(), env);
                    } catch (Exception e) {
                        // Can throw UnsupportedOperationException for example or doesn't accept nulls.
                        log.debug("Exception while trying to set a value of a field:", e);
                    }
                }
            }
        }
        return value;
    }

    @Override
    public void replaceChild(ParameterDeclarationTreeNode oldNode, ParameterDeclarationTreeNode newNode) {
        super.replaceChild(oldNode, newNode);

        IOpenField field = getType().getField(newNode.getName());
        if (!field.isConst() && field.isWritable()) {
            try {
                field.set(getValue(), newNode.getValue(), new SimpleVM().getRuntimeEnv());
            } catch (Exception e) {
                // Can throw NotSupportedOperationException for example.
                log.debug("Exception while trying to set a value of a field:", e);
            }
        }
    }

    public boolean isBaseType() {
        IOpenClass type = getType();
        return type == JavaOpenClass.OBJECT || type.isAbstract();
    }

    public IOpenClass getTypeToCreate() {
        return typeToCreate;
    }

    public void setTypeToCreate(IOpenClass typeToCreate) {
        this.typeToCreate = typeToCreate;
    }

    public boolean isDisposeRestricted() {
        return getType().getInstanceClass().isAnnotationPresent(RestrictDispose.class);
    }
}
