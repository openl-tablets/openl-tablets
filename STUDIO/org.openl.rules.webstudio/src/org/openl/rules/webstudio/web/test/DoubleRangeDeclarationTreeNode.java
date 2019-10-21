package org.openl.rules.webstudio.web.test;

import org.openl.rules.helpers.DoubleRange;
import org.openl.types.IOpenClass;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoubleRangeDeclarationTreeNode extends SimpleParameterTreeNode {
    private final Logger log = LoggerFactory.getLogger(DoubleRangeDeclarationTreeNode.class);

    public DoubleRangeDeclarationTreeNode(String fieldName,
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
                setValueForced(new DoubleRange(value));
            } catch (RuntimeException e) {
                // TODO message on UI
                log.warn("Failed to set '{}' value to field [{}]", value, getName(), e);
            }
        }
    }
}
