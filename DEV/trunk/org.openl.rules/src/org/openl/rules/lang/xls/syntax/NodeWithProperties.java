package org.openl.rules.lang.xls.syntax;

import org.openl.IOpenSourceCodeModule;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.NaryNode;
import org.openl.util.text.ILocation;

public class NodeWithProperties extends NaryNode {

    public NodeWithProperties(String type, ILocation pos, ISyntaxNode[] nodes, IOpenSourceCodeModule module) {
        super(type, pos, nodes, module);
    }

    ITableProperties nodeProperties;

    public ITableProperties getNodeProperties() {
        return nodeProperties;
    }

    public void setNodeProperties(ITableProperties nodeProperties) {
        this.nodeProperties = nodeProperties;
    }

    /**
     * Loops through parent nodes until finds a NodeWithProperties or null
     * 
     * @return
     */
    static public NodeWithProperties getParentNodeWithproperties(ISyntaxNode node) {
        ISyntaxNode parent = node;

        while (true) {

            parent = parent.getParent();
            if (parent == null)
                return null;
            if (parent instanceof NodeWithProperties) {
                NodeWithProperties found = (NodeWithProperties) parent;
                return found;

            }
        }

    }
}
