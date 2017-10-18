package org.openl.binding.impl.method;

import java.lang.reflect.Array;

import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class VarArgsOpenMethod extends AOpenMethodDelegator {

    private int indexOfFirstVarArg;
    private Class<?> componentVarArgClass;

    public VarArgsOpenMethod(IOpenMethod openMethod, Class<?> componentVarArgClass, int indexOfFirstVarArg) {
        super(openMethod);
        this.componentVarArgClass = componentVarArgClass;
        this.indexOfFirstVarArg = indexOfFirstVarArg;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return getDelegate().invoke(target, modifyParameters(params), env);
    }

    private Object[] modifyParameters(Object[] methodParameters) {
        int parametersCount = getSignature().getNumberOfParameters();
        Object[] modifiedParameters = new Object[parametersCount];
        System.arraycopy(methodParameters, 0, modifiedParameters, 0, indexOfFirstVarArg);

        // all the parameters of the same type in the tail of parameters
        // sequence,
        // should be wrapped by array of this type
        //
        modifiedParameters[parametersCount - 1] = getAllParametersOfTheSameType(methodParameters);
        return modifiedParameters;
    }

    private Object getAllParametersOfTheSameType(Object[] methodParameters) {
        int parametersOfTheSameType = methodParameters.length - indexOfFirstVarArg;
        Object sameTypeParameters = Array.newInstance(componentVarArgClass, parametersOfTheSameType);

        for (int i = 0; i < parametersOfTheSameType; i++) {
            Array.set(sameTypeParameters, i, methodParameters[i + indexOfFirstVarArg]);
        }
        return sameTypeParameters;
    }
}
