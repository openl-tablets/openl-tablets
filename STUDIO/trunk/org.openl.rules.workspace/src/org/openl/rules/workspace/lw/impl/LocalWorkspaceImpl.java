package org.openl.rules.workspace.lw.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.impl.LocalAPI;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.impl.ArtefactPathImpl;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.LocalWorkspaceListener;
import org.openl.util.MsgHelper;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class LocalWorkspaceImpl implements LocalWorkspace {
    private static final Log log = LogFactory.getLog(LocalWorkspaceImpl.class);

    private WorkspaceUser user;
    private File location;
    private Map<String, AProject> localProjects;
    private List<LocalWorkspaceListener> listeners = new ArrayList<LocalWorkspaceListener>();
    private FileFilter localWorkspaceFolderFilter;
    private FileFilter localWorkspaceFileFilter;
    private UserWorkspace userWorkspace;

    public LocalWorkspaceImpl(WorkspaceUser user, File location, FileFilter localWorkspaceFolderFilter,
            FileFilter localWorkspaceFileFilter) {
        this.user = user;
        this.location = location;
        this.localWorkspaceFolderFilter = localWorkspaceFolderFilter;
        this.localWorkspaceFileFilter = localWorkspaceFileFilter;

        localProjects = new HashMap<String, AProject>();

        loadProjects();
    }

    public AProject addProject(AProject project) throws ProjectException {
        String name = project.getName();

        if (hasProject(name)) {
            removeProject(name);
            // TODO smart update (if it is reasonable)
        }

        return downloadProject(project);
    }

    public void addWorkspaceListener(LocalWorkspaceListener listener) {
        listeners.add(listener);
    }

    protected AProject downloadProject(AProject project) throws ProjectException {
        String name = project.getName();

        ArtefactPath ap = new ArtefactPathImpl(new String[] { name });
        File f = new File(location, name);
        f.mkdir();

        AProject localProject = new AProject(new LocalAPI(f, ap, this), user);

        localProject.update(project);
//      localProject.save();

        // add project
        localProjects.put(name, localProject);
        return localProject;
    }

    public AProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        String projectName = artefactPath.segment(0);
        AProject lp = getProject(projectName);

        ArtefactPath pathInProject = artefactPath.withoutFirstSegment();
        return lp.getArtefactByPath(pathInProject);
    }

    public File getLocation() {
        return location;
    }

    public AProject getProject(String name) throws ProjectException {
        AProject lp = localProjects.get(name);
        if (lp == null) {
            throw new ProjectException("Cannot find project ''{0}''!", null, name);
        }

        return lp;
    }

    public Collection<AProject> getProjects() {
        return localProjects.values();
    }

    protected WorkspaceUser getUser() {
        return user;
    }

    public boolean hasProject(String name) {
        return (localProjects.get(name) != null);
    }

    private boolean isLocalOnly(AProject lp) {
        if (userWorkspace == null) {
            return false;
        }
        try {
            return userWorkspace.getProject(lp.getName()).isLocalOnly();
        } catch (ProjectException e) {
            return false;
        }
    }

    protected void loadProjects() {
        File[] folders = location.listFiles(localWorkspaceFolderFilter);
        for (File f : folders) {
            String name = f.getName();
            ArtefactPath ap = new ArtefactPathImpl(new String[] { name });

            AProject lpi = new AProject(new LocalAPI(f, ap, this), user);;
            try {
                lpi.open();
            } catch (ProjectException e) {
                log.error(MsgHelper.format("Error loading local project ''{0}''!", lpi.getName()), e);
            }

            localProjects.put(name, lpi);
        }
    }

    protected void notifyRemoved(AProject project) {
        localProjects.remove(project.getName());
    }

    public void refresh() {
        // check existing
        Iterator<AProject> i = localProjects.values().iterator();
        while (i.hasNext()) {
            AProject lp = i.next();

            File projectLocation = new File(location, lp.getName());
            if (projectLocation.exists()) {
                // still here
                lp.refresh();
            } else {
                // deleted externally
                i.remove();
            }
        }

        // check new
        File[] folders = location.listFiles(localWorkspaceFolderFilter);
        if (folders == null) {
            return;
        }

        for (File folder : folders) {
            String name = folder.getName();
            if (!localProjects.containsKey(name)) {
                // new project detected
                ArtefactPath ap = new ArtefactPathImpl(new String[] { name });
                AProject newlyDetected = new AProject(new LocalAPI(folder, ap, this), user);;

                try {
                    newlyDetected.open();
                } catch (ProjectException e) {
                    String msg = MsgHelper.format("Error loading just detected local project ''{0}''!", name);
                    log.error(msg, e);
                }

                // add it
                localProjects.put(name, newlyDetected);
            }
        }
    }

    public void release() {
        saveAll();
        localProjects.clear();

        userWorkspace = null;
        for (LocalWorkspaceListener lwl : listeners) {
            lwl.workspaceReleased(this);
        }
    }

    // --- protected

    public void removeProject(String name) throws ProjectException {
        AProject project = getProject(name);
        notifyRemoved(project);
        project.delete();
    }

    public boolean removeWorkspaceListener(LocalWorkspaceListener listener) {
        return listeners.remove(listener);
    }

    public void saveAll() {
        for (AProject lp : localProjects.values()) {
            if (!isLocalOnly(lp)) {
                try {
                    lp.checkIn();
                } catch (ProjectException e) {
                    String msg = MsgHelper.format("Error saving local project ''{0}''!", lp.getName());
                    log.error(msg, e);
                }
            }
        }
    }

    public void setUserWorkspace(UserWorkspace userWorkspace) {
        this.userWorkspace = userWorkspace;
    }

    public FileFilter getLocalWorkspaceFileFilter() {
        return localWorkspaceFileFilter;
    }
}
