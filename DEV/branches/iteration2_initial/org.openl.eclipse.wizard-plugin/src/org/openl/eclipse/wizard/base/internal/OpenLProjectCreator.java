package org.openl.eclipse.wizard.base.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

/**
 * @author Aliaksandr Antonik.
 */
public class OpenLProjectCreator {
    private IProjectDescription description;
    private IProject project;

    public OpenLProjectCreator(IProject newProjectHandle, IPath projectLocation) {
        project = newProjectHandle;
        description = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
        if (projectLocation != null) {
            description.setLocation(projectLocation);
        }
    }

    public IProjectDescription getDescription() {
        return description;
    }

    public IProject getProject() {
        return project;
    }

    public void createAndOpen(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
        try {
            monitor.beginTask(null, 2000);

            project.create(description, new SubProgressMonitor(monitor, 1000));

            if (monitor.isCanceled()) {
                throw new OperationCanceledException();
            }

            project.open(new SubProgressMonitor(monitor, 1000));

        } finally {
            monitor.done();
        }
    }
}