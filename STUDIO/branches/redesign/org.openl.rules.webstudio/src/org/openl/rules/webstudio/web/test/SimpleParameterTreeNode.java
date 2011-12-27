package org.openl.rules.webstudio.web.test;

import java.util.LinkedHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.types.IOpenClass;

public class SimpleParameterTreeNode extends ParameterDeclarationTreeNode {
    private static final Log LOG = LogFactory.getLog(SimpleParameterTreeNode.class);

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
        return FormattersManager.getFormatter(value).format(value);
    }
    
    public String getValueForEdit() {
        Object value = getValue();
        if (value != null) {
            return FormattersManager.getFormatter(value).format(value);
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
                setValueForced(convertor.parse(value, null, null));
            } catch (Exception e) {
                // TODO message on UI
                LOG.warn(String.format("Failed to set \"%s\" value to field [%s]", value, getName()), e);
            }
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
