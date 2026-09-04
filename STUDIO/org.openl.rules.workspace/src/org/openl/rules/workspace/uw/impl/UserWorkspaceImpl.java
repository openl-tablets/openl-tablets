package org.openl.rules.workspace.uw.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.xml.sax.InputSource;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.ProjectKey;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.BranchedProject;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceListener;

@Slf4j
public class UserWorkspaceImpl implements UserWorkspace {

    private static final Comparator<AProject> PROJECTS_COMPARATOR = Comparator
            .comparing(AProject::getBusinessName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(o -> o.getRepository().getId())
            .thenComparing(AProject::getRealPath, String.CASE_INSENSITIVE_ORDER);

    @Getter
    private final WorkspaceUser user;
    private final LocalWorkspace localWorkspace;
    @Getter
    private final DesignTimeRepository designTimeRepository;
    private final ProjectBranchPreferenceStore branchPreferences;

    private final HashMap<ProjectKey, RulesProject> userRulesProjects;
    private final HashMap<String, List<ProjectKey>> rulesProjectKeysByName;

    private volatile boolean projectsRefreshNeeded = true;
    private volatile boolean syncNeeded = true;
    private volatile boolean cleanUpOnActivation = false;

    private final List<UserWorkspaceListener> listeners = new ArrayList<>();
    @Getter
    private final LockEngine projectsLockEngine;
    private final DesignTimeRepositoryListener designRepoListener = this::designRepositoryRefreshed;

    public UserWorkspaceImpl(WorkspaceUser user,
                             LocalWorkspace localWorkspace,
                             DesignTimeRepository designTimeRepository,
                             LockEngine projectsLockEngine) {
        this.user = user;
        this.localWorkspace = localWorkspace;
        this.designTimeRepository = designTimeRepository;
        this.projectsLockEngine = projectsLockEngine;
        branchPreferences = ProjectBranchPreferenceStore.open(localWorkspace.getLocation().toPath());

        userRulesProjects = new HashMap<>();
        rulesProjectKeysByName = new HashMap<>();
    }

    @Override
    public void activate() {
        cleanUpOnActivation = true;
        refresh();
    }

    @Override
    public void addWorkspaceListener(UserWorkspaceListener listener) {
        listeners.add(listener);
        designTimeRepository.addListener(designRepoListener);
    }

    // --- protected

    @Override
    public LocalWorkspace getLocalWorkspace() {
        if (syncNeeded) {
            // We must ensure that all folders are renamed to correct names before using local workspace.
            doSyncProjects();
        }
        return localWorkspace;
    }

    @Override
    public RulesProject getProject(String repositoryId, String name) throws ProjectException {
        // FIXME: This method has performance issues and should be optimized.
        //  Currently, it refreshes all projects in the workspace every time it is called,
        //  which results in significant performance degradation.
        //  Ideally, the workspace should only be refreshed when necessary.
        return getProject(repositoryId, name, true);
    }

    @Override
    public RulesProject getProject(String repositoryId, String name, boolean refreshBefore) throws ProjectException {
        if (refreshBefore || projectsRefreshNeeded) {
            refreshRulesProjects();
        }

        RulesProject uwp;
        synchronized (userRulesProjects) {
            uwp = userRulesProjects.get(new ProjectKey(repositoryId, name.toLowerCase(Locale.ROOT)));
        }

        if (uwp == null) {
            throw new ProjectException("Cannot find project ''{0}'' or access to the project is not permitted.",
                    null,
                    name);
        }

        return uwp;
    }

    @Override
    public void setProjectBranch(RulesProject project, String branch) throws ProjectException {
        var repositoryId = project.getDesignRepository().getId();
        var projectName = getDesignProjectName(project);
        var designProject = designTimeRepository.getBranchedProject(repositoryId, projectName)
                .flatMap(branchedProject -> branchedProject.entry(branch))
                .map(BranchedProject.BranchEntry::project)
                .orElseThrow(() -> new ProjectException(
                        "Project ''{0}'' is not found in branch ''{1}''.", null, projectName, branch));
        project.setBranch(designProject.getRepository(), designProject.getFileData());
        branchPreferences.put(repositoryId, projectName, branch);
    }

    @Override
    public Collection<RulesProject> getProjects() {
        // FIXME: This method has performance issues and should be optimized.
        //  Currently, it refreshes all projects in the workspace every time it is called,
        //  which results in significant performance degradation.
        //  Ideally, the workspace should only be refreshed when necessary.
        return getProjects(true);
    }

    @Override
    public List<RulesProject> getProjects(String repositoryId) {
        if (projectsRefreshNeeded) {
            refreshRulesProjects();
        }
        synchronized (userRulesProjects) {
            return userRulesProjects.entrySet()
                    .stream()
                    .filter(entry -> Objects.equals(repositoryId, entry.getKey().repositoryId()))
                    .map(Map.Entry::getValue)
                    .sorted(PROJECTS_COMPARATOR)
                    .toList();
        }
    }

    @Override
    public Collection<RulesProject> getProjects(boolean refreshBefore) {
        if (refreshBefore || projectsRefreshNeeded) {
            refreshRulesProjects();
        }

        ArrayList<RulesProject> result;
        synchronized (userRulesProjects) {
            result = new ArrayList<>(userRulesProjects.values());
        }

        result.sort(PROJECTS_COMPARATOR);

        return result;
    }

    @Override
    public Collection<RulesProject> getProjectsByName(String name) {
        return getProjectsByName(name, true);
    }

    @Override
    public Collection<RulesProject> getProjectsByName(String name, boolean refreshBefore) {
        if (refreshBefore || projectsRefreshNeeded) {
            refreshRulesProjects();
        }
        synchronized (userRulesProjects) {
            var keys = rulesProjectKeysByName.get(name.toLowerCase(Locale.ROOT));
            if (keys == null || keys.isEmpty()) {
                return List.of();
            }
            return keys.stream()
                    .map(userRulesProjects::get)
                    .filter(Objects::nonNull)
                    .sorted(PROJECTS_COMPARATOR)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public Optional<RulesProject> getProjectByPath(String repositoryId, String realPath) {
        return getProjects(false).stream()
                .filter(p -> !p.isLocalOnly() && repositoryId.equals(p.getDesignRepository()
                        .getId()) && ((realPath.equals(p.getRealPath())) || realPath.startsWith(p.getRealPath() + "/")))
                .findFirst();
    }

    @Override
    public boolean hasProject(String repositoryId, String name) {
        synchronized (userRulesProjects) {
            if (projectsRefreshNeeded) {
                refreshRulesProjects();
            }
            return userRulesProjects.containsKey(new ProjectKey(repositoryId, name.toLowerCase(Locale.ROOT)));
        }
    }

    @Override
    public void passivate() {
        synchronized (userRulesProjects) {
            clearRulesProjectsCache();
        }
        scheduleProjectsRefresh();

        cleanUpOnActivation = false;
    }

    @Override
    public void refresh() {
        localWorkspace.refresh();
        scheduleProjectsRefresh();
    }

    @Override
    public void syncProjects() {
        syncNeeded = true;
    }

    private void doSyncProjects() {
        syncNeeded = false;

        var anyProjectRenamed = false;
        for (RulesProject rPr : getProjects(false)) {
            var repository = designTimeRepository.getRepository(rPr.getRepository().getId());
            if (repository != null && repository.supports().mappedFolders()) {
                if (rPr.isOpened() && !rPr.isLocalOnly()) {
                    try {
                        var realProjectName = getActualName(rPr);
                        if (!rPr.getLocalFolderName().equals(realProjectName)) {
                            // We cannot close and then open a project in workspace, we should rename the folder
                            // in file system directly. Otherwise we will lose unsaved user changes.
                            var prevPath = rPr.getFolderPath();
                            var index = prevPath.lastIndexOf('/');
                            var newPath = prevPath.substring(0, index + 1) + realProjectName;
                            anyProjectRenamed |= localWorkspace.getMetainfoRegistry()
                                    .renameProjectFolder(prevPath, newPath);
                        }
                    } catch (Exception e) {
                        log.warn("Could not rename the project '{}' because of error: {}",
                                rPr.getName(),
                                e.getMessage(), e);
                    }
                }
            }
        }

        if (anyProjectRenamed) {
            // We need to recreate projects list in user workspace.
            refreshRulesProjects();
        }
    }

    @Override
    public String getActualName(AProject project) throws ProjectException, IOException {
        if (project.hasArtefact(ProjectDescriptor.FILE_NAME)) {
            var artefact = project
                    .getArtefact(ProjectDescriptor.FILE_NAME);
            if (artefact instanceof AProjectResource resource) {
                try (var content = resource.getContent()) {
                    return getActualName(content);
                }
            }
        }
        var actualPath = project.getRealPath();
        return actualPath.substring(actualPath.lastIndexOf('/') + 1);
    }

    private String getActualName(InputStream inputStream) {
        try {
            var inputSource = new InputSource(inputStream);
            XPathFactory factory = XPathFactory.newInstance();
            var xPath = factory.newXPath();
            var xPathExpression = xPath.compile("/project/name");
            return xPathExpression.evaluate(inputSource);
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    private void clearRulesProjectsCache() {
        userRulesProjects.clear();
        rulesProjectKeysByName.clear();
    }

    private static ProjectKey projectKey(String repositoryId, String projectName) {
        return new ProjectKey(repositoryId, projectName.toLowerCase(Locale.ROOT));
    }

    private void putRulesProject(RulesProject project) {
        var repoId = project.getRepository().getId();
        var key = projectKey(repoId, project.getName());
        userRulesProjects.put(key, project);
        var businessNameKey = project.getBusinessName().toLowerCase(Locale.ROOT);
        rulesProjectKeysByName.computeIfAbsent(businessNameKey, k -> new ArrayList<>()).add(key);
    }

    private void scheduleProjectsRefresh() {
        scheduleProjectsRefresh(true);
    }

    private void designRepositoryRefreshed() {
        scheduleProjectsRefresh(false);
    }

    private void scheduleProjectsRefresh(boolean refreshDesignRepository) {
        synchronized (userRulesProjects) {
            projectsRefreshNeeded = true;
            if (refreshDesignRepository) {
                designTimeRepository.refresh();
            }
        }
        for (UserWorkspaceListener listener : listeners) {
            listener.workspaceRefreshed();
        }
    }

    private void refreshRulesProjects() {
        localWorkspace.refresh();

        synchronized (userRulesProjects) {
            for (RulesProject project : userRulesProjects.values()) {
                if (!project.isOpened() && project.isSupportsBranches()) {
                    branchPreferences.put(project.getDesignRepository().getId(),
                            getDesignProjectName(project),
                            project.getBranch());
                }
            }

            clearRulesProjectsCache();

            var designProjects = designTimeRepository.getProjects();

            // The folders the design side accounts for, per repository. A workspace copy sitting in one of them
            // belongs to the project that holds it, so it must not be claimed by another project that merely
            // shares its name. Repositories are kept apart, because two of them may hold the very same folder.
            var designFolders = designProjects.stream()
                    .collect(Collectors.groupingBy(project -> project.getRepository().getId(),
                            Collectors.mapping(AProject::getRealPath, Collectors.toSet())));

            // add new
            for (AProject rp : designProjects) {
                var repoId = rp.getRepository().getId();
                var localRepository = localWorkspace.getRepository(repoId);
                var name = rp.getName();
                var branchedProject = designTimeRepository.getBranchedProject(repoId, name);
                var lp = findLocalProject(repoId, rp, designFolders.getOrDefault(repoId, Set.of()));

                FileData local = lp == null ? null : lp.getFileData();

                var selectedProject = rp;
                var closeProject = false;

                if (branchedProject.isPresent()) {
                    var selectedBranch = local == null
                            ? branchPreferences.get(repoId, name).orElse(null)
                            : local.getBranch();
                    if (selectedBranch != null) {
                        var branchEntry = branchedProject.get().entry(selectedBranch);
                        if (branchEntry.isPresent()) {
                            selectedProject = branchEntry.get().project();
                        } else if (local != null) {
                            log.info("Close the project '{}' because it does not exist in branch '{}'.",
                                    name,
                                    selectedBranch);
                            closeProject = true;
                        } else {
                            branchPreferences.remove(repoId, name);
                        }
                    }
                }

                var project = new RulesProject(getUser(),
                        localRepository,
                        local,
                        selectedProject.getRepository(),
                        selectedProject.getFileData(),
                        projectsLockEngine);

                if (cleanUpOnActivation) {
                    // Clean ups after session activation (should be done only once).
                    if (!isVersionExistInHistory(project)) {
                        log.warn("The Project '{}' has a version {}, but absents in the history.",
                                project.getName(),
                                project.getHistoryVersion());
                        if (!project.isModified()) {
                            log.warn(
                                    "The project '{}' is not modified and will be closed because it absents in the history.",
                                    project.getName());
                            closeProject = true;
                        }
                    }
                }

                if (closeProject) {
                    try {
                        project.close();
                    } catch (ProjectException e) {
                        log.warn("Cannot close the project {}", project.getName(), e);
                    }
                }
                putRulesProject(project);
            }

            // Workspace projects that have no corresponding project in the main branch of the
            // design repositories: genuine local projects and opened copies whose repository is
            // unavailable or removed or whose project was deleted from the main branch.
            for (AProject lp : localWorkspace.getProjects()) {
                var repoId = lp.getRepository().getId();
                var name = lp.getName();

                if (!userRulesProjects.containsKey(new ProjectKey(repoId, name.toLowerCase(Locale.ROOT)))) {
                    var project = resolveUnmatchedProject(repoId, lp);
                    if (project != null) {
                        putRulesProject(project);
                    }
                }
            }

            projectsRefreshNeeded = false;
            cleanUpOnActivation = false;

            if (syncNeeded) {
                doSyncProjects();
            }
        }
    }

    /**
     * The workspace copy of the project, if the user has one.
     *
     * <p>A copy is recognised by the folder it was taken from. Every branch keeps the project in that one folder,
     * so a single lookup answers for all of them.
     *
     * <p>Only a copy whose folder the design side no longer accounts for is matched by name, because a name is
     * shown by more than one project when branches disagree about it, and claiming a copy of another folder
     * closes it. A copy that does not say which folder it came from is left to the name, as it was before.
     */
    private AProject findLocalProject(String repositoryId, AProject project, Set<String> designFolders) {
        var localProject = localWorkspace.getProjectForPath(repositoryId, project.getRealPath());
        if (localProject != null) {
            return localProject;
        }
        var byName = localWorkspace.getProjectForName(repositoryId, project.getBusinessName());
        if (byName == null) {
            return null;
        }
        var folder = folderOf(byName);
        return folder != null && designFolders.contains(folder) ? null : byName;
    }

    /**
     * The repository folder a workspace copy was taken from, or {@code null} when the copy does not carry it.
     *
     * <p>A copy lives under its own name locally, so its own path tells nothing about the folder. The folder
     * travels with it as mapping data.
     */
    private static @Nullable String folderOf(AProject project) {
        var fileData = project.getFileData();
        var mapping = fileData == null ? null : fileData.getAdditionalData(FileMappingData.class);
        return mapping == null ? null : mapping.getInternalPath();
    }

    private static String getDesignProjectName(RulesProject project) {
        return project.getDesignProjectName();
    }

    /**
     * Resolves a workspace copy that has no design counterpart in this refresh.
     *
     * <p>A copy whose repository was removed or whose project was deleted from the configured main
     * branch is silently closed and resolves to {@code null} regardless of local changes. A copy of
     * an unavailable repository and a genuine local project are served from the last known state.
     *
     * <p>A copy already closed by this refresh also resolves to {@code null}.
     */
    private RulesProject resolveUnmatchedProject(String repoId, AProject lp) {
        var local = lp.getFileData();
        var repository = (LocalRepository) lp.getRepository();

        try {
            if (repository.check(local.getName()) == null) {
                return null;
            }
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
            return null;
        }

        var project = createUnmatchedProject(repoId, repository, local);
        var designRepository = designTimeRepository.getRepository(repoId);
        if (LocalWorkspace.LOCAL_ID.equals(repoId)) {
            return project;
        }

        var designPath = designPath(lp.getName(), local);
        if (designRepository == null) {
            log.info("Close the project '{}' because repository '{}' was removed from the configuration",
                    project.getName(),
                    repoId);
        } else if (isMissingFromMainBranch(designRepository, designPath)) {
            log.info("Close the project '{}' because it does not exist in the main branch of repository '{}'",
                    project.getName(),
                    repoId);
        } else {
            return project;
        }

        try {
            releaseOrphanLock(project, repoId, local.getBranch(), designPath);
            project.close();
            return null;
        } catch (ProjectException e) {
            log.warn("Cannot close the project {}", project.getName(), e);
        }
        return project;
    }

    private void releaseOrphanLock(RulesProject project, String repoId, String branch, String designPath) {
        try {
            var lockInfo = projectsLockEngine.getLockInfo(repoId, branch, designPath);
            if (project.isLockedByMe(lockInfo)) {
                projectsLockEngine.unlock(repoId, branch, designPath);
            }
        } catch (RuntimeException e) {
            log.warn("Cannot release the lock of orphan project '{}' in repository '{}'",
                    project.getName(),
                    repoId,
                    e);
        }
    }

    /**
     * Checks that the project does not exist in the configured main branch of the design repository.
     *
     * <p>The base repository uses its configured main branch. The answer is authoritative only when
     * the repository responds: a missing path or a deletion marker means the project is gone. A
     * repository that cannot respond is unavailable, not empty, so its projects are not reported as
     * missing.
     */
    private boolean isMissingFromMainBranch(Repository designRepository, String designPath) {
        try {
            var fileData = designRepository.check(designPath);
            return fileData == null || fileData.isDeleted();
        } catch (IOException e) {
            log.warn("Cannot check the project '{}' in the main branch of repository '{}' because of error: {}",
                    designPath,
                    designRepository.getId(),
                    e.getMessage(), e);
            return false;
        }
    }

    private String designPath(String projectName, FileData local) {
        var mappingData = local.getAdditionalData(FileMappingData.class);
        if (mappingData != null) {
            return mappingData.getInternalPath();
        }
        var metainfo = localWorkspace.getMetainfoRegistry().get(projectName);
        if (metainfo != null && metainfo.pathInRepository() != null) {
            return metainfo.pathInRepository();
        }
        return designTimeRepository.getRulesLocation() + projectName;
    }

    /**
     * Builds a workspace project from its last known state when it has no design counterpart.
     *
     * <p>A genuine local project stays local. An opened copy linked to a configured design repository
     * keeps the link during a temporary repository outage. The metainfo record is not rewritten.
     */
    private RulesProject createUnmatchedProject(String repoId, LocalRepository repository, FileData local) {
        Repository designRepository = LocalWorkspace.LOCAL_ID.equals(repoId) ? null
                : designTimeRepository.getRepository(repoId);
        FileData designFileData = null;
        if (designRepository != null && local.getVersion() != null) {
            designFileData = new FileData();
            var mappingData = local.getAdditionalData(FileMappingData.class);
            designFileData.setName(mappingData != null ? mappingData.getInternalPath()
                    : designTimeRepository.getRulesLocation() + local.getName());
            designFileData.setVersion(local.getVersion());
            designFileData.setBranch(local.getBranch());
            designFileData.setAuthor(local.getAuthor());
            designFileData.setModifiedAt(local.getModifiedAt());
            designFileData.setSize(local.getSize());
        }
        return new RulesProject(getUser(),
                repository,
                local,
                designFileData == null ? null : designRepository,
                designFileData,
                projectsLockEngine);
    }

    /**
     * Checks if a project's version is exist in history. Version can be absent in history if repository configuration
     * was switched to another path but projects in workspace point to revision in the previous repository.
     */
    private boolean isVersionExistInHistory(RulesProject project) {
        if (project.isLastVersion()) {
            return true;
        }

        var version = project.getVersion();
        var found = false;
        for (ProjectVersion v : project.getVersions()) {
            if (version.equals(v)) {
                found = true;
                break;
            }
        }
        return found;
    }

    @Override
    public void release() {
        localWorkspace.release();
        synchronized (userRulesProjects) {
            clearRulesProjectsCache();
        }

        scheduleProjectsRefresh();

        for (UserWorkspaceListener listener : new ArrayList<>(listeners)) {
            listener.workspaceReleased(this);
        }
    }

    @Override
    public void removeWorkspaceListener(UserWorkspaceListener listener) {
        designTimeRepository.removeListener(designRepoListener);
        listeners.remove(listener);
    }

    @Override
    public RulesProject uploadLocalProject(Repository designRepository,
                                           String name,
                                           String projectFolder,
                                           String comment) throws ProjectException {
        var repositoryId = designRepository.getId();
        try {
            var designPath = designTimeRepository.getRulesLocation() + name;
            var designData = new FileData();
            designData.setName(designPath);

            var createdProject = new AProject(designRepository, designData);
            var project = localWorkspace.getProject(null, name);
            project.refresh();
            if (designRepository.supports().mappedFolders()) {
                var fileData = createdProject.getFileData();
                fileData.addAdditionalData(FileMappingData.forProject(designPath, projectFolder, name));
            }
            createdProject.getFileData().setComment(comment);
            createdProject.update(project, user);
            designData.setName(createdProject.getFolderPath());

            var rulesProject = new RulesProject(getUser(),
                    localWorkspace.getRepository(repositoryId),
                    project.getFileData(),
                    designRepository,
                    designData,
                    projectsLockEngine);
            rulesProject.open();

            refreshRulesProjects();

            return rulesProject;
        } catch (ProjectException e) {
            try {
                if (designTimeRepository.hasProject(repositoryId, name)) {
                    designTimeRepository.getProject(repositoryId, name).delete(user, comment);
                }
            } catch (ProjectException e1) {
                log.error(e1.getMessage(), e1);
            }
            throw e;
        }
    }

    @Override
    public boolean isOpenedOtherProject(AProject project) {
        String name;
        try {
            name = getActualName(project);
        } catch (ProjectException | IOException e) {
            name = project.getBusinessName();
        }
        var actualName = name;
        return getProjects(false).stream()
                .anyMatch(p -> p.isOpened() && actualName.equals(p.getBusinessName()) && (!project.getRepository()
                        .getId()
                        .equals(p.getRepository().getId()) || !project.getRealPath().equals(p.getRealPath())));

    }
}
