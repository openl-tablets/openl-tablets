package org.openl.rules.workspace.dtr.impl;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.env.PropertyResolver;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Features;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.ProjectKey;
import org.openl.rules.workspace.dtr.BranchedProject;
import org.openl.rules.workspace.dtr.BranchedProject.BranchEntry;
import org.openl.rules.workspace.dtr.BranchedProjectIndexService;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

/**
 * @author Aleh Bykhavets
 */
@Slf4j
public class DesignTimeRepositoryImpl implements DesignTimeRepository {

    private static final String DESIGN_REPOSITORIES = "design-repository-configs";

    @Getter
    private volatile List<Repository> repositories;
    @Getter
    private volatile String rulesLocation;
    private volatile boolean projectsRefreshNeeded = true;
    private volatile boolean destroyed;

    /**
     * Project Cache
     */
    private final HashMap<ProjectKey, AProject> projects = new HashMap<>();
    private final HashMap<ProjectKey, AProject> projectsVersions = new HashMap<>();
    private final HashMap<ProjectKey, BranchedProject> branchedProjects = new HashMap<>();
    private final Map<String, Map<ProjectKey, AProject>> configuredBranchFallbacks = new HashMap<>();

    private final List<DesignTimeRepositoryListener> listeners = new ArrayList<>();

    private final PropertyResolver propertyResolver;
    private final BranchedProjectIndexService indexService;

    @Getter
    private final List<String> exceptions = new ArrayList<>();

    public DesignTimeRepositoryImpl(PropertyResolver propertyResolver) {
        this(propertyResolver, new BranchedProjectIndexService());
    }

    public DesignTimeRepositoryImpl(PropertyResolver propertyResolver,
                                    BranchedProjectIndexService indexService) {
        this.propertyResolver = propertyResolver;
        this.indexService = indexService;
    }

    public void init() {
        synchronized (projects) {
            if (repositories != null) {
                return;
            }

            repositories = new ArrayList<>();

            rulesLocation = getBasePath();
            var designRepositories = Objects.requireNonNull(propertyResolver.getProperty(DESIGN_REPOSITORIES))
                    .split("\\s*,\\s*", -1);
            for (String repoId : designRepositories) {

                var repository = createRepo(repoId, rulesLocation);

                repositories.add(repository);
                if (isBranchRepository(repository) && repository instanceof BranchRepository branchRepository) {
                    configuredBranchFallbacks.put(repository.getId(), scanProjects(repository));
                    // The callback runs after both the early default-branch snapshot and the complete one, so the
                    // workspace refreshes as soon as the default branch's projects are mapped across their branches.
                    indexService.register(branchRepository, rulesLocation, this::indexPublished);
                    repository.setListener(new RepositoryListener(() -> repositoryChanged(repository)));
                } else {
                    repository.setListener(new RepositoryListener(() -> repositoryChanged(repository)));
                }
            }
            repositories = repositories.stream()
                    .filter(r -> Objects.nonNull(r.getName()))
                    .sorted(Comparator.comparing(Repository::getName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
            refreshProjects();
        }
    }

    private String getBasePath() {
        var repoPrefix = Comments.REPOSITORY_PREFIX + "design";
        var basePath = propertyResolver.getProperty(repoPrefix + ".base.path");
        if (basePath == null) {
            basePath = propertyResolver.getProperty("repo-default.design.base.path");
        }
        if (StringUtils.isNotEmpty(basePath) && !basePath.endsWith("/")) {
            basePath += "/";
        }
        return basePath;
    }

    protected Repository createRepo(String configName, String baseFolder) {
        Repository repo = null;
        try {
            var repoPrefix = Comments.REPOSITORY_PREFIX + configName;
            repo = RepositoryInstatiator.newRepository(repoPrefix, propertyResolver::getProperty);

            if (repo.supports().folders()) {
                // Nested folder structure is supported for FolderRepository only
                repo = MappedRepository.create(repo, baseFolder);
            }

            return repo;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            // If exception is thrown, we must close repository in this method.
            // If no exception, repository will be closed later.
            if (repo != null) {
                IOUtils.closeQuietly(repo);
            }

            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause == null) {
                rootCause = e;
            }
            String message;
            if (rootCause.getMessage() == null || !(rootCause instanceof IOException)) {
                // For some exceptions like ClassNotFoundException the messages aren't understandable for a user. Use
                // default.
                message = "Repository configuration is incorrect. Please change configuration.";
            } else {
                message = rootCause.getMessage();
            }

            return (Repository) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{Repository.class},
                    (proxy, method, args) -> {
                        final var methodName = method.getName();
                        final Class<?> returnType = method.getReturnType();
                        if (methodName.startsWith("set") && returnType == void.class) {
                            return null;
                        } else if ("supports".equals(methodName) && returnType == Features.class) {
                            return new FeaturesBuilder(null).setVersions(false).build();
                        } else if ("close".equals(methodName) && returnType == void.class && args == null) {
                            return null;
                        } else if ("getId".equals(methodName) && returnType == String.class) {
                            return configName;
                        }
                        var repoName = propertyResolver.getProperty(Comments.REPOSITORY_PREFIX + configName + ".name");
                        if ("getName".equals(methodName) && returnType == String.class) {
                            return repoName;
                        }
                        throw new IllegalStateException(message);
                    });
        }
    }

    @Override
    public AProject getProject(String repositoryId, String name) throws ProjectException {
        synchronized (projects) {
            if (projectsRefreshNeeded) {
                refreshProjects();
            }

            var projectKey = projectKey(repositoryId, name);

            var cached = projects.get(projectKey);
            if (cached != null) {
                return cached;
            } else {
                Optional<AProject> project = projects.values()
                        .stream()
                        .filter(p -> p.getRepository().getId().equals(repositoryId)
                                && p.getBusinessName().equalsIgnoreCase(name))
                        .findFirst();
                if (project.isPresent()) {
                    return project.get();
                }
                throw new ProjectException("Project '{0}' is not found.", null, name);
            }
        }
    }

    @Override
    public Optional<BranchedProject> getBranchedProject(String repositoryId, String name) {
        synchronized (projects) {
            if (projectsRefreshNeeded) {
                refreshProjects();
            }
            var direct = branchedProjects.get(projectKey(repositoryId, name));
            if (direct != null) {
                return Optional.of(direct);
            }
            return branchedProjects.values()
                    .stream()
                    .filter(project -> project.homeEntry().project().getRepository().getId().equals(repositoryId))
                    .filter(project -> project.homeEntry().project().getBusinessName().equalsIgnoreCase(name))
                    .findFirst();
        }
    }

    @Override
    public List<AProject> getProjectsHeldOnlyBy(String repositoryId, String branch) {
        synchronized (projects) {
            if (projectsRefreshNeeded) {
                refreshProjects();
            }
            return branchedProjects.values()
                    .stream()
                    .filter(project -> project.homeEntry().project().getRepository().getId().equals(repositoryId))
                    .filter(project -> project.heldOnlyBy(branch))
                    .map(project -> project.entry(branch).map(BranchEntry::project).orElse(null))
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    @Override
    public Optional<BranchedProjectIndexService.IndexHealth> getProjectIndexHealth(String repositoryId) {
        var repository = getRepository(repositoryId);
        if (!isBranchRepository(repository)) {
            return Optional.empty();
        }
        return Optional.of(indexService.getSnapshot(repositoryId).health());
    }

    @Override
    public AProject getProject(String repositoryId, String name, CommonVersion version) {
        var repoVersion = version.getVersionName();
        var key = new ProjectKey(repositoryId, "%s:%s".formatted(name, repoVersion));
        var project = projectsVersions.get(key);

        if (project == null) {
            var repository = getRepository(repositoryId);
            if (repository == null) {
                return null;
            }
            var projectPath = rulesLocation + name;

            if (repository.supports().branches()) {
                try {
                    var branchedProject = getBranchedProject(repositoryId, name);
                    if (branchedProject.isPresent()) {
                        for (var entry : branchedProject.orElseThrow().entries().values()) {
                            var branchProject = entry.project();
                            var fileData = branchProject.getRepository()
                                    .checkHistory(branchProject.getFolderPath(), repoVersion);
                            if (fileData != null) {
                                project = new AProject(branchProject.getRepository(), fileData);
                                break;
                            }
                        }
                    }
                    if (project == null) {
                        log.warn("Project '{}' with version '{}' is not found.", name, repoVersion);
                        project = new AProject(repository, projectPath, repoVersion);
                    }
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    project = new AProject(repository, projectPath, repoVersion);
                }
            } else {
                project = new AProject(repository, projectPath, repoVersion);
            }

            projectsVersions.put(key, project);
        }
        return project;
    }

    @Override
    public AProject getProjectByPath(String repositoryId,
                                     String branch,
                                     String path,
                                     String version) throws IOException {
        synchronized (projects) {
            if (projectsRefreshNeeded) {
                refreshProjects();
            }

            if (branch != null) {
                var branchProject = branchedProjects.values()
                        .stream()
                        .filter(project -> project.homeEntry().project().getRepository().getId().equals(repositoryId))
                        .map(project -> project.entry(branch).orElse(null))
                        .filter(Objects::nonNull)
                        .map(BranchEntry::project)
                        .filter(project -> project.getRealPath().equals(path))
                        .findFirst();
                if (branchProject.isPresent()) {
                    var project = branchProject.get();
                    return new AProject(project.getRepository(), project.getFolderPath(), version);
                }
            }

            Optional<AProject> project = projects.values()
                    .stream()
                    .filter(p -> p.getRepository().getId().equals(repositoryId) && p.getRealPath().equals(path))
                    .findFirst();
            if (project.isPresent()) {
                var repository = project.get().getRepository();
                if (branch != null && repository.supports().branches()) {
                    repository = ((BranchRepository) repository).forBranch(branch);
                }
                return new AProject(repository, project.get().getFolderPath(), version);
            }
        }
        return null;
    }

    @Override
    public void refresh() {
        var hasNonBranchedRepository = false;
        var currentRepositories = repositories;
        if (currentRepositories == null) {
            synchronized (projects) {
                projectsRefreshNeeded = true;
            }
            return;
        }
        for (Repository repository : currentRepositories) {
            if (isBranchRepository(repository)) {
                indexService.invalidateRepository(repository.getId());
            } else {
                hasNonBranchedRepository = true;
            }
        }
        synchronized (projects) {
            projectsRefreshNeeded |= hasNonBranchedRepository;
        }
    }

    @Override
    public CompletionStage<Void> refreshBranch(String repositoryId, String branch) {
        var repository = getRepository(repositoryId);
        if (!isBranchRepository(repository)) {
            refresh();
            return CompletableFuture.completedFuture(null);
        }
        // Publishing a snapshot refreshes the projection on its own (see init); the stage only reports completion.
        return indexService.invalidateBranch(repositoryId, branch).thenRun(() -> {
        });
    }

    @Override
    public CompletionStage<Void> refreshRepository(String repositoryId) {
        var repository = getRepository(repositoryId);
        if (!isBranchRepository(repository)) {
            refresh();
            return CompletableFuture.completedFuture(null);
        }
        return indexService.invalidateRepository(repositoryId).thenRun(() -> {
        });
    }

    @Override
    public Collection<AProject> getProjects() {
        List<AProject> result;

        synchronized (projects) {
            if (projectsRefreshNeeded) {
                refreshProjects();
            }

            result = new ArrayList<>(projects.values());
        }

        result.sort(Comparator.comparing(AProjectFolder::getName, String.CASE_INSENSITIVE_ORDER));

        return result;
    }

    @Override
    public List<? extends AProject> getProjects(String repositoryId) {
        synchronized (projects) {
            if (projectsRefreshNeeded) {
                refreshProjects();
            }
            return projects.entrySet()
                    .stream()
                    .filter(entry -> Objects.equals(repositoryId, entry.getKey().repositoryId()))
                    .map(Map.Entry::getValue)
                    .sorted(Comparator.comparing(AProjectFolder::getName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
        }
    }

    private void refreshProjects() {
        projects.clear();
        projectsVersions.clear();
        branchedProjects.clear();
        exceptions.clear();
        for (Repository repository : repositories) {
            if (isBranchRepository(repository) && repository instanceof BranchRepository branchRepository) {
                var snapshot = indexService.getSnapshot(repository.getId());
                if (!snapshot.published()) {
                    // Nothing indexed yet: fall back to the default branch until the first snapshot is published.
                    projects.putAll(configuredBranchFallbacks.getOrDefault(repository.getId(), Map.of()));
                } else {
                    addSnapshotProjects(branchRepository, snapshot);
                    if (snapshot.health().state() == BranchedProjectIndexService.IndexState.DEGRADED &&
                            !snapshot.branches().containsKey(branchRepository.getBranch())) {
                        configuredBranchFallbacks.getOrDefault(repository.getId(), Map.of())
                                .forEach(projects::putIfAbsent);
                    }
                }
                var error = snapshot.health().lastError();
                if (error != null) {
                    exceptions.add("Repository '%s' : %s".formatted(repository.getName(), error));
                }
            } else {
                projects.putAll(scanProjects(repository));
            }
        }

        projectsRefreshNeeded = false;
    }

    private void addSnapshotProjects(BranchRepository repository,
                                     BranchedProjectIndexService.RepositorySnapshot snapshot) {
        snapshot.projects().values().forEach(projectSnapshot -> {
            var entries = new LinkedHashMap<String, BranchEntry>();
            projectSnapshot.entries().forEach((branch, indexedProject) -> {
                var branchSnapshot = snapshot.branches().get(branch);
                if (branchSnapshot != null) {
                    entries.put(branch,
                            new BranchEntry(
                                    new AProject(indexedProject.repository(), indexedProject.fileData()),
                                    branchSnapshot.status()));
                }
            });
            if (!entries.isEmpty()) {
                var project = BranchedProject.create(projectSnapshot.name(), repository.getBaseBranch(), entries);
                var key = projectKey(repository.getId(), project.name());
                branchedProjects.put(key, project);
                projects.put(key, project.homeEntry().project());
            }
        });
    }

    private Map<ProjectKey, AProject> scanProjects(Repository repository) {
        Collection<FileData> fileDatas = List.of();
        try {
            fileDatas = repository.supports().folders()
                    ? repository.listFolders(rulesLocation)
                    : repository.list(rulesLocation);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            exceptions.add("Repository '%s' : %s".formatted(repository.getName(), e.getMessage()));
        }

        var result = new LinkedHashMap<ProjectKey, AProject>();
        for (FileData fileData : fileDatas) {
            var project = new AProject(repository, fileData);
            if (!project.isDeleted()) {
                result.putIfAbsent(projectKey(repository.getId(), project.getName()), project);
            }
        }
        return result;
    }

    @Override
    public boolean hasProject(String repositoryId, String name) {
        synchronized (projects) {
            if (projectsRefreshNeeded) {
                refreshProjects();
            }
            // Check full name for mapped repositories
            var project = projects.get(projectKey(repositoryId, name));
            if (project != null) {
                return true;
            }

            // Check business name
            return projects.values()
                    .stream()
                    .anyMatch(p -> p.getRepository().getId().equals(repositoryId)
                            && p.getBusinessName().equalsIgnoreCase(name));
        }
    }

    // --- private

    @Override
    public void addListener(DesignTimeRepositoryListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(DesignTimeRepositoryListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * destroy-method
     */
    public void destroy() throws Exception {
        destroyed = true;
        indexService.close();
        synchronized (projects) {
            if (repositories != null) {
                for (Repository repository : repositories) {
                    repository.setListener(null);
                    repository.close();
                }
                repositories = null;
            }

            projects.clear();
            projectsVersions.clear();
            branchedProjects.clear();
            configuredBranchFallbacks.clear();
        }
    }

    @Override
    public Repository getRepository(String id) {
        return repositories.stream()
                .filter(repository -> Objects.equals(id, repository.getId()))
                .findFirst()
                .orElse(null);
    }

    private void repositoryChanged(Repository repository) {
        if (destroyed) {
            return;
        }
        if (isBranchRepository(repository)) {
            indexService.invalidateRepository(repository.getId());
        } else {
            synchronized (projects) {
                projectsRefreshNeeded = true;
            }
            notifyListeners();
        }
    }

    private void indexPublished() {
        if (destroyed) {
            return;
        }
        synchronized (projects) {
            projectsRefreshNeeded = true;
            refreshProjects();
        }
        notifyListeners();
    }

    private void notifyListeners() {
        List<DesignTimeRepositoryListener> localListeners;
        synchronized (listeners) {
            localListeners = new ArrayList<>(listeners);
        }
        for (DesignTimeRepositoryListener listener : localListeners) {
            listener.onRepositoryModified();
        }
    }

    private static boolean isBranchRepository(Repository repository) {
        return repository instanceof BranchRepository && repository.supports().branches();
    }

    private static ProjectKey projectKey(String repositoryId, String projectName) {
        return new ProjectKey(repositoryId, projectName.toLowerCase(Locale.ROOT));
    }

    private static class RepositoryListener implements Listener {
        private final Runnable callback;

        private RepositoryListener(Runnable callback) {
            this.callback = callback;
        }

        @Override
        public void onChange() {
            callback.run();
        }
    }
}
