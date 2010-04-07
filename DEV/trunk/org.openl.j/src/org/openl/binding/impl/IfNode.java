/*
 * Created on Jun 17, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class IfNode extends ABoundNode {

    /**
     * @param syntaxNode
     * @param children
     */
    public IfNode(ISyntaxNode syntaxNode, IBoundNode[] children) {
        super(syntaxNode, children);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#evaluate(org.openl.vm.IRuntimeEnv)
     */
    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        Boolean res = (Boolean) children[0].evaluate(env);

        if (res.booleanValue()) {
            children[1].evaluate(env);
        } else if (children.length > 2) {
            children[2].evaluate(env);
        }

        return null;
    }

    public IOpenClass getType() {
        // return NullOpenClass.the;
        // TODO use both branches
        return children[1].getType();
    }
    @Override
    public boolean isLiteralExpressionParent() {
        return true;
    }

}
