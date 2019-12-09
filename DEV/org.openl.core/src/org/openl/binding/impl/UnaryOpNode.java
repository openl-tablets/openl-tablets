/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;

/**
 * @author snshor
 *
 */
public class UnaryOpNode extends MethodBoundNode {
    /**
     * @param syntaxNode
     * @param child
     * @param method
     */
    public UnaryOpNode(ISyntaxNode syntaxNode, IBoundNode[] child, IMethodCaller method) {
        super(syntaxNode, child, method);
    }
}
