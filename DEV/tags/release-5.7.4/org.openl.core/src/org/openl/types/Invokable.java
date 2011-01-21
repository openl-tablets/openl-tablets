package org.openl.types;

import org.openl.vm.IRuntimeEnv;

/**
 * Interface for invokable objects.
 * 
 * @author DLiauchuk 
 */
public interface Invokable {
        
    Object invoke(Object target, Object[] params, IRuntimeEnv env);
}
