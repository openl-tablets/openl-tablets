package org.openl.syntax.impl;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ILiteralNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.text.ILocation;

public class CompositeLiteralNode extends NaryNode  implements ILiteralNode{

    public CompositeLiteralNode(String type, ILocation pos, ISyntaxNode[] nodes, IOpenSourceCodeModule module) {
        super(type, pos, nodes, module);
    }

    public String getImage() {
        StringBuilder sb = new StringBuilder();
        ISyntaxNode[] nodes = getNodes(); 
        for (int i = 0; i < nodes.length; i++) {
            if (i > 0)
                sb.append(' ');
            sb.append(((ILiteralNode)nodes[i]).getImage());
        }
        return sb.toString();
    }

}
