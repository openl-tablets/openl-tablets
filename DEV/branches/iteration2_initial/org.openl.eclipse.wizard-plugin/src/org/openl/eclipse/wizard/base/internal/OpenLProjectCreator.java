package org.openl.eclipse.wizard.base.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.openl.eclipse.base.OpenlNature;
import org.openl.eclipse.util.IOpenlConstants;
import org.openl.eclipse.wizard.base.UtilBase;

/**
 * @author Aliaksandr Antonik.
 */
public class OpenLProjectCreator {
    private IProject project;
    private IPath projectLocation;

    public OpenLProjectCreator(IProject newProjectHandle, IPath projectLocation) {
        project = newProjectHandle;
        this.projectLocation = projectLocation;
    }

    public IProject getProject() {
        return project;
    }

    public void createAndOpen(IProgressMonitor monitor, int ticks) throws CoreException, OperationCanceledException {
        IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
        if (projectLocation != null) {
            description.setLocation(projectLocation);
        }
        project.create(description, new SubProgressMonitor(monitor, ticks / 2));

        if (monitor.isCanceled()) {
            throw new OperationCanceledException();
        }

        project.open(new SubProgressMonitor(monitor, (ticks + 1) / 2));
    }

    public boolean addProjectNature() throws CoreException {
        IProjectDescription description = project.getDescription();
        String[] natures = description.getNatureIds();
        String[] newNatures = new String[natures.length + 1];
        System.arraycopy(natures, 0, newNatures, 0, natures.length);
        newNatures[natures.length] = IOpenlConstants.OPENL_NATURE_ID;
        IStatus status = ResourcesPlugin.getWorkspace().validateNatureSet(newNatures);

        if (status.getCode() == IStatus.OK) {
            description.setNatureIds(newNatures);
            project.setDescription(description, IResource.FORCE, null);
            return true;
        } else {
            UtilBase.handleException("unable to add openl nature");
            return false;
        }
    }
}