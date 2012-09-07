package org.openl.rules.project.resolving;

import org.openl.rules.project.model.ProjectDescriptor;

/**
 * @author Marat Kamalov
 */
public interface InitializingProjectListener {
    /**
     * Listener on module load method
     * 
     * @param module
     */
    void afterProjectLoad(ProjectDescriptor projectDescriptor);
}
