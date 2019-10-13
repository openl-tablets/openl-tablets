package org.openl.binding.impl.method;

import java.util.Objects;

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
        this.returnType = Objects.requireNonNull(returnType, "returnType can't be null.");
        this.cast = Objects.requireNonNull(cast, "cast can't be null.");
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