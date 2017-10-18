package org.openl.binding.impl.method;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public final class AutoCastableResultOpenMethod extends AOpenMethodDelegator {

    private IOpenCast cast;

    private IOpenClass returnType;

    public AutoCastableResultOpenMethod(IOpenMethod openMethod, IOpenClass returnType, IOpenCast cast) {
        super(openMethod);
        if (returnType == null) {
            throw new IllegalArgumentException();
        }
        if (cast == null) {
            throw new IllegalArgumentException();
        }
        this.returnType = returnType;
        this.cast = cast;
    }

    public IOpenClass getType() {
        return returnType;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return cast.convert(getDelegate().invoke(target, params, env));
    }

}