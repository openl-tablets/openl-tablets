package org.openl.rules.workspace.lw.impl;

import java.io.File;
import java.io.FileFilter;
import java.util.*;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.LocalWorkspaceListener;

public class LocalWorkspaceImpl implements LocalWorkspace {

    private final WorkspaceUser user;
    private final File location;
    private final Map<String, AProject> localProjects;
    private final List<LocalWorkspaceListener> listeners = new ArrayList<>();
    private final FileFilter localWorkspaceFolderFilter;
    private final FileFilter localWorkspaceFileFilter;
    private final LocalRepository localRepository;

    public LocalWorkspaceImpl(WorkspaceUser user,
            File location,
            FileFilter localWorkspaceFolderFilter,
            FileFilter localWorkspaceFileFilter) {
        this.user = user;
        this.location = location;
        this.localWorkspaceFolderFilter = localWorkspaceFolderFilter;
        this.localWorkspaceFileFilter = localWorkspaceFileFilter;

        localProjects = new HashMap<>();
        localRepository = new LocalRepository(location);
        try {
            localRepository.initialize();
        } catch (RRepositoryException e) {
            throw new IllegalStateException(e);
        }

        loadProjects();
    }

    @Override
    public void addWorkspaceListener(LocalWorkspaceListener listener) {
        listeners.add(listener);
    }

    @Override
    public LocalRepository getRepository() {
        return localRepository;
    }

    @Override
    public AProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        String projectName = artefactPath.segment(0);
        AProject lp = getProject(projectName);

        ArtefactPath pathInProject = artefactPath.withoutFirstSegment();
        return lp.getArtefactByPath(pathInProject);
    }

    @Override
    public File getLocation() {
        return location;
    }

    @Override
    public AProject getProject(String name) throws ProjectException {
        AProject lp;
        synchronized (localProjects) {
            lp = localProjects.get(name.toLowerCase());
        }
        if (lp == null) {
            throw new ProjectException("Cannot find project ''{0}''.", null, name);
        }

        return lp;
    }

    @Override
    public Collection<AProject> getProjects() {
        synchronized (localProjects) {
            return localProjects.values();
        }
    }

    protected WorkspaceUser getUser() {
        return user;
    }

    @Override
    public boolean hasProject(String name) {
        synchronized (localProjects) {
            return localProjects.get(name.toLowerCase()) != null;
        }
    }

    private void loadProjects() {
        File[] folders = location.listFiles(localWorkspaceFolderFilter);
        if (folders != null) {
            for (File f : folders) {
                String name = f.getName();

                AProject lpi;
                FileData fileData = localRepository.getProjectState(name).getFileData();
                if (fileData == null) {
                    String version = localRepository.getProjectState(name).getProjectVersion();
                    lpi = new AProject(getRepository(), name, version);
                } else {
                    lpi = new AProject(getRepository(), fileData);
                }
                synchronized (localProjects) {
                    localProjects.put(name.toLowerCase(), lpi);
                }
            }
        }
    }

    @Override
    public void refresh() {
        // check existing
        synchronized (localProjects) {
            localProjects.clear();
        }
        loadProjects();
    }

    @Override
    public void release() {
        synchronized (localProjects) {
            localProjects.clear();
        }

        localRepository.close();

        for (LocalWorkspaceListener lwl : new ArrayList<>(listeners)) {
            lwl.workspaceReleased(this);
        }
    }

    @Override
    public boolean removeWorkspaceListener(LocalWorkspaceListener listener) {
        return listeners.remove(listener);
    }

    public FileFilter getLocalWorkspaceFileFilter() {
        return localWorkspaceFileFilter;
    }
}
