package org.openl.rules.workspace.lw.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.ProjectState;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.ProjectKey;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.LocalWorkspaceListener;

public class LocalWorkspaceImpl implements LocalWorkspace {
    private static final Comparator<AProject> PROJECTS_COMPARATOR = (o1, o2) -> o1.getName()
        .compareToIgnoreCase(o2.getName());

    private final WorkspaceUser user;
    private final File location;
    private final Map<ProjectKey, AProject> localProjects;
    private final List<LocalWorkspaceListener> listeners = new ArrayList<>();
    private final LocalRepository localRepository;
    private final DesignTimeRepository designTimeRepository;

    LocalWorkspaceImpl(WorkspaceUser user, File location, DesignTimeRepository designTimeRepository) {
        this.user = user;
        this.location = location;
        this.designTimeRepository = designTimeRepository;

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
    public LocalRepository getRepository(String id) {
        if (id == null) {
            // For backward compatibility.
            id = "design";
        }
        // Create a new instance with id and name.
        LocalRepository repository = new LocalRepository(localRepository.getRoot());
        repository.setId(id);
        if (designTimeRepository != null) {
            Repository designRepository = designTimeRepository.getRepository(id);
            if (designRepository != null) {
                repository.setName(designRepository.getName());
            }
        }
        try {
            repository.initialize();
        } catch (RRepositoryException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return repository;
    }

    @Override
    public File getLocation() {
        return location;
    }

    @Override
    public AProject getProject(String repositoryId, String name) throws ProjectException {
        AProject lp;
        synchronized (localProjects) {
            lp = localProjects.get(new ProjectKey(repositoryId, name.toLowerCase()));
        }
        if (lp == null) {
            throw new ProjectException("Cannot find project ''{0}''.", null, name);
        }

        return lp;
    }

    @Override
    public Collection<AProject> getProjects() {
        synchronized (localProjects) {
            ArrayList<AProject> projects = new ArrayList<>(localProjects.values());
            projects.sort(PROJECTS_COMPARATOR);
            return projects;
        }
    }

    protected WorkspaceUser getUser() {
        return user;
    }

    @Override
    public boolean hasProject(String repositoryId, String name) {
        synchronized (localProjects) {
            return localProjects.get(new ProjectKey(repositoryId, name.toLowerCase())) != null;
        }
    }

    private void loadProjects() {
        List<FileData> folders = localRepository.listFolders("");
        for (FileData folder : folders) {
            AProject lpi;
            String name = folder.getName();
            ProjectState projectState = localRepository.getProjectState(name);
            LocalRepository repository = getRepository(projectState.getRepositoryId());
            FileData fileData = projectState.getFileData();
            if (fileData == null) {
                String version = projectState.getProjectVersion();
                lpi = new AProject(repository, name, version);
            } else {
                lpi = new AProject(repository, fileData);
            }
            synchronized (localProjects) {
                localProjects.put(new ProjectKey(repository.getId(), name.toLowerCase()), lpi);
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
    public void removeWorkspaceListener(LocalWorkspaceListener listener) {
        listeners.remove(listener);
    }
}
