package org.openl.main;

import org.openl.CompiledOpenClass;
import org.openl.types.IOpenClass;

/**
 * @deprecated Use generated interface and beans
 */
@Deprecated
public interface OpenLWrapper {

    /**
     *
     * @return CompiledOpenClass - it is a safe operation, it does not throw
     *         exceptions in case when there were compilation errors
     */
    CompiledOpenClass getCompiledOpenClass();

    Object getInstance();

    IOpenClass getOpenClass();

    void reload();

}
