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
import org.openl.types.NullOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class QMarkNode extends ABoundNode {

    /**
     * @param syntaxNode
     * @param children
     */
    public QMarkNode(ISyntaxNode syntaxNode, IBoundNode[] children) {
        super(syntaxNode, children);
    }

    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        Boolean res = (Boolean) children[0].evaluate(env);

        return res.booleanValue() ? children[1].evaluate(env) : children[2].evaluate(env);

    }

    public IOpenClass getType() {
        if (NullOpenClass.the.equals(children[1].getType())){
            return children[2].getType();
        }
        return children[1].getType();
    }

    @Override
    public boolean isLiteralExpressionParent() {
        return true;
    }

}
