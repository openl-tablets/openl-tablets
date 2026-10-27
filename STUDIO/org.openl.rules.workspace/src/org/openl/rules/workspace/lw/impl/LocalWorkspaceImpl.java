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
import org.jspecify.annotations.Nullable;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.impl.local.InPlaceRepository;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.repository.IgnoredPaths;
import org.openl.rules.repository.PathCheckedRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RepositoryDelegate;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.rules.workspace.ProjectKey;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.dtr.impl.MappedRepository;
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
    /**
     * Whether the projects of a design repository on the file system are edited where they are, with no copy in the
     * workspace.
     */
    private final boolean inPlace;
    /** The paths the workspace copies neither show nor change, as the design repositories do. */
    private final IgnoredPaths ignoredPaths;

    LocalWorkspaceImpl(String userId,
                       File location,
                       DesignTimeRepository designTimeRepository,
                       MetainfoRegistry metainfoRegistry,
                       boolean inPlace,
                       IgnoredPaths ignoredPaths) {
        this.userId = userId;
        this.location = location;
        this.designTimeRepository = designTimeRepository;
        this.metainfoRegistry = metainfoRegistry;
        this.inPlace = inPlace;
        this.ignoredPaths = ignoredPaths;

        localRepository = new LocalRepository(location.toPath(), metainfoRegistry);
        localRepository.ignore(ignoredPaths);
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
        repository.ignore(ignoredPaths);
        return named(repository, id);
    }

    /** Gives the repository the id and the name of the design repository it holds the projects of. */
    private <T extends LocalRepository> T named(T repository, String id) {
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
     * Reads the projects of the workspace from its registry, and the projects edited in place, sorted by name: the
     * order stays the same from one start to the next.
     */
    private LocalProjects loadProjects() {
        var projects = new HashMap<ProjectKey, AProject>();
        for (String name : metainfoRegistry.projects()) {
            addRecordedProject(projects, name);
        }
        addInPlaceProjects(projects);
        var sorted = new LinkedHashMap<ProjectKey, AProject>();
        projects.entrySet()
                .stream()
                .sorted(Map.Entry.<ProjectKey, AProject>comparingByValue(PROJECTS_COMPARATOR)
                        .thenComparing(Map.Entry::getKey, KEY_ORDER))
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        return new LocalProjects(sorted);
    }

    /** Adds the workspace copy the registry records under the name. */
    private void addRecordedProject(Map<ProjectKey, AProject> projects, String name) {
        var projectState = localRepository.getProjectState(name);
        AProject lpi;
        var repositoryPath = designTimeRepository.getRulesLocation() + name;
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

    /**
     * Adds the projects edited in place: every project of a design repository on the file system, as it is there.
     *
     * <p>A copy of the project in the workspace, opened before, stays while it holds changes that are not saved yet,
     * so that they can be saved. A copy without changes of its own gives way to the folder of the design repository.
     */
    private void addInPlaceProjects(Map<ProjectKey, AProject> projects) {
        if (!inPlace || designTimeRepository == null) {
            return;
        }
        var repositories = new HashMap<String, Optional<InPlaceRepository>>();
        for (var designProject : designTimeRepository.getProjects()) {
            var repositoryId = designProject.getRepository().getId();
            var designData = designProject.getFileData();
            var repository = repositories.computeIfAbsent(repositoryId, this::inPlaceRepository);
            if (designData != null && repository.isPresent()) {
                var folder = designProject.getRealPath();
                var fileData = new FileData();
                fileData.setName(folder);
                var mappingData = designData.getAdditionalData(FileMappingData.class);
                fileData.addAdditionalData(mappingData == null
                        ? new FileMappingData(designData.getName(), folder)
                        : mappingData);
                var key = new ProjectKey(repositoryId, folder);
                var copy = projects.get(key);
                if (copy == null || !metainfoRegistry.isDirty(copy.getName())) {
                    projects.put(key, new AProject(repository.get(), fileData));
                }
            }
        }
    }

    /**
     * The repository the projects of the design repository are edited in, or nothing when they are copied into the
     * workspace.
     */
    private Optional<InPlaceRepository> inPlaceRepository(String repositoryId) {
        return Optional.ofNullable(inPlaceDesign(repositoryId))
                .map(design -> named(new InPlaceRepository(design.getRoot(),
                        location.toPath(),
                        metainfoRegistry,
                        design.getIgnoredPaths()), repositoryId));
    }

    /**
     * The design repository on the file system whose projects are edited in place, or {@code null} when they are
     * copied into the workspace.
     *
     * <p>Projects are edited in place only in single-user mode, and only in a design repository on the file system.
     */
    private @Nullable FileSystemRepository inPlaceDesign(String repositoryId) {
        var repository = designTimeRepository.getRepository(repositoryId);
        while (repository != null) {
            if (repository instanceof FileSystemRepository fileSystem) {
                return fileSystem.supports().isLocal() ? fileSystem : null;
            }
            repository = unwrap(repository);
        }
        return null;
    }

    private static @Nullable Repository unwrap(Repository repository) {
        return switch (repository) {
            case MappedRepository mapped -> mapped.getDelegate();
            case PathCheckedRepository checked -> checked.getDelegate();
            case RepositoryDelegate delegate -> delegate.getOriginal();
            default -> null;
        };
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
