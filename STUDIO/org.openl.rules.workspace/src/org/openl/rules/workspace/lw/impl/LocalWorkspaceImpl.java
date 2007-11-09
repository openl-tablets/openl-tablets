package org.openl.rules.workspace.lw.impl;

import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.abstracts.impl.ArtefactPathImpl;
import org.openl.rules.workspace.lw.LocalProject;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.util.Log;

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
            throw new ProjectException("Cannot find project ''{0}''", null, name);
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
            try {
                lp.save();
            } catch (ProjectException e) {
                Log.error("error saving local project {0}", e, lp.getName());
            }
        }
    }

    public void release() {
        saveAll();
        localProjects.clear();

        workspaceManager.notifyReleased(this);
    }

    public File getLocation() {
        return location;
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

        // add project
        localProjects.put(name, lpi);
        return lpi;
    }

    protected void loadProjects() {
        File[] folders = location.listFiles(FolderHelper.getFoldersOnlyFilter());
        for (File f : folders) {
            String name = f.getName();
            ArtefactPath ap = new ArtefactPathImpl(new String[]{name});

            LocalProjectImpl lpi = new LocalProjectImpl(name, ap, f, null, this);
            try {
                lpi.load();
            } catch (ProjectException e) {
                Log.error("error loading local project {0}", e, lpi.getName());
            }

            localProjects.put(name, lpi);
        }
    }
}
