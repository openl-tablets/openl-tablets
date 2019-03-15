package org.openl.syntax.impl;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.text.ILocation;

final class CompositeLiteralNode extends NaryNode  {

    CompositeLiteralNode(String type, ILocation pos, ISyntaxNode[] nodes, IOpenSourceCodeModule module) {
        super(type, pos, nodes, module);
    }

    public String getText() {
        StringBuilder sb = new StringBuilder();
        ISyntaxNode[] nodes = getNodes(); 
        for (int i = 0; i < nodes.length; i++) {
            if (i > 0)
                sb.append(' ');
            sb.append(nodes[i].getText());
        }
        return sb.toString();
    }

}
