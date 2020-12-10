package org.openl.rules.webstudio.web.test;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class ContextParameterTreeNode extends ComplexParameterTreeNode {
    public ContextParameterTreeNode(ParameterRenderConfig config) {
        super(config);
    }

    @Override
    public String getDisplayedValue() {
        return "Context";
    }

    @Override
    protected LinkedHashMap<Object, ParameterDeclarationTreeNode> initChildrenMap() {
        LinkedHashMap<Object, ParameterDeclarationTreeNode> children = super.initChildrenMap();

        children.values().removeIf(node -> node.getValue() == null);
        return children;
    }
}
