package org.openl.rules.webstudio.web.test;

import java.util.LinkedHashMap;

import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.types.IOpenClass;

public class UnmodifiableParameterTreeNode extends ParameterDeclarationTreeNode {

    public static final String UNMODIFIABLE_TYPE = "unmodifiable";
    
    private String warnMessage;

    public UnmodifiableParameterTreeNode(String fieldName,
            Object value,
            IOpenClass fieldType,
            ParameterDeclarationTreeNode parent) {
        super(fieldName, value, fieldType, parent);
    }

    public UnmodifiableParameterTreeNode(ParameterWithValueDeclaration paramDescription, ParameterDeclarationTreeNode parent) {
        super(paramDescription, parent);
    }

    @Override
    public String getDisplayedValue() {
        Object value = getValue();
        return FormattersManager.getFormatter(value).format(value);
    }
    
    @Override
    public String getNodeType() {
        return UNMODIFIABLE_TYPE;
    }

    @Override
    protected LinkedHashMap<Object, ParameterDeclarationTreeNode> initChildernMap() {
        return new LinkedHashMap<Object, ParameterDeclarationTreeNode>();
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
