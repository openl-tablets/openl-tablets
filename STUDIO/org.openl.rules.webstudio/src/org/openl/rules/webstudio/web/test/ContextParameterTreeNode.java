package org.openl.rules.webstudio.web.test;

import java.util.Iterator;
import java.util.LinkedHashMap;

import org.openl.types.IOpenClass;

public class ContextParameterTreeNode extends ComplexParameterTreeNode {
    public ContextParameterTreeNode(String fieldName,
            Object value,
            IOpenClass fieldType,
            ParameterDeclarationTreeNode parent, String requestId) {
        super(fieldName, value, fieldType, parent, null, requestId);
    }

    @Override
    public String getDisplayedValue() {
        return "Context";
    }

    @Override
    protected LinkedHashMap<Object, ParameterDeclarationTreeNode> initChildrenMap() {
        LinkedHashMap<Object, ParameterDeclarationTreeNode> children = super.initChildrenMap();

        Iterator<ParameterDeclarationTreeNode> iterator = children.values().iterator();
        while (iterator.hasNext()) {
            ParameterDeclarationTreeNode node = iterator.next();
            if (node.getValue() == null) {
                iterator.remove();
            }
        }
        return children;
    }
}
