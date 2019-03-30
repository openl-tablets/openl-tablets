package org.openl.binding.impl.method;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public final class AutoCastableResultOpenMethod extends AOpenMethodDelegator {

    private IOpenCast cast;
    private IOpenClass returnType;
    private IMethodCaller methodCaller;

    public AutoCastableResultOpenMethod(IMethodCaller methodCaller, IOpenClass returnType, IOpenCast cast) {
        super(methodCaller.getMethod());
        if (returnType == null) {
            throw new IllegalArgumentException();
        }
        if (cast == null) {
            throw new IllegalArgumentException();
        }
        this.returnType = returnType;
        this.cast = cast;
        this.methodCaller = methodCaller;
    }

    @Override
    public IOpenClass getType() {
        return returnType;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return cast.convert(methodCaller.invoke(target, params, env));
    }

}