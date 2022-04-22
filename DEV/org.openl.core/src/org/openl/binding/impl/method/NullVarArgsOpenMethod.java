package org.openl.binding.impl.method;

import org.openl.types.IMethodCaller;
import org.openl.vm.IRuntimeEnv;

/**
 * This method is designed to wrap method call with additional argument set to null. It is used in varargs logic. For
 * example: we have a method m(String... args) and call it as m()
 */
public class NullVarArgsOpenMethod extends AOpenMethodDelegator {

    private final IMethodCaller methodCaller;

    public NullVarArgsOpenMethod(IMethodCaller methodCaller) {
        super(methodCaller.getMethod());
        this.methodCaller = methodCaller;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return methodCaller.invoke(target, modifyParameters(params), env);
    }

    private Object[] modifyParameters(Object[] methodParameters) {
        int parametersCount = getSignature().getNumberOfParameters();
        Object[] modifiedParameters = new Object[parametersCount];
        System.arraycopy(methodParameters, 0, modifiedParameters, 0, methodParameters.length);
        return modifiedParameters;
    }

}
