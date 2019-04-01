package org.openl.rules.webstudio.web.test;

import java.util.LinkedHashMap;

import org.openl.rules.table.formatters.FormattersManager;
import org.openl.types.IOpenClass;

public class UnmodifiableParameterTreeNode extends ParameterDeclarationTreeNode {

    private static final String UNMODIFIABLE_TYPE = "unmodifiable";

    private String warnMessage;

    public UnmodifiableParameterTreeNode(String fieldName,
            Object value,
            IOpenClass fieldType,
            ParameterDeclarationTreeNode parent) {
        super(fieldName, value, fieldType, parent);
    }

    @Override
    public String getDisplayedValue() {
        Object value = getValue();
        return FormattersManager.format(value);
    }

    @Override
    public String getNodeType() {
        return UNMODIFIABLE_TYPE;
    }

    @Override
    protected LinkedHashMap<Object, ParameterDeclarationTreeNode> initChildrenMap() {
        return new LinkedHashMap<>();
    }

    @Override
    protected Object constructValueInternal() {
        return getValue();
    }

    public String getWarnMessage() {
        return warnMessage;
    }

    public void setWarnMessage(String warnMessage) {
        this.warnMessage = warnMessage;
    }
}
