package org.openl.types.impl;

import lombok.RequiredArgsConstructor;

import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

/**
 * @author Yury Molchan
 */
@RequiredArgsConstructor
public class ParameterMethodCaller implements Invokable {

    private final int parameterNumber;

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return params[parameterNumber];
    }
}
