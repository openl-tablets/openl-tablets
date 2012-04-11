package org.openl.eclipse.launch;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.openl.eclipse.util.IOpenlConstants;

public class OpenLLaunchablePropertyTester extends PropertyTester implements IOpenlConstants {

    private static final String PROPERTY_IS_IN_OPENL_PROJECT = "isInOpenLProject";
    private static final String PROPERTY_IS_PROJECT = "isProject";
    private static final String PROPERTY_IS_OPENL_EXTENSION = "isOpenLExtension";

    private boolean hasExtension(IResource resource, Object[] args) {
        if (resource instanceof IFile) {
            IFile file = (IFile) resource;

            for (int i = 0; i < args.length; i++) {
                if (file.getName().endsWith((String) args[i])) {
                    return true;
                }
            }

        }
        return false;
    }

    protected boolean isProjectNature(IProject project, String natureID) throws CoreException {
        return project.isAccessible() && project.hasNature(natureID);
    }

    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {

        IResource resource = null;
        IProject project = null;

        if (receiver instanceof IAdaptable) {
            resource = (IResource) ((IAdaptable) receiver).getAdapter(IResource.class);
            if (resource == null) {
                return false;
            }
            project = resource.getProject();
        }

        if (PROPERTY_IS_IN_OPENL_PROJECT.equals(property)) {
            try {
                return isProjectNature(project, OPENL_NATURE_ID) || isProjectNature(project, OLD_OPENL_NATURE_ID);
            } catch (CoreException e) {
                return false;
            }
        }

        if (PROPERTY_IS_OPENL_EXTENSION.equals(property)) {
            return hasExtension(resource, args);
        }

        if (PROPERTY_IS_PROJECT.equals(property)) {
            return resource == project;
        }

        return false;
    }

}
