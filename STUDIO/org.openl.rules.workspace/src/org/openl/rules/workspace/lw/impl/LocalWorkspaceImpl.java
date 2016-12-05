package org.openl.rules.workspace.lw.impl;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.LocalWorkspaceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;

public class LocalWorkspaceImpl implements LocalWorkspace {
    private final Logger log = LoggerFactory.getLogger(LocalWorkspaceImpl.class);

    private final WorkspaceUser user;
    private final File location;
    private final Map<String, AProject> localProjects;
    private final List<LocalWorkspaceListener> listeners = new ArrayList<LocalWorkspaceListener>();
    private final FileFilter localWorkspaceFolderFilter;
    private final FileFilter localWorkspaceFileFilter;
    private final LocalRepository localRepository;

    public LocalWorkspaceImpl(WorkspaceUser user, File location, FileFilter localWorkspaceFolderFilter,
                              FileFilter localWorkspaceFileFilter) {
        this.user = user;
        this.location = location;
        this.localWorkspaceFolderFilter = localWorkspaceFolderFilter;
        this.localWorkspaceFileFilter = localWorkspaceFileFilter;

        localProjects = new HashMap<String, AProject>();
        localRepository = new LocalRepository(location, new LocalProjectModificationHandler(location));
        try {
            localRepository.initialize();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

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

    private AProject downloadProject(AProject project) throws ProjectException {
        String name = project.getName();

        ArtefactPath ap = new ArtefactPathImpl(new String[]{name});
        File f = new File(location, name);
        if (!f.mkdir() && !f.exists()) {
            throw new ProjectException(String.format("Can't create the folder '%s'", f.getAbsolutePath()));
        }

        AProject localProject = createLocalProject(ap);

        localProject.update(project, user);

        // add project
        synchronized (localProjects) {
            localProjects.put(name, localProject);
        }
        return localProject;
    }

    private AProject createLocalProject(ArtefactPath ap) {
        return new AProject(getRepository(), ap.getStringValue(), true);
    }

    @Override
    public LocalRepository getRepository() {
        return localRepository;
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
        AProject lp;
        synchronized (localProjects) {
            lp = localProjects.get(name);
        }
        if (lp == null) {
            throw new ProjectException("Cannot find project ''{0}''!", null, name);
        }

        return lp;
    }

    public Collection<AProject> getProjects() {
        synchronized (localProjects) {
            return localProjects.values();
        }
    }

    protected WorkspaceUser getUser() {
        return user;
    }

    public boolean hasProject(String name) {
        synchronized (localProjects) {
            return (localProjects.get(name) != null);
        }
    }

    private void loadProjects() {
        File[] folders = location.listFiles(localWorkspaceFolderFilter);
        if (folders != null) {
            for (File f : folders) {
                String name = f.getName();
                ArtefactPath ap = new ArtefactPathImpl(new String[]{name});

                AProject lpi = createLocalProject(ap);
                synchronized (localProjects) {
                    localProjects.put(name, lpi);
                }
            }
        }
    }

    private void notifyRemoved(AProject project) {
        synchronized (localProjects) {
            localProjects.remove(project.getName());
        }
    }

    public void refresh() {
        // check existing
        synchronized (localProjects) {
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
        }

        // check new
        File[] folders = location.listFiles(localWorkspaceFolderFilter);
        if (folders == null) {
            return;
        }

        for (File folder : folders) {
            String name = folder.getName();
            synchronized (localProjects) {
                if (!localProjects.containsKey(name)) {
                    // new project detected
                    ArtefactPath ap = new ArtefactPathImpl(new String[]{name});
                    AProject newlyDetected = createLocalProject(ap);

                    // add it
                    localProjects.put(name, newlyDetected);
                }
            }
        }
    }

    public void release() {
        synchronized (localProjects) {
            localProjects.clear();
        }

        try {
            localRepository.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        for (LocalWorkspaceListener lwl : listeners) {
            lwl.workspaceReleased(this);
        }
    }

    public void removeProject(String name) throws ProjectException {
        AProject project = getProject(name);
        notifyRemoved(project);
        project.delete(user);
    }

    public boolean removeWorkspaceListener(LocalWorkspaceListener listener) {
        return listeners.remove(listener);
    }

    public FileFilter getLocalWorkspaceFileFilter() {
        return localWorkspaceFileFilter;
    }
}
