package org.openl.rules.workspace.uw.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.ProjectState;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.ProjectKey;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceListener;

public class UserWorkspaceImpl implements UserWorkspace {
    private final Logger log = LoggerFactory.getLogger(UserWorkspaceImpl.class);

    private static final Comparator<AProject> PROJECTS_COMPARATOR = Comparator
            .comparing(AProject::getBusinessName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(o -> o.getRepository().getId())
            .thenComparing(AProject::getRealPath, String.CASE_INSENSITIVE_ORDER);

    private final WorkspaceUser user;
    private final LocalWorkspace localWorkspace;
    private final DesignTimeRepository designTimeRepository;

    private final HashMap<ProjectKey, RulesProject> userRulesProjects;

    private volatile boolean projectsRefreshNeeded = true;
    private volatile boolean syncNeeded = true;
    private volatile boolean cleanUpOnActivation = false;

    private final List<UserWorkspaceListener> listeners = new ArrayList<>();
    private final LockEngine projectsLockEngine;
    private final DesignTimeRepositoryListener designRepoListener = this::refresh;

    public UserWorkspaceImpl(WorkspaceUser user,
                             LocalWorkspace localWorkspace,
                             DesignTimeRepository designTimeRepository,
                             LockEngine projectsLockEngine) {
        this.user = user;
        this.localWorkspace = localWorkspace;
        this.designTimeRepository = designTimeRepository;
        this.projectsLockEngine = projectsLockEngine;

        userRulesProjects = new HashMap<>();
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

    @Override
    public DesignTimeRepository getDesignTimeRepository() {
        return designTimeRepository;
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
            uwp = userRulesProjects.get(new ProjectKey(repositoryId, name.toLowerCase()));
        }

        if (uwp == null) {
            throw new ProjectException("Cannot find project ''{0}'' or access to the project is not permitted.",
                    null,
                    name);
        }

        return uwp;
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
                    .filter(entry -> Objects.equals(repositoryId, entry.getKey().getRepositoryId()))
                    .map(Map.Entry::getValue)
                    .sorted(PROJECTS_COMPARATOR)
                    .collect(Collectors.toList());
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
    public Optional<RulesProject> getProjectByPath(String repositoryId, String realPath) {
        return getProjects(false).stream()
                .filter(p -> !p.isLocalOnly() && repositoryId.equals(p.getDesignRepository()
                        .getId()) && ((realPath.equals(p.getRealPath())) || realPath.startsWith(p.getRealPath() + "/")))
                .findFirst();
    }

    @Override
    public WorkspaceUser getUser() {
        return user;
    }

    @Override
    public boolean hasProject(String repositoryId, String name) {
        synchronized (userRulesProjects) {
            if (projectsRefreshNeeded) {
                refreshRulesProjects();
            }
            return userRulesProjects.containsKey(new ProjectKey(repositoryId, name.toLowerCase()));
        }
    }

    @Override
    public void passivate() {
        synchronized (userRulesProjects) {
            userRulesProjects.clear();
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

        boolean anyProjectRenamed = false;
        for (RulesProject rPr : getProjects(false)) {
            Repository repository = designTimeRepository.getRepository(rPr.getRepository().getId());
            if (repository != null && repository.supports().mappedFolders()) {
                if (rPr.isOpened() && !rPr.isLocalOnly()) {
                    try {
                        String realProjectName = getActualName(rPr);
                        if (!rPr.getLocalFolderName().equals(realProjectName)) {
                            // We cannot close and then open a project in workspace, we should rename the folder
                            // in file system directly. Otherwise we will lose unsaved user changes.
                            File repoRoot = localWorkspace.getRepository(rPr.getRepository().getId()).getRoot().toFile();
                            String prevPath = rPr.getFolderPath();
                            int index = prevPath.lastIndexOf('/');
                            String newPath = prevPath.substring(0, index + 1) + realProjectName;
                            boolean renamed = new File(repoRoot, prevPath).renameTo(new File(repoRoot, newPath));
                            if (renamed) {
                                anyProjectRenamed = true;
                            } else {
                                log.warn("Cannot rename folder from {} to {}", prevPath, newPath);
                            }
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
        if (project.hasArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME)) {
            AProjectArtefact artefact = project
                    .getArtefact(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
            if (artefact instanceof AProjectResource) {
                try (InputStream content = ((AProjectResource) artefact).getContent()) {
                    return getActualName(content);
                }
            }
        }
        String actualPath = project.getRealPath();
        return actualPath.substring(actualPath.lastIndexOf('/') + 1);
    }

    private String getActualName(InputStream inputStream) {
        try {
            InputSource inputSource = new InputSource(inputStream);
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            XPathExpression xPathExpression = xPath.compile("/project/name");
            return xPathExpression.evaluate(inputSource);
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    private void scheduleProjectsRefresh() {
        synchronized (userRulesProjects) {
            projectsRefreshNeeded = true;
            designTimeRepository.refresh();
        }
        for (UserWorkspaceListener listener : listeners) {
            listener.workspaceRefreshed();
        }
    }

    private void refreshRulesProjects() {
        localWorkspace.refresh();

        synchronized (userRulesProjects) {

            Map<ProjectKey, String> closedProjectBranches = new HashMap<>();
            for (Map.Entry<ProjectKey, RulesProject> entry : userRulesProjects.entrySet()) {
                ProjectKey projectKey = entry.getKey();
                final Repository designRepository = designTimeRepository.getRepository(projectKey.getRepositoryId());
                if (designRepository != null) {
                    boolean supportsBranches = designRepository.supports().branches();
                    if (supportsBranches) {
                        RulesProject project = entry.getValue();
                        // Deleted projects should be switched to default branch
                        if (!project.isOpened() && !project.isDeleted()) {
                            closedProjectBranches.put(projectKey, project.getBranch());
                        }
                    }
                }
            }

            userRulesProjects.clear();

            // add new
            for (AProject rp : designTimeRepository.getProjects()) {
                String repoId = rp.getRepository().getId();
                LocalRepository localRepository = localWorkspace.getRepository(repoId);
                String name = rp.getName();

                AProject lp = localWorkspace.getProjectForPath(repoId, rp.getRealPath());

                FileData local = lp == null ? null : lp.getFileData();

                Repository desRepo = rp.getRepository();
                FileData designFileData = rp.getFileData();
                boolean closeProject = false;

                try {
                    if (desRepo.supports().branches()) {
                        BranchRepository branchRepository = (BranchRepository) desRepo;
                        String repoBranch = branchRepository.getBranch();
                        String branch;
                        if (local != null) {
                            branch = local.getBranch();
                            if (branch == null) {
                                log.warn("Unknown branch in repository supporting branches for project {}.",
                                        local.getName());
                            }
                        } else {
                            branch = closedProjectBranches.get(new ProjectKey(repoId, name.toLowerCase()));
                        }

                        // If branch is null then keep default branch.
                        if (branch != null && !branch.equals(repoBranch)) {
                            if (branchRepository.branchExists(branch)) {
                                // We are inside alternative branch. Must change design repo info.
                                final BranchRepository repo = branchRepository.forBranch(branch);
                                // Other branch — other version of file data
                                if (designFileData != null) {
                                    final FileData fileData = repo.check(designFileData.getName());
                                    if (fileData != null) {
                                        // Switch branch
                                        desRepo = repo;
                                        designFileData = fileData;
                                    } else {
                                        log.info("Project '{}' does not exist in the branch '{}' anymore",
                                                name,
                                                branch);
                                        if (local != null) {
                                            // We should either close the project or make it local if we don't want to
                                            // lose the changes.
                                            final RulesProject tmp = new RulesProject(getUser(),
                                                    localRepository,
                                                    local,
                                                    repo,
                                                    designFileData,
                                                    projectsLockEngine);
                                            if (tmp.isModified()) {
                                                log.info(
                                                        "Project '{}' is modified. Convert it to local project instead of closing to prevent losing user changes. ",
                                                        tmp.getName());
                                                if (tmp.isLockedByMe()) {
                                                    tmp.unlock();
                                                }
                                                // We will have 2 projects: local project with changes in the branch
                                                // with deleted project and closed project in the main branch.
                                                // 1) Local project with changes in the branch with deleted project
                                                ProjectState state = localRepository
                                                        .getProjectState(lp.getFolderPath());
                                                if (state != null && !LocalWorkspaceImpl.LOCAL_ID
                                                        .equals(state.getRepositoryId())) {
                                                    state.saveFileData(LocalWorkspaceImpl.LOCAL_ID, local);
                                                }
                                                // 2) Closed project in design repository in main branch
                                                local = null;
                                                localRepository = null;
                                            } else {
                                                // Close the project and stay in the main branch.
                                                log.info(
                                                        "Close the project '{}' because it does not exist in the branch '{}'",
                                                        name,
                                                        branch);
                                                closeProject = true;
                                            }
                                        }
                                    }
                                }
                            } else if (local != null) {
                                log.info("Close the project {} because the branch {} was removed", name, branch);
                                closeProject = true;
                            }
                        }
                    }
                } catch (IOException e) {
                    log.warn("Skip workspace changes for project '{}' because of error: {}",
                            rp.getName(),
                            e.getMessage(), e);
                    desRepo = rp.getRepository();
                    designFileData = rp.getFileData();
                    local = null;
                }

                RulesProject project = new RulesProject(getUser(),
                        localRepository,
                        local,
                        desRepo,
                        designFileData,
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
                userRulesProjects.put(new ProjectKey(repoId, project.getName().toLowerCase()), project);
            }

            // LocalProjects that hasn't corresponding project in
            // DesignTimeRepository
            for (AProject lp : localWorkspace.getProjects()) {
                String repoId = lp.getRepository().getId();
                String name = lp.getName();

                if (!userRulesProjects.containsKey(new ProjectKey(repoId, name.toLowerCase()))) {
                    FileData local = lp.getFileData();
                    LocalRepository repository = (LocalRepository) lp.getRepository();

                    // Project can be closed during refresh. See closeProject variable above.
                    try {
                        if (repository.check(local.getName()) == null) {
                            continue;
                        }
                    } catch (IOException e) {
                        log.warn(e.getMessage(), e);
                        continue;
                    }

                    ProjectState state = repository.getProjectState(lp.getFolderPath());
                    if (state != null) {
                        if (!LocalWorkspaceImpl.LOCAL_ID.equals(state.getRepositoryId())) {
                            state.saveFileData(LocalWorkspaceImpl.LOCAL_ID, local);
                        }
                    }

                    RulesProject project = new RulesProject(getUser(),
                            repository,
                            local,
                            null,
                            null,
                            projectsLockEngine);
                    userRulesProjects.put(new ProjectKey(repoId, project.getName().toLowerCase()), project);
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
     * Checks if a project's version is exist in history. Version can be absent in history if repository configuration
     * was switched to another path but projects in workspace point to revision in the previous repository.
     */
    private boolean isVersionExistInHistory(RulesProject project) {
        if (project.isLastVersion()) {
            return true;
        }

        ProjectVersion version = project.getVersion();
        boolean found = false;
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
            userRulesProjects.clear();
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
    public RulesProject uploadLocalProject(String repositoryId,
                                           String name,
                                           String projectFolder,
                                           String comment) throws ProjectException {
        try {
            String designPath = designTimeRepository.getRulesLocation() + name;
            FileData designData = new FileData();
            designData.setName(designPath);

            AProject createdProject = new AProject(designTimeRepository.getRepository(repositoryId), designData);
            AProject project = localWorkspace.getProject(null, name);
            project.refresh();
            if (designTimeRepository.getRepository(repositoryId).supports().mappedFolders()) {
                FileData fileData = createdProject.getFileData();
                fileData.addAdditionalData(new FileMappingData(designPath, projectFolder + name));
            }
            createdProject.getFileData().setComment(comment);
            createdProject.update(project, user);
            designData.setName(createdProject.getFolderPath());

            RulesProject rulesProject = new RulesProject(getUser(),
                    localWorkspace.getRepository(repositoryId),
                    project.getFileData(),
                    designTimeRepository.getRepository(repositoryId),
                    designData,
                    projectsLockEngine);
            rulesProject.open();

            refreshRulesProjects();

            return rulesProject;
        } catch (ProjectException e) {
            try {
                if (designTimeRepository.hasProject(repositoryId, name)) {
                    designTimeRepository.getProject(repositoryId, name).erase(user, comment);
                }
            } catch (ProjectException e1) {
                log.error(e1.getMessage(), e1);
            }
            throw e;
        }
    }

    @Override
    public LockEngine getProjectsLockEngine() {
        return projectsLockEngine;
    }

    @Override
    public boolean isOpenedOtherProject(AProject project) {
        String name;
        try {
            name = getActualName(project);
        } catch (ProjectException | IOException e) {
            name = project.getBusinessName();
        }
        String actualName = name;
        return getProjects(false).stream()
                .anyMatch(p -> p.isOpened() && actualName.equals(p.getBusinessName()) && (!project.getRepository()
                        .getId()
                        .equals(p.getRepository().getId()) || !project.getRealPath().equals(p.getRealPath())));

    }
}
