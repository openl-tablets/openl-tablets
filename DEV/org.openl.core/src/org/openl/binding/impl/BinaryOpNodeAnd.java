package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * <ul>
 * <li>FALSE and FALSE = FALSE</li>
 * <li>FALSE and TRUE = FALSE</li>
 * <li>TRUE and FALSE = FALSE</li>
 * <li>TRUE and TRUE = TRUE</li>
 * <li>FALSE and NULL = FALSE</li>
 * <li>NULL and FALSE = FALSE</li>
 * <li>TRUE and NULL = NULL</li>
 * <li>NULL and TRUE = NULL</li>
 * <li>NULL and NULL = NULL</li>
 * </ul>
 *
 * @author Yury Molchan
 */
public class BinaryOpNodeAnd extends ABoundNode {

    private final IBoundNode left;
    private final IBoundNode right;

    BinaryOpNodeAnd(ISyntaxNode syntaxNode, IBoundNode left, IBoundNode right) {
        super(syntaxNode, left, right);
        this.left = left;
        this.right = right;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {

        Object res1 = left.evaluate(env);
        if (Boolean.FALSE.equals(res1)) {
            return Boolean.FALSE;
        }
        Object res2 = right.evaluate(env);
        if (Boolean.FALSE.equals(res2)) {
            return Boolean.FALSE;
        }
        if (res1 == null || res2 == null) {
            return null;
        } else {
            return Boolean.TRUE;
        }
    }

    @Override
    public IOpenClass getType() {
        return JavaOpenClass.getOpenClass(Boolean.class);
    }

}
