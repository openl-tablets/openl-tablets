package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.ILocalVar;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.AmbiguousVarException;
import org.openl.binding.impl.module.RootDictionaryContext;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * Binding context for all expressions that are related to some type. All fields
 * specified in the type will be available as variables.
 *
 * @author PUdalau
 */
public class TypeBindingContext extends BindingContextDelegator {
    private RootDictionaryContext context;
    private ILocalVar localVar;

    public TypeBindingContext(IBindingContext delegate, ILocalVar localVar) {
        super(delegate);
        this.context = new RootDictionaryContext(new IOpenField[] { localVar }, 1);
        this.localVar = localVar;
    }

    @Override
    public IOpenField findVar(String namespace, String name, boolean strictMatch) throws AmbiguousVarException {
        IOpenField res = null;
        if (namespace.equals(ISyntaxConstants.THIS_NAMESPACE)) {
            res = context.findField(name);
        }

        return res != null ? res : super.findVar(namespace, name, strictMatch);
    }

    @Override
    public IMethodCaller findMethodCaller(String namespace, String name,
            IOpenClass[] parTypes) throws AmbiguousMethodException {
        IMethodCaller res = null;
        //        IOpenMethod method = null;
        if (namespace.equals(ISyntaxConstants.THIS_NAMESPACE)) {
            res = MethodSearch.getMethodCaller(name, parTypes, this, localVar.getType());

            //        	method = localVar.getType().getMatchingMethod(name, parTypes);
            if (res != null)
                res = new LocalvarMethodCaller(localVar, res);
        }

        return res == null ? super.findMethodCaller(namespace, name, parTypes) : res;
    }

    private static class LocalvarMethodCaller implements IMethodCaller {

        ILocalVar localvar;
        IMethodCaller method;

        public LocalvarMethodCaller(ILocalVar localvar, IMethodCaller method) {
            super();
            this.localvar = localvar;
            this.method = method;
        }

        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            Object newTarget = localvar.get(target, env);
            return method.invoke(newTarget, params, env);
        }

        @Override
        public IOpenMethod getMethod() {
            return method.getMethod();
        }

    }

}