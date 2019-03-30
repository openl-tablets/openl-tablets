package org.openl.binding.impl.method;

import java.lang.reflect.Array;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IMethodCaller;
import org.openl.vm.IRuntimeEnv;

public class VarArgsOpenMethod extends AOpenMethodDelegator {

    private int indexOfFirstVarArg;
    private Class<?> componentVarArgClass;
    private IOpenCast[] parameterCasts;
    private IMethodCaller methodCaller;

    public VarArgsOpenMethod(IMethodCaller methodCaller, Class<?> componentVarArgClass, int indexOfFirstVarArg) {
        this(methodCaller, componentVarArgClass, indexOfFirstVarArg, null);
    }

    public VarArgsOpenMethod(IMethodCaller methodCaller,
            Class<?> componentVarArgClass,
            int indexOfFirstVarArg,
            IOpenCast[] parameterCasts) {
        super(methodCaller.getMethod());
        this.methodCaller = methodCaller;
        this.componentVarArgClass = componentVarArgClass;
        this.indexOfFirstVarArg = indexOfFirstVarArg;
        this.parameterCasts = parameterCasts;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return methodCaller.invoke(target, modifyParameters(params), env);
    }

    private Object[] modifyParameters(Object[] methodParameters) {
        int parametersCount = getSignature().getNumberOfParameters();
        Object[] modifiedParameters = new Object[parametersCount];
        System.arraycopy(methodParameters, 0, modifiedParameters, 0, indexOfFirstVarArg);

        // all the parameters of the same type in the tail of parameters
        // sequence,
        // should be wrapped by array of this type
        //
        modifiedParameters[parametersCount - 1] = buildVarArgsParameter(methodParameters);
        return modifiedParameters;
    }

    private Object buildVarArgsParameter(Object[] methodParameters) {
        int countOfParametersForVarArgs = methodParameters.length - indexOfFirstVarArg;
        Object params = Array.newInstance(componentVarArgClass, countOfParametersForVarArgs);

        for (int i = 0; i < countOfParametersForVarArgs; i++) {
            if (parameterCasts == null) {
                Array.set(params, i, methodParameters[i + indexOfFirstVarArg]);
            } else {
                Array.set(params, i, parameterCasts[i].convert(methodParameters[i + indexOfFirstVarArg]));
            }
        }
        return params;
    }
}
