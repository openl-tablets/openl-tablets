package org.openl.eclipse.wizard.base.internal;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;

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
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.IJavaProject;
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

    public boolean removeProjectNature(String natureId) throws CoreException {
        if (!project.hasNature(natureId)) {
            return true;
        }

        IProjectDescription description = project.getDescription();
        String[] natures = description.getNatureIds();
        Collection<String> newNatures = new ArrayList<String>();
        for (String nature : natures) {
            if (!nature.equals(natureId)) {
                newNatures.add(nature);
            }
        }

        description.setNatureIds(newNatures.toArray(new String[newNatures.size()]));
        project.setDescription(description, IResource.FORCE, null);
        
        return true;
    }

    public void setupClasspath(boolean isNewProject) throws CoreException {
        IPath projPath = project.getFullPath();

        IPath outputPath = projPath.append("bin");
        IPath srcPath = projPath.append("src");
        IPath srcGenPath = projPath.append("gen");

        IClasspathEntry[] entries;

        if (isNewProject) {
            entries = new IClasspathEntry[]{
                    JavaCore.newSourceEntry(srcPath),
                    JavaCore.newSourceEntry(srcGenPath),
                    JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER")),
                    JavaCore.newContainerEntry(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH)
            };
        } else {
            entries = new IClasspathEntry[] {
                    JavaCore.newSourceEntry(srcPath),
                    JavaCore.newSourceEntry(srcGenPath),
            };
        }

        IJavaProject javaProject = JavaCore.create(project);
        entries = mergeClasspath(isNewProject ? new IClasspathEntry[0] : javaProject.getRawClasspath(), entries);

        createSourceFolders(entries);
        if (isNewProject) {
            createFolder(outputPath);
            javaProject.setRawClasspath(entries, outputPath, true, null);
        } else {
            javaProject.setRawClasspath(entries, true, null);
        }
    }

    private void createSourceFolders(IClasspathEntry[] entries) throws CoreException {
        for (IClasspathEntry e : entries) {
            if (e.getEntryKind() == IClasspathEntry.CPE_SOURCE)
                createFolder(e.getPath());
        }
    }

    private static IClasspathEntry[] mergeClasspath(IClasspathEntry[] sourceEntries, IClasspathEntry[] newEntries) throws JavaModelException {
        Collection<IClasspathEntry> entries = new ArrayList<IClasspathEntry>(Arrays.asList(sourceEntries));
        for (IClasspathEntry newEntry : newEntries)
            if (!entries.contains(newEntry))
                entries.add(newEntry);

        return entries.toArray(new IClasspathEntry[entries.size()]);
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