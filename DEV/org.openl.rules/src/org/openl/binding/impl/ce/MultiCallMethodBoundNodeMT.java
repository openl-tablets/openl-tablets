package org.openl.binding.impl.ce;

import java.lang.reflect.Array;
import java.util.List;

import org.openl.binding.IBoundNode;
import org.openl.rules.core.ce.Runnable;
import org.openl.rules.core.ce.ServiceMT;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.vm.IRuntimeEnv;

public class MultiCallMethodBoundNodeMT extends MultiCallMethodBoundNode {

    public MultiCallMethodBoundNodeMT(ISyntaxNode syntaxNode,
            IBoundNode[] children,
            IMethodCaller singleParameterMethod,
            List<Integer> arrayArgArgumentList) {
        super(syntaxNode, children, singleParameterMethod, arrayArgArgumentList);
    }

    @Override
    protected void invokeMethodAndSetResultToArray(IMethodCaller methodCaller,
            Object target,
            IRuntimeEnv env,
            Object[] callParameters,
            Object results,
            int index,
            int resultLength) {
        if (resultLength <= 1) {
            super.invokeMethodAndSetResultToArray(methodCaller,
                target,
                env,
                callParameters,
                results,
                index,
                resultLength);
        } else {
            InvokeMethodAndSetResultToArrayRunnable runnable = new InvokeMethodAndSetResultToArrayRunnable(methodCaller,
                target,
                callParameters.clone(),
                results,
                index);
            ServiceMT.getInstance().execute(env, runnable);
        }
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object result = super.evaluateRuntime(env);
        ServiceMT.getInstance().join(env);
        return result;
    }

    private static final class InvokeMethodAndSetResultToArrayRunnable implements Runnable {
        private IMethodCaller methodCaller;
        private Object target;
        private Object[] callParameters;
        private Object results;
        private int index;

        public InvokeMethodAndSetResultToArrayRunnable(IMethodCaller methodCaller,
                Object target,
                Object[] callParameters,
                Object results,
                int index) {
            this.methodCaller = methodCaller;
            this.target = target;
            this.callParameters = callParameters;
            this.results = results;
            this.index = index;
        }

        @Override
        public void run(IRuntimeEnv env) {
            Object value = methodCaller.invoke(target, callParameters, env);
            if (results != null) {
                Array.set(results, index, value);
            }
        }
    }

}
