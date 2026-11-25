package org.openl.binding.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.binding.IBindingContext;
import org.openl.binding.ILocalVar;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.binding.impl.module.RootDictionaryContext;
import org.openl.binding.impl.module.VariableInContextFinder;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOwnTargetMethod;
import org.openl.types.java.CustomJavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * Binding context for all expressions that are related to some type. All fields specified in the type will be available
 * as variables.
 *
 * @author PUdalau
 */
public class TypeBindingContext extends BindingContextDelegator {
    private final VariableInContextFinder context;
    private final ILocalVar localVar;
    private final @Nullable ILocalVar variable;
    /**
     * Tells whether an expression bound in this context reads the local variable, a field or a method of it, or the
     * other variable.
     */
    @Getter
    private boolean variablesUsed;

    public static TypeBindingContext create(IBindingContext delegate, ILocalVar localVar) {
        return create(delegate, localVar, 1);
    }

    public static TypeBindingContext create(IBindingContext delegate, ILocalVar localVar, int maxDepthLevel) {
        return new TypeBindingContext(delegate, localVar, createVariableFinder(localVar, maxDepthLevel), null);
    }

    /**
     * Creates the context of the expression of an array index operator. The expression reads by name the fields of
     * the element, and the index of the element when it is given.
     * <p>
     * A field with the name of the index is ambiguous, as a field with the name of the element is.
     * <p>
     * A field that only some elements have can be read from the context the operator is written in for the other
     * elements, see {@link VariableInContextFinder#findElementField}, unless the operator is inside another one.
     */
    static TypeBindingContext createForElement(IBindingContext delegate,
                                               ILocalVar element,
                                               @Nullable ILocalVar index) {
        var finder = createVariableFinder(element, 1);
        VariableInContextFinder enclosing = (fieldName, strictMatch) -> delegate
                .findVar(ISyntaxConstants.THIS_NAMESPACE, fieldName, strictMatch);
        // inside another operator, a lookup in the outer context would count as a use of the outer element
        VariableInContextFinder elementFinder = delegate instanceof TypeBindingContext ? finder
                : (fieldName, strictMatch) -> finder.findElementField(fieldName, strictMatch, enclosing);
        return new TypeBindingContext(delegate, element, elementFinder, index);
    }

    private static VariableInContextFinder createVariableFinder(ILocalVar localVar, int maxDepthLevel) {
        Class<?> instanceClass = localVar.getType().getInstanceClass();
        CustomJavaOpenClass annotation = instanceClass == null ? null
                : instanceClass.getAnnotation(CustomJavaOpenClass.class);
        if (annotation != null) {
            return createCustomVariableFinder(annotation, localVar, maxDepthLevel);
        }
        return new RootDictionaryContext(new IOpenField[]{localVar}, maxDepthLevel);
    }

    private static VariableInContextFinder createCustomVariableFinder(CustomJavaOpenClass annotation,
                                                                      IOpenField localVar,
                                                                      int maxDepthLevel) {
        Class<? extends VariableInContextFinder> type = annotation.variableInContextFinder();
        try {
            return type.getConstructor(IOpenField.class, int.class).newInstance(localVar, maxDepthLevel);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Cannot find constructor with signature 'public MyCustomVariableFinder(IOpenField<?> field, int depthLevel)' in type %s".formatted(
                    type.getTypeName()), e);
        } catch (InstantiationException e) {
            throw new IllegalStateException(
                    "Error while creating a custom VariableInContextFinder of type '%s'".formatted(type.getTypeName()),
                    e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Constructor of a custom VariableInContextFinder of type '%s' is inaccessible".formatted(
                            type.getTypeName()),
                    e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(
                    "Constructor of a class '%s' threw and exception".formatted(type.getTypeName()),
                    e);
        }

    }

    private TypeBindingContext(IBindingContext delegate,
                               ILocalVar localVar,
                               VariableInContextFinder context,
                               @Nullable ILocalVar variable) {
        super(delegate);
        this.context = context;
        this.localVar = localVar;
        this.variable = variable;
    }

    @Override
    public IOpenField findVar(String namespace, String name, boolean strictMatch) throws AmbiguousFieldException {
        IOpenField res = null;
        if (namespace.equals(ISyntaxConstants.THIS_NAMESPACE)) {
            res = context.findVariable(name, strictMatch);
        }
        if (res == null) {
            res = super.findVar(namespace, name, strictMatch);
            variablesUsed |= res != null && (res == localVar || res == variable);
            return res;
        }
        variablesUsed = true;
        if (variable != null && (strictMatch ? variable.getName().equals(name)
                : variable.getName().equalsIgnoreCase(name))) {
            throw new AmbiguousFieldException(name, List.of(variable, res));
        }
        return res;
    }

    @Override
    public IMethodCaller findMethodCaller(String namespace,
                                          String name,
                                          IOpenClass[] parTypes) throws AmbiguousMethodException {
        IMethodCaller res = null;
        if (namespace.equals(ISyntaxConstants.THIS_NAMESPACE)) {
            res = MethodSearch.findMethod(name, parTypes, this, localVar.getType(), true);

            if (res != null) {
                res = new LocalVarMethodCaller(localVar, res);
                variablesUsed = true;
            }
        }

        return res == null ? super.findMethodCaller(namespace, name, parTypes) : res;
    }

    private static class LocalVarMethodCaller implements IMethodCaller, IOwnTargetMethod {

        private final ILocalVar localvar;
        private final IMethodCaller method;

        private LocalVarMethodCaller(ILocalVar localvar, IMethodCaller method) {
            this.localvar = localvar;
            this.method = method;
        }

        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            var newTarget = localvar.get(target, env);
            if (newTarget == null && !(method instanceof IOwnTargetMethod) && !method.getMethod().isStatic()) {
                // An empty element gives an empty result, as the call with the named element does.
                return method.getMethod().getType().nullObject();
            }
            return method.invoke(newTarget, params, env);
        }

        @Override
        public IOpenMethod getMethod() {
            return method.getMethod();
        }

    }

}
