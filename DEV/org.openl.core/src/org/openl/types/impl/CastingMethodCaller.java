/*
 * Created on May 26, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class CastingMethodCaller extends MethodCaller {

    private IOpenCast[] cast;

    public CastingMethodCaller(IOpenMethod method, IOpenCast[] cast) {
        super(method);
        this.cast = cast;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        Object[] newParams = new Object[params.length];

        for (int i = 0; i < newParams.length; i++) {
            if (cast[i] == null) {
                newParams[i] = params[i];
            } else {
                newParams[i] = cast[i].convert(params[i]);
            }
        }

        return getMethod().invoke(target, newParams, env);
    }

}
