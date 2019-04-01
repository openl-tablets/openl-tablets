package org.openl.binding.impl;

import java.lang.reflect.InvocationTargetException;

import org.openl.binding.IBindingContext;
import org.openl.binding.ILocalVar;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.AmbiguousVarException;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.binding.impl.module.RootDictionaryContext;
import org.openl.binding.impl.module.VariableInContextFinder;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.*;
import org.openl.types.java.CustomJavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * Binding context for all expressions that are related to some type. All fields specified in the type will be available
 * as variables.
 *
 * @author PUdalau
 */
public class TypeBindingContext extends BindingContextDelegator {
    private VariableInContextFinder context;
    private ILocalVar localVar;

    public static TypeBindingContext create(IBindingContext delegate, ILocalVar localVar) {
        Class<?> instanceClass = localVar.getType().getInstanceClass();
        CustomJavaOpenClass annotation = instanceClass == null ? null
                                                               : instanceClass.getAnnotation(CustomJavaOpenClass.class);
        VariableInContextFinder context;
        if (annotation != null && annotation.variableInContextFinder() != null) {
            context = createCustomVariableFinder(annotation, localVar);
        } else {
            context = new RootDictionaryContext(new IOpenField[] { localVar }, 1);
        }

        return new TypeBindingContext(delegate, localVar, context);
    }

    private static VariableInContextFinder createCustomVariableFinder(CustomJavaOpenClass annotation,
            IOpenField localVar) {
        Class<? extends VariableInContextFinder> type = annotation.variableInContextFinder();
        try {
            return type.getConstructor(IOpenField.class, int.class).newInstance(localVar, 1);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(String.format(
                "Cannot find constructor with signature 'public MyCustomVariableFinder(IOpenField<?> field, int depthLevel)' in type %s",
                type.getCanonicalName()), e);
        } catch (InstantiationException e) {
            throw new IllegalStateException(
                String.format("Error while creating a custom VariableInContextFinder of type '%s'",
                    type.getCanonicalName()),
                e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                String.format("Constructor of a custom VariableInContextFinder of type '%s' is inaccessible",
                    type.getCanonicalName()),
                e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(
                String.format("Constructor of a class '%s' threw and exception", type.getCanonicalName()),
                e);
        }

    }

    private TypeBindingContext(IBindingContext delegate, ILocalVar localVar, VariableInContextFinder context) {
        super(delegate);
        this.context = context;
        this.localVar = localVar;
    }

    @Override
    public IOpenField findVar(String namespace, String name, boolean strictMatch) throws AmbiguousVarException {
        IOpenField res = null;
        if (namespace.equals(ISyntaxConstants.THIS_NAMESPACE)) {
            res = context.findVariable(name);
        }

        return res != null ? res : super.findVar(namespace, name, strictMatch);
    }

    @Override
    public IMethodCaller findMethodCaller(String namespace,
            String name,
            IOpenClass[] parTypes) throws AmbiguousMethodException {
        IMethodCaller res = null;
        // IOpenMethod method = null;
        if (namespace.equals(ISyntaxConstants.THIS_NAMESPACE)) {
            res = MethodSearch.findMethod(name, parTypes, this, localVar.getType());

            // method = localVar.getType().getMatchingMethod(name, parTypes);
            if (res != null) {
                res = new LocalVarMethodCaller(localVar, res);
            }
        }

        return res == null ? super.findMethodCaller(namespace, name, parTypes) : res;
    }

    private static class LocalVarMethodCaller implements IMethodCaller, IOwnTargetMethod {

        ILocalVar localvar;
        IMethodCaller method;

        public LocalVarMethodCaller(ILocalVar localvar, IMethodCaller method) {
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