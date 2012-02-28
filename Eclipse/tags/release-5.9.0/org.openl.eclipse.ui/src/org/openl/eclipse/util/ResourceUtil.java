/*
 * Created on 24.12.2004
 */
package org.openl.eclipse.util;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * @author smesh
 */
public class ResourceUtil {

    static public IResource findResource(IPath path, IContainer parent) {
        if (path.isAbsolute()) {
            parent = ResourcesPlugin.getWorkspace().getRoot();
        }

        IResource resource = parent.findMember(path);
        return resource;
    }

    // TODO re-wise: move to IEWorkspaceRoot
    // TODO re-wise: replace with (IPath, IContainer)
    // TODO re-wise: it seems that this method serves invalid hypothesis:
    // everything is IResource
    static public IResource findWorkspaceResource(IPath path) {
        IWorkspaceRoot ws = ResourcesPlugin.getWorkspace().getRoot();
        IPath wsPath = ws.getLocation();

        if (wsPath.isPrefixOf(path)) {
            path = path.removeFirstSegments(wsPath.segmentCount());
            path = path.setDevice(null);
        }

        return ws.findMember(path);
    }

    static public IResource findWorkspaceResource(String url) {
        url = normalizeFile(url);
        return findWorkspaceResource(new Path(url));
    }

    static public IProject getProject(String projectName) {
        return (IProject) getWorkspaceRoot().findMember(projectName);
    }

    static public IWorkspace getWorkspace() {
        return ResourcesPlugin.getWorkspace();
    }

    static public IWorkspaceRoot getWorkspaceRoot() {
        return getWorkspace().getRoot();
    }

    // TODO: re-implement: OS specific (unix, UNC, MAC)
    static private String normalizeFile(String s) {
        if (s.indexOf('/') == 0) {
            s = s.substring(1);
        }

        return s;
    }

    static public String toCanonicalUrl(IContainer parent, IPath path) {

        IResource resource = findResource(path, parent);
        if (resource != null) {
            path = resource.getLocation();
        }
        return UrlUtil.toCanonicalUrl(path.toString());
    }

}