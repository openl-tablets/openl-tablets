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
public class BinaryNode extends ASyntaxNode {
    private ISyntaxNode left;
    private ISyntaxNode right;

    public BinaryNode(String type, ILocation pos, ISyntaxNode left, ISyntaxNode right, IOpenSourceCodeModule module) {
        super(type, pos, module);
        this.left = left;
        this.right = right;
        left.setParent(this);
        right.setParent(this);
    }

    public ISyntaxNode getChild(int i) {
        if (i == 0) {
            return left;
        }
        if (i == 1) {
            return right;
        }
        throw new RuntimeException("BinaryNode has only two children, not " + (i + 1));
    }

    public int getNumberOfChildren() {
        return 2;
    }

}
