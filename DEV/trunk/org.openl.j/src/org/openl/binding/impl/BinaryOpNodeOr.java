/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class BinaryOpNodeOr extends ABoundNode {
    /**
     * @param syntaxNode
     * @param child
     * @param method
     */
    public BinaryOpNodeOr(ISyntaxNode syntaxNode, IBoundNode[] child) {
        super(syntaxNode, child);
    }

    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {

        Boolean b1 = (Boolean) children[0].evaluate(env);
        if (!b1.booleanValue()) {
            return children[1].evaluate(env);
        }
        return Boolean.TRUE;
    }

    public IOpenClass getType() {

        return JavaOpenClass.BOOLEAN;
    }

    @Override
    public boolean isLiteralExpressionParent() {
        return true;
    }

}
