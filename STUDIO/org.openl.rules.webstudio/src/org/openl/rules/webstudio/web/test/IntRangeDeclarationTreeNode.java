package org.openl.rules.webstudio.web.test;


import lombok.extern.slf4j.Slf4j;

import org.openl.rules.helpers.IntRange;
import org.openl.types.IOpenClass;
import org.openl.util.StringUtils;

@Slf4j
public class IntRangeDeclarationTreeNode extends SimpleParameterTreeNode {

    public IntRangeDeclarationTreeNode(String fieldName,
                                       Object value,
                                       IOpenClass fieldType,
                                       ParameterDeclarationTreeNode parent) {
        super(fieldName, value, fieldType, parent);
    }

    @Override
    public void setValueForEdit(String value) {
        if (StringUtils.isBlank(value)) {
            setValueForced(null);
        } else {
            try {
                setValueForced(new IntRange(value));
            } catch (RuntimeException e) {
                // TODO message on UI
                log.warn("Failed to set '{}' value to field [{}]", value, getName(), e);
            }
        }
    }
}
