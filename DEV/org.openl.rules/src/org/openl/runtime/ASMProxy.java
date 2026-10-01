package org.openl.runtime;

public abstract class ASMProxy {
    // Generated proxies resolve the field through their interfaces first, where a constant of a plain name may hide it.
    @SuppressWarnings("java:S116")
    protected ASMProxyHandler _handler;
}
