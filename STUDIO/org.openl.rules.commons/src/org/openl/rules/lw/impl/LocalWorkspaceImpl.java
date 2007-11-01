package org.openl.rules.lw.impl;

import org.openl.rules.WorkspaceUser;
import org.openl.rules.commons.Utils;
import org.openl.rules.commons.artefacts.ArtefactPath;
import org.openl.rules.commons.artefacts.impl.ArtefactPathImpl;
import org.openl.rules.commons.projects.Project;
import org.openl.rules.commons.projects.ProjectArtefact;
import org.openl.rules.commons.projects.ProjectException;
import org.openl.rules.commons.projects.ProjectVersion;
import org.openl.rules.lw.LocalProject;
import org.openl.rules.lw.LocalWorkspace;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LocalWorkspaceImpl implements LocalWorkspace {
    private LocalWorkspaceManagerImpl workspaceManager;
    private WorkspaceUser user;
    private File location;
    private Map<String, LocalProject> localProjects;

    public LocalWorkspaceImpl(LocalWorkspaceManagerImpl workspaceManager, WorkspaceUser user, File location) {
        this.workspaceManager = workspaceManager;
        this.user = user;
        this.location = location;
        localProjects = new HashMap<String, LocalProject>();

        loadProjects();
    }

    public Collection<LocalProject> getProjects() {
        return localProjects.values();
    }

    public LocalProject getProject(String name) throws ProjectException {
        LocalProject lp = localProjects.get(name);
        if (lp == null) {
            throw new ProjectException("Cannot find project ''{0}''", name);
        }

        return lp;
    }

    public boolean hasProject(String name) {
        return (localProjects.get(name) != null);
    }

    public ProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        String projectName = artefactPath.segment(0);
        LocalProject lp = getProject(projectName);

        ArtefactPath pathInProject = artefactPath.getRelativePath(1);
        return lp.getArtefactByPath(pathInProject);
    }

    public LocalProject addProject(Project project) throws ProjectException {
        String name = project.getName();

        if (hasProject(name)) {
            // remove it
            getProject(name).remove();
            // TODO smart update (if it is reasonable)
        }

        return downloadProject(project);
    }

    public void removeProject(String name) throws ProjectException {
        getProject(name).remove();
    }

    public void refresh() {
        for (LocalProject lp : localProjects.values()) {
            lp.refresh();
        }
    }

    public void saveAll() {
        for (LocalProject lp : localProjects.values()) {
            lp.save();
        }
    }

    public void release() {
        saveAll();
        localProjects.clear();

        workspaceManager.notifyReleased(this);
    }

// --- protected

    protected void notifyRemoved(LocalProject project) {
        localProjects.remove(project.getName());
    }

    protected WorkspaceUser getUser() {
        return user;
    }

    protected LocalProjectImpl downloadProject(Project project) throws ProjectException {
        String name = project.getName();

        ArtefactPath ap = new ArtefactPathImpl(new String[]{name});
        ProjectVersion pv = project.getVersion();
        File f = new File(location, name);

        LocalProjectImpl lpi = new LocalProjectImpl(name, ap, f, pv, this);
        lpi.downloadArtefact(project);

        localProjects.put(name, lpi);
        return lpi;
    }

    protected void loadProjects() {
        File[] folders = location.listFiles(Utils.getFoldersOnlyFilter());
        for (File f : folders) {
            String name = f.getName();
            ArtefactPath ap = new ArtefactPathImpl(new String[]{name});

            LocalProjectImpl lpi = new LocalProjectImpl(name, ap, f, null, this);
            lpi.load();
            
            localProjects.put(name, lpi);
        }
    }
}
