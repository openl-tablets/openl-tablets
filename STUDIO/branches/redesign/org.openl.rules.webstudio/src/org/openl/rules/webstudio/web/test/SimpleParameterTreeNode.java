package org.openl.rules.webstudio.web.test;

import java.util.LinkedHashMap;

import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.types.IOpenClass;

public class SimpleParameterTreeNode extends ParameterDeclarationTreeNode {
    public static final String SIMPLE_TYPE = "simple";

    public SimpleParameterTreeNode(String fieldName, Object value, IOpenClass fieldType, ParameterDeclarationTreeNode parent) {
        super(fieldName, value, fieldType, parent);
    }

    public SimpleParameterTreeNode(ParameterWithValueDeclaration paramDescription, ParameterDeclarationTreeNode parent) {
        super(paramDescription, parent);
    }

    @Override
    public String getDisplayedValue() {
        Object value = getValue();
        return FormattersManager.getFormatter(value).format(value);
    }

    @Override
    public String getNodeType() {
        return SIMPLE_TYPE;
    }

    public void setDisplayedValue(String value) {
        IString2DataConvertor convertor = String2DataConvertorFactory.getConvertor(getType().getInstanceClass());
        try {
            setValueForced(convertor.parse(value, null, null));
        } catch (Exception e) {
            //TODO message on UI
        }
    }

    @Override
    protected LinkedHashMap<Object, ParameterDeclarationTreeNode> initChildernMap() {
        return new LinkedHashMap<Object, ParameterDeclarationTreeNode>();
    }

    @Override
    protected Object constructValueInternal() {
        return getValue();
    }

}
