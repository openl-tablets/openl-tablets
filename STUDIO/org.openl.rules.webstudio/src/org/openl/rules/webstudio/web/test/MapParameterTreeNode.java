package org.openl.rules.webstudio.web.test;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

public class MapParameterTreeNode extends CollectionParameterTreeNode {
    public MapParameterTreeNode(String fieldName,
            Object value,
            IOpenClass fieldType,
            ParameterDeclarationTreeNode parent, IOpenField previewField, boolean hasExplainLinks) {
        super(fieldName, value, fieldType, parent, previewField, hasExplainLinks);
    }

    @Override
    protected Object getKeyFromElementNum(Object elementNum) {
        if (elementNum == null) {
            // Some maps doesn't support null as a key
            return "";
        }
        return getChild(elementNum).getChild("key").getValue();
    }

    @Override
    protected Object getEmptyValue() {
        // Some maps doesn't support null as a value
        return "";
    }
}
