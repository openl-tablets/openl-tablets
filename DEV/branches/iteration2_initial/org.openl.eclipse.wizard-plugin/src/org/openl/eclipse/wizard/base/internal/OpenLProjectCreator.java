package org.openl.eclipse.wizard.base.internal;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.pde.internal.core.PDECore;
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

    public void createAndOpen(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
        monitor.beginTask(null, 2);

        try {
            IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
            if (projectLocation != null) {
                description.setLocation(projectLocation);
            }
            project.create(description, new SubProgressMonitor(monitor, 1));

            if (monitor.isCanceled()) {
                throw new OperationCanceledException();
            }

            project.open(new SubProgressMonitor(monitor, 1));
        } finally {
            monitor.done();
        }
    }

    public boolean addProjectNature(String natureId) throws CoreException {
        if (project.hasNature(natureId)) {
            return true;
        }

        IProjectDescription description = project.getDescription();
        String[] natures = description.getNatureIds();

        String[] newNatures = new String[natures.length + 1];
        System.arraycopy(natures, 0, newNatures, 0, natures.length);
        newNatures[natures.length] = natureId;
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

    public void setupClasspath() throws CoreException {
        IPath projPath = project.getFullPath();

        IPath outputPath = projPath.append("bin");
        IPath srcPath = projPath.append("src");
        IPath srcGenPath = projPath.append("src");

        IClasspathEntry[] entries = new IClasspathEntry[] {
                JavaCore.newSourceEntry(srcPath),
                JavaCore.newSourceEntry(srcGenPath),
                JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER")),
                JavaCore.newContainerEntry(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH)
        };

        setClasspath(entries, outputPath);
    }

    private void setClasspath(IClasspathEntry[] entries, IPath outputLocation) throws CoreException {
        for (IClasspathEntry e : entries) {
            if (e.getEntryKind() == IClasspathEntry.CPE_SOURCE)
                createFolder(e.getPath());
        }

        createFolder(outputLocation);

        JavaCore.create(project).setRawClasspath(entries, outputLocation, true, null);
    }

    private void createFolder(IPath folderPath) throws CoreException {
        if (project.getFullPath().equals(folderPath))
            return;

        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        if (!workspaceRoot.exists(folderPath)) {
            IFolder folder = workspaceRoot.getFolder(folderPath);
            folder.create(IResource.FORCE, true, null);
        }
    }
}