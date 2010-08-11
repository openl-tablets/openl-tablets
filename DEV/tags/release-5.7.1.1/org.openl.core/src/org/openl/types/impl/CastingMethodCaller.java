/*
 * Created on May 26, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import org.openl.types.IOpenCast;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class CastingMethodCaller extends MethodCaller {
    IOpenCast[] cast;

    public CastingMethodCaller(IOpenMethod method, IOpenCast[] cast) {
        super(method);
        this.cast = cast;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        Object[] newParams = new Object[params.length];

        for (int i = 0; i < newParams.length; i++) {
            newParams[i] = cast[i] == null ? params[i] : cast[i].convert(params[i]);
        }
        return method.invoke(target, newParams, env);
    }

}
