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
public final class AssignNode extends MethodBoundNode {
    private IOpenCast cast;
    private IBoundNode target, source;

    /**
     * target = source - simple assign.
     *
     * target += source - assign with operation through method.
     */
    AssignNode(ISyntaxNode syntaxNode, IBoundNode target, IBoundNode source, IMethodCaller method, IOpenCast cast) {
        super(syntaxNode, method, target, source);
        this.target = target;
        this.source = source;
        this.cast = cast;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object res = source.evaluate(env);
        if (boundMethod != null) {
            Object targetValue = target.evaluate(env);

            res = BinaryOpNode.evaluateBinaryMethod(env, new Object[] { targetValue, res }, boundMethod);
        }

        res = cast == null ? res : cast.convert(res);
        target.assign(res, env);

        return res;
    }

    @Override
    public IOpenClass getType() {
        if (boundMethod != null) {
            return super.getType();
        }
        return target.getType();
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        dependencies.addAssign(target, this);
    }
}
