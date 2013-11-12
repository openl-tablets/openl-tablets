package org.openl.rules.project.resolving;

import org.openl.rules.project.model.Module;

/**
 * Implementations of this interface should have default constructor.
 * 
 * 
 * @author Marat Kamalov
 * 
 */
public interface InitializingModuleListener {

    /**
     * Listener on module load method
     * 
     * @param module
     */
    void afterModuleLoad(Module module);
}
