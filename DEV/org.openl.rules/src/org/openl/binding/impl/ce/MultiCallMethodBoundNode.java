package org.openl.binding.impl.ce;

import java.util.List;

import org.openl.binding.IBoundNode;
import org.openl.rules.lang.xls.binding.wrapper.IOpenMethodWrapper;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenMethod;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.Tracer;

public class MultiCallMethodBoundNode extends org.openl.binding.impl.MultiCallMethodBoundNode {

    public MultiCallMethodBoundNode(ISyntaxNode syntaxNode,
            IBoundNode[] children,
            IMethodCaller singleParameterMethod,
            List<Integer> arrayArgArgumentList) {
        super(syntaxNode, children, singleParameterMethod, arrayArgArgumentList);
    }

    @Override
    protected IMethodCaller getMethodCaller(IRuntimeEnv env) {
        IMethodCaller methodCaller = getMethodCaller();
        if (methodCaller instanceof OpenMethodDispatcher) {
            OpenMethodDispatcher openMethodDispatcher = (OpenMethodDispatcher) methodCaller;
            if (openMethodDispatcher instanceof IOpenMethodWrapper) {
                openMethodDispatcher = (OpenMethodDispatcher) ((IOpenMethodWrapper) openMethodDispatcher).getDelegate();
            }
            IOpenMethod matchingMethod = openMethodDispatcher.findMatchingMethod(env);
            if (Tracer.isEnabled()) {
                return new OpenMethodDispatcherTracerWrapper(openMethodDispatcher, matchingMethod);
            } else {
                return matchingMethod;
            }
        }
        return methodCaller;
    }

    private static class OpenMethodDispatcherTracerWrapper implements IMethodCaller {
        private OpenMethodDispatcher openMethodDispatcher;
        private IMethodCaller matchingMethod;

        public OpenMethodDispatcherTracerWrapper(OpenMethodDispatcher openMethodDispatcher,
                IMethodCaller matchingMethod) {
            this.openMethodDispatcher = openMethodDispatcher;
            this.matchingMethod = matchingMethod;
            this.invokeInner = new Invokable() {
                @Override
                public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
                    Tracer.put(OpenMethodDispatcherTracerWrapper.this.openMethodDispatcher, "rule", OpenMethodDispatcherTracerWrapper.this.matchingMethod);
                    return OpenMethodDispatcherTracerWrapper.this.matchingMethod.invoke(target, params, env);
                }
            };
        }

        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            return Tracer.invoke(invokeInner, target, params, env, openMethodDispatcher);
        }

        private final Invokable invokeInner;

        @Override
        public IOpenMethod getMethod() {
            throw new IllegalStateException();
        }
    }
}
