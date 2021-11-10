package org.openl.binding.impl.module;

import java.util.Arrays;
import java.util.Objects;

import org.openl.binding.IBindingContext;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.impl.method.AOpenMethodDelegator;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.NullOpenClass;

public class ModuleSpecificOpenMethod extends AOpenMethodDelegator {

    private final IOpenClass type;

    public ModuleSpecificOpenMethod(IOpenMethod delegate, IOpenClass type) {
        super(delegate);
        this.type = Objects.requireNonNull(type, "type cannot be null");
    }

    @Override
    public IOpenClass getType() {
        return type;
    }

    public static IMethodCaller findMethodCaller(IOpenClass type,
            String methodName,
            IOpenClass[] types,
            IBindingContext bindingContext) {
        return findCaller(type, methodName, types, false, bindingContext);
    }

    public static IMethodCaller findConstructorCaller(IOpenClass type,
            IOpenClass[] types,
            IBindingContext bindingContext) {
        return findCaller(type, null, types, true, bindingContext);
    }

    private static IMethodCaller findCaller(IOpenClass type,
            String methodName,
            IOpenClass[] types,
            boolean constructor,
            IBindingContext bindingContext) {
        IMethodCaller methodCaller = constructor ? MethodSearch.findConstructor(types, bindingContext, type)
                                                 : MethodSearch.findMethod(methodName, types, bindingContext, type);
        if (type instanceof WrapModuleSpecificTypes && methodCaller == null) {
            IOpenClass[] nullModuleSpecificTypes = Arrays.copyOf(types, types.length);
            for (int i = 0; i < nullModuleSpecificTypes.length; i++) {
                if (nullModuleSpecificTypes[i] instanceof ModuleSpecificType) {
                    nullModuleSpecificTypes[i] = NullOpenClass.the;
                }
            }
            try {
                IMethodCaller mc = constructor ? MethodSearch.findConstructor(nullModuleSpecificTypes,
                    bindingContext,
                    type) : MethodSearch.findMethod(methodName, nullModuleSpecificTypes, bindingContext, type);
                if (mc != null && isMatchToParamsModuleSpecificTypesByNames(mc.getMethod(), types)) {
                    methodCaller = mc;
                }
            } catch (AmbiguousMethodException e) {
                for (IOpenMethod method : e.getMatchingMethods()) {
                    if (isMatchToParamsModuleSpecificTypesByNames(method, types)) {
                        methodCaller = method;
                        break;
                    }
                }
            }
        }

        if (type instanceof WrapModuleSpecificTypes && methodCaller instanceof IOpenMethod && methodCaller.getMethod()
            .getType() instanceof ModuleSpecificType) {
            IOpenClass t = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE,
                methodCaller.getMethod().getType().getName());
            if (t != null) {
                methodCaller = new ModuleSpecificOpenMethod((IOpenMethod) methodCaller, t);
            }
        }
        return methodCaller;
    }

    private static boolean isMatchToParamsModuleSpecificTypesByNames(IOpenMethod method, IOpenClass[] types) {
        for (int i = 0; i < method.getSignature().getNumberOfParameters(); i++) {
            IOpenClass paramType = method.getSignature().getParameterType(i);
            if (paramType instanceof ModuleSpecificType && !Objects.equals(paramType.getName(), types[i].getName())) {
                return false;
            }
        }
        return true;
    }
}
