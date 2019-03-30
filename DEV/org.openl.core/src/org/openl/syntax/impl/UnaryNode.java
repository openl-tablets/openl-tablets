/*
 * Created on May 12, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.text.ILocation;

/**
 * @author snshor
 *
 */
public class UnaryNode extends ASyntaxNode {
    private ISyntaxNode left;

    public UnaryNode(String type, ILocation pos, ISyntaxNode left, IOpenSourceCodeModule module) {
        super(type, pos, module);
        this.left = left;
        left.setParent(this);
    }

    @Override
    public ISyntaxNode getChild(int i) {
        if (i == 0) {
            return left;
        }
        throw new RuntimeException("UnaryOp has only one child, not " + (i + 1));
    }

    @Override
    public int getNumberOfChildren() {
        return 1;
    }

}
