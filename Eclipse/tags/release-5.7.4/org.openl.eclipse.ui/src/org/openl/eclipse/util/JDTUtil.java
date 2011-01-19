/*
 * Created on 22.12.2004
 */
package org.openl.eclipse.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * @author smesh
 */
public class JDTUtil {

    static private void addJavaLibraries(IJavaProject javaProject, Collection classpath, boolean exportedOnly)
            throws Exception {

        // UtilBase util = new UtilBase();

        IPath defaultOutputLocation = javaProject.getOutputLocation();

        IClasspathEntry[] classpathEntries = javaProject.getResolvedClasspath(true);

        for (int i = 0; i < classpathEntries.length; i++) {

            IPath path = null;

            switch (classpathEntries[i].getEntryKind()) {
                case IClasspathEntry.CPE_LIBRARY:
                    if (exportedOnly && !classpathEntries[i].isExported()) {
                        continue;
                    }
                    path = classpathEntries[i].getPath();
                    break;
                case IClasspathEntry.CPE_SOURCE:
                    path = classpathEntries[i].getOutputLocation();
                    if (path == null) {
                        path = defaultOutputLocation;
                    }
                    break;
            }

            if (path == null) {
                continue;
            }

            String path2 = ResourceUtil.toCanonicalUrl(javaProject.getProject(), path);
            if (!classpath.contains(path2)) {
                classpath.add(path2);
            }
        }
    }

    static private void addRequiredJavaProjectNames(String projectName, Collection projects, boolean exportedOnly)
            throws Exception {

        if (projects.contains(projectName)) {
            return;
        }

        projects.add(projectName);

        IJavaProject javaProject = getJavaProject(projectName);
        if (javaProject == null) {
            return;
        }

        boolean ignoreUnresolvedEntry = true;
        IClasspathEntry[] classpathEntries = javaProject.getResolvedClasspath(ignoreUnresolvedEntry);

        for (int i = 0; i < classpathEntries.length; i++) {
            switch (classpathEntries[i].getEntryKind()) {
                case IClasspathEntry.CPE_PROJECT:
                    if (exportedOnly && !classpathEntries[i].isExported()) {
                        continue;
                    }

                    String requiredProjectName = classpathEntries[i].getPath().lastSegment();
                    boolean exportedOnlyForNonRootProject = true;
                    addRequiredJavaProjectNames(requiredProjectName, projects, exportedOnlyForNonRootProject);
                    break;
            }
        }
    }

    static public IJavaProject getJavaProject(IProject project) {
        try {
            return (IJavaProject) project.getNature(JavaCore.NATURE_ID);
        } catch (Exception e) {
            System.err.println(e);
            return null;
        }
    }

    static public IJavaProject getJavaProject(String projectName) {
        try {
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
            return project == null ? null : getJavaProject(project);
        } catch (Exception e) {
            System.err.println(e);
            return null;
        }

    }

    static public String[] getJavaProjectClasspath(IProject project) throws Exception {

        Collection projects = new ArrayList();
        Collection classpath = new ArrayList();

        addRequiredJavaProjectNames(project.getName(), projects, false);

        boolean isRootProject = true;
        for (Iterator it = projects.iterator(); it.hasNext();) {
            String projectName = (String) it.next();

            IJavaProject javaProject = getJavaProject(projectName);
            if (javaProject == null) {
                continue;
            }

            boolean exportedOnly = !isRootProject;
            addJavaLibraries(javaProject, classpath, exportedOnly);
            isRootProject = false;
        }

        return (String[]) classpath.toArray(new String[classpath.size()]);

    }

}