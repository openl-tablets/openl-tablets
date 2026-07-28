package org.openl.binding.impl;

import lombok.Getter;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * <ul>
 * <li>FALSE or FALSE = FALSE</li>
 * <li>FALSE or TRUE = TRUE</li>
 * <li>TRUE or FALSE = TRUE</li>
 * <li>TRUE or TRUE = TRUE</li>
 * <li>FALSE or NULL = NULL</li>
 * <li>NULL or FALSE = NULL</li>
 * <li>TRUE or NULL = TRUE</li>
 * <li>NULL or TRUE = TRUE</li>
 * <li>NULL or NULL = NULL</li>
 * </ul>
 *
 * @author Yury Molchan
 */
public class BinaryOpNodeOr extends ABoundNode {

    @Getter
    private final IBoundNode left;
    @Getter
    private final IBoundNode right;

    BinaryOpNodeOr(ISyntaxNode syntaxNode, IBoundNode left, IBoundNode right) {
        super(syntaxNode, left, right);
        this.left = left;
        this.right = right;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {

        var res1 = left.evaluate(env);
        if (Boolean.TRUE.equals(res1)) {
            return Boolean.TRUE;
        }
        var res2 = right.evaluate(env);
        if (Boolean.TRUE.equals(res2)) {
            return Boolean.TRUE;
        }
        if (res1 == null || res2 == null) {
            return null;
        } else {
            return Boolean.FALSE;
        }
    }

    @Override
    public IOpenClass getType() {
        return JavaOpenClass.getOpenClass(Boolean.class);
    }

}
