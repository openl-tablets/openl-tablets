package org.openl.runtime;

import org.openl.vm.IRuntimeEnv;

public interface IEngineWrapper<T>
{
    IRuntimeEnv getRuntimeEnv();
    Object getInstance();

    
    EngineFactory<T> getFactory();
}
