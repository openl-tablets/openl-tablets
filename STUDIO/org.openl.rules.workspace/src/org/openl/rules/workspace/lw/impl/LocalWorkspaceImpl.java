package org.openl.rules.workspace.lw.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import lombok.AccessLevel;
import lombok.Getter;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.workspace.ProjectKey;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.lw.LocalProjects;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.LocalWorkspaceListener;

public class LocalWorkspaceImpl implements LocalWorkspace {
    private static final Comparator<AProject> PROJECTS_COMPARATOR = (o1, o2) -> o1.getName()
            .compareToIgnoreCase(o2.getName());
    /** Orders the projects whose names differ only in case, so the order does not depend on hashing. */
    private static final Comparator<ProjectKey> KEY_ORDER = Comparator.comparing(ProjectKey::repositoryId)
            .thenComparing(ProjectKey::repositoryPath);

    @Getter(AccessLevel.PROTECTED)
    private final String userId;
    @Getter
    private final File location;
    /**
     * The projects as the last refresh found them. A refresh puts a new view in place of the old one, so a reader
     * sees either view whole and needs no lock.
     */
    private final AtomicReference<LocalProjects> localProjects = new AtomicReference<>(new LocalProjects(Map.of()));
    /** One refresh at a time: a refresh that read the registry earlier must not replace the view of a later one. */
    private final Lock refreshLock = new ReentrantLock();
    private final List<LocalWorkspaceListener> listeners = new ArrayList<>();
    private final LocalRepository localRepository;
    private final DesignTimeRepository designTimeRepository;
    @Getter
    private final MetainfoRegistry metainfoRegistry;

    LocalWorkspaceImpl(String userId,
                       File location,
                       DesignTimeRepository designTimeRepository,
                       MetainfoRegistry metainfoRegistry) {
        this.userId = userId;
        this.location = location;
        this.designTimeRepository = designTimeRepository;
        this.metainfoRegistry = metainfoRegistry;

        localRepository = new LocalRepository(location.toPath(), metainfoRegistry);
        localRepository.initialize();
        localProjects.set(loadProjects());
    }

    @Override
    public void addWorkspaceListener(LocalWorkspaceListener listener) {
        listeners.add(listener);
    }

    @Override
    public LocalRepository getRepository(String id) {
        if (id == null) {
            id = LOCAL_ID;
        }
        // Create a new instance with id and name.
        var repository = new LocalRepository(localRepository.getRoot(), metainfoRegistry);
        repository.setId(id);
        if (designTimeRepository != null) {
            var designRepository = designTimeRepository.getRepository(id);
            if (designRepository != null) {
                repository.setName(designRepository.getName());
            }
        }
        repository.initialize();
        return repository;
    }

    @Override
    public AProject getProject(String repositoryId, String name) throws ProjectException {
        return findProject(repositoryId, name)
                .orElseThrow(() -> new ProjectException("Cannot find project ''{0}''.", null, name));
    }

    @Override
    public LocalProjects snapshot() {
        return localProjects.get();
    }

    @Override
    public Collection<AProject> getProjects() {
        return new ArrayList<>(snapshot().projects());
    }

    @Override
    public List<? extends AProject> getProjects(String repositoryId) {
        return snapshot().projects()
                .stream()
                .filter(project -> Objects.equals(repositoryId, project.getRepository().getId()))
                .toList();
    }

    @Override
    public boolean hasProject(String repositoryId, String name) {
        return findProject(repositoryId, name).isPresent();
    }

    private Optional<AProject> findProject(String repositoryId, String name) {
        return snapshot().projects()
                .stream()
                .filter(p -> (repositoryId == null || repositoryId.equals(p.getRepository().getId())) && p.getName()
                        .equalsIgnoreCase(name))
                .findFirst();
    }

    /**
     * Reads the projects of the workspace from its registry, sorted by name: the order stays the same from one start
     * to the next.
     */
    private LocalProjects loadProjects() {
        var projects = new HashMap<ProjectKey, AProject>();
        for (String name : metainfoRegistry.projects()) {
            AProject lpi;
            var repositoryPath = designTimeRepository.getRulesLocation() + name;
            var projectState = localRepository.getProjectState(name);
            var repository = getRepository(projectState.getRepositoryId());
            var fileData = projectState.getFileData();
            if (fileData == null) {
                var version = projectState.getProjectVersion();
                lpi = new AProject(repository, name, version);
                repositoryPath = "<local-path>/" + name;
            } else {
                var mappingData = fileData.getAdditionalData(FileMappingData.class);
                if (mappingData != null) {
                    repositoryPath = mappingData.getInternalPath();
                    mappingData.setExternalPath(getExternalPath(repository, name, repositoryPath));
                }
                lpi = new AProject(repository, fileData);
            }
            projects.put(new ProjectKey(repository.getId(), repositoryPath), lpi);
        }
        var sorted = new LinkedHashMap<ProjectKey, AProject>();
        projects.entrySet()
                .stream()
                .sorted(Map.Entry.<ProjectKey, AProject>comparingByValue(PROJECTS_COMPARATOR)
                        .thenComparing(Map.Entry::getKey, KEY_ORDER))
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        return new LocalProjects(sorted);
    }

    /**
     * The path the design repository shows the project folder under.
     */
    private String getExternalPath(LocalRepository repository, String name, String repositoryPath) {
        var mappedName = name;
        var designRepo = designTimeRepository.getRepository(repository.getId());
        var rulesLocation = designTimeRepository.getRulesLocation();
        if (designRepo != null && designRepo.supports().mappedFolders()) {
            var mapper = (FolderMapper) designRepo;
            var mappedPath = mapper.findMappedName(repositoryPath);
            if (mappedPath == null) {
                mappedName = mapper.getMappedName(name, repositoryPath);
            } else {
                mappedName = mappedPath.startsWith(rulesLocation)
                        ? mappedPath
                        .substring(rulesLocation.length())
                        : mappedPath;
            }
        }
        return rulesLocation + mappedName;
    }

    @Override
    public void refresh() {
        refreshLock.lock();
        try {
            localProjects.set(loadProjects());
        } finally {
            refreshLock.unlock();
        }
    }

    @Override
    public void release() {
        refreshLock.lock();
        try {
            localProjects.set(new LocalProjects(Map.of()));
        } finally {
            refreshLock.unlock();
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
