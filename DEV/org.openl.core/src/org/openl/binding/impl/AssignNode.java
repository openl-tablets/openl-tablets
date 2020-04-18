package org.openl.binding.impl;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class AssignNode extends MethodBoundNode {
    private IOpenCast cast;

    /**
     * @param syntaxNode
     * @param child
     * @param method
     */
    public AssignNode(ISyntaxNode syntaxNode, IBoundNode[] child, IMethodCaller method, IOpenCast cast) {
        super(syntaxNode, method, child);
        this.cast = cast;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object res;
        if (boundMethod != null) {
            Object[] pars = evaluateChildren(env);

            res = BinaryOpNode.evaluateBinaryMethod(env, pars, boundMethod);
        } else {
            res = children[1].evaluate(env);
        }

        res = cast == null ? res : cast.convert(res);
        children[0].assign(res, env);

        return res;
    }

    @Override
    public IOpenClass getType() {
        if (boundMethod != null) {
            return super.getType();
        }
        return children[0].getType();
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        dependencies.addAssign(children[0], this);
    }

}
