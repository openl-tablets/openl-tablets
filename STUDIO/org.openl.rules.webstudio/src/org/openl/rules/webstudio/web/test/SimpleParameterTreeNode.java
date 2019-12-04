package org.openl.rules.webstudio.web.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import org.openl.domain.IDomain;
import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaEnumDomain;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleParameterTreeNode extends ParameterDeclarationTreeNode {
    private final Logger log = LoggerFactory.getLogger(SimpleParameterTreeNode.class);

    public SimpleParameterTreeNode(String fieldName,
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
        IOpenClass type = getType();
        Class<?> instanceClass = type.getInstanceClass();
        if (boolean.class == instanceClass) {
            return "boolean";
        } else if (ClassUtils.isAssignable(instanceClass,
            Boolean.class) || type instanceof DomainOpenClass || type instanceof JavaEnumDomain) {
            return "selection";
        } else if (ClassUtils.isAssignable(instanceClass, Date.class)) {
            return "date";
        } else if (ClassUtils.isAssignable(instanceClass,
            Number.class) || byte.class == instanceClass || short.class == instanceClass || int.class == instanceClass || long.class == instanceClass || float.class == instanceClass || double.class == instanceClass) {
            return "number";
        } else {
            return "string";
        }
    }

    public List<String> getValuesForSelect() {
        IOpenClass type = getType();
        if (type.getInstanceClass() == Boolean.class) {
            return Arrays.asList("", "true", "false");
        }

        IDomain<?> domain = type.getDomain();
        if (domain != null) {
            List<String> result = new ArrayList<>();
            result.add("");
            for (Object o : domain) {
                result.add(FormattersManager.format(o));
            }
            return result;
        }

        return Collections.emptyList();
    }

    public void setValueForEdit(String value) {
        if (StringUtils.isBlank(value)) {
            setValueForced(null);
        } else {
            try {
                IString2DataConvertor convertor = String2DataConvertorFactory
                    .getConvertor(getType().getInstanceClass());
                setValueForced(convertor.parse(value, null));
            } catch (Exception e) {
                // TODO message on UI
                log.warn("Failed to set '{}' value to field [{}]", value, getName(), e);
            }
        }
    }

    @Override
    protected LinkedHashMap<Object, ParameterDeclarationTreeNode> initChildrenMap() {
        return new LinkedHashMap<>();
    }

    @Override
    protected Object constructValueInternal() {
        return getValue();
    }

}
