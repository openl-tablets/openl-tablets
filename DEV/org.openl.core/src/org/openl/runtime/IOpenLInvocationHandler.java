package org.openl.runtime;

import java.lang.reflect.InvocationHandler;

public interface IOpenLInvocationHandler extends InvocationHandler {
    Object getTarget();
}
