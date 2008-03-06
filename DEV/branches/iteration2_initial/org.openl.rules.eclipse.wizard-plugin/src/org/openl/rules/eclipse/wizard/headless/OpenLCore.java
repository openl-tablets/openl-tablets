package org.openl.rules.eclipse.wizard.headless;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.openl.eclipse.util.IOpenlConstants;

/**
 * @author Aliaksandr Antonik.
 */
public class OpenLCore {
    public static void addOpenlCapabilities(IProject project) throws CoreException {
    	if (project.hasNature(IOpenlConstants.OPENL_NATURE_ID)) {
            return;
        }

        
    }
}
