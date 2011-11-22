package org.openl.rules.webstudio.web.test;

import java.util.LinkedHashMap;

import javax.faces.application.FacesMessage;
import javax.faces.validator.ValidatorException;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.testmethod.ExecutionParamDescription;
import org.openl.types.IOpenClass;

public class SimpleFieldNode extends FieldDescriptionTreeNode {
    public static final String SIMPLE_TYPE = "simple";

    public SimpleFieldNode(String fieldName, Object value, IOpenClass fieldType, FieldDescriptionTreeNode parent) {
        super(fieldName, value, fieldType, parent);
    }

    public SimpleFieldNode(ExecutionParamDescription paramDescription, FieldDescriptionTreeNode parent) {
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
        IString2DataConvertor convertor = String2DataConvertorFactory.getConvertor(getFieldType().getInstanceClass());
        try {
            setValueForced(convertor.parse(value, null, null));
        } catch (Exception e) {
            //TODO message on UI
        }
    }

    @Override
    protected LinkedHashMap<Object, FieldDescriptionTreeNode> initChildernMap() {
        return new LinkedHashMap<Object, FieldDescriptionTreeNode>();
    }

    @Override
    protected Object constructValueInternal() {
        return getValue();
    }

}
