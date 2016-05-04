package org.openl.rules.webstudio.web.test;

import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.types.IOpenClass;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;

public class SimpleParameterTreeNode extends ParameterDeclarationTreeNode {
    private final Logger log = LoggerFactory.getLogger(SimpleParameterTreeNode.class);

    public static final String SIMPLE_TYPE = "simple";

    public SimpleParameterTreeNode(String fieldName,
                                   Object value,
                                   IOpenClass fieldType,
                                   ParameterDeclarationTreeNode parent) {
        super(fieldName, value, fieldType, parent);
    }

    public SimpleParameterTreeNode(ParameterWithValueDeclaration paramDescription, ParameterDeclarationTreeNode parent) {
        super(paramDescription, parent);
    }

    @Override
    public String getDisplayedValue() {
        Object value = getValue();
        return FormattersManager.format(value);
    }

    public String getValueForEdit() {
        Object value = getValue();
        if (value != null) {
            return FormattersManager.format(value);
        } else {
            return "";
        }
    }

    @Override
    public String getNodeType() {
        return SIMPLE_TYPE;
    }

    public void setValueForEdit(String value) {
        if (StringUtils.isBlank(value)) {
            setValueForced(null);
        } else {
            try {
                IString2DataConvertor convertor = String2DataConvertorFactory.getConvertor(getType().getInstanceClass());
                setValueForced(convertor.parse(value, null));
            } catch (Exception e) {
                // TODO message on UI
                log.warn("Failed to set \"{}\" value to field [{}]", value, getName(), e);
            }
        }
    }

    @Override
    protected LinkedHashMap<Object, ParameterDeclarationTreeNode> initChildrenMap() {
        return new LinkedHashMap<Object, ParameterDeclarationTreeNode>();
    }

    @Override
    protected Object constructValueInternal() {
        return getValue();
    }

}
