package org.openl.rules.project.instantiation;

import org.openl.rules.project.model.Module;

/**
 * Instances of this class should have default constructor.
 * 
 * 
 * @author Marat Kamalov
 * 
 */
public interface InitializingListener {

    /**
     * Listener on module load method
     * 
     * @param module
     */
    void afterModuleLoad(Module module);
}
