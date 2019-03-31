package org.openl.rules.workspace.uw.impl;

import java.io.IOException;
import java.util.*;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.*;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceListener;
import org.openl.util.RuntimeExceptionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserWorkspaceImpl implements UserWorkspace {
    private final Logger log = LoggerFactory.getLogger(UserWorkspaceImpl.class);

    private static final Comparator<AProject> PROJECTS_COMPARATOR = new Comparator<AProject>() {
        @Override
        public int compare(AProject o1, AProject o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    };

    private final WorkspaceUser user;
    private final LocalWorkspace localWorkspace;
    private final DesignTimeRepository designTimeRepository;

    private final HashMap<String, RulesProject> userRulesProjects;
    private final HashMap<String, ADeploymentProject> userDProjects;

    private boolean projectsRefreshNeeded = true;
    private boolean deploymentsRefreshNeeded = true;

    private final List<UserWorkspaceListener> listeners = new ArrayList<>();
    private final LockEngine projectsLockEngine;
    private final LockEngine deploymentsLockEngine;

    public UserWorkspaceImpl(WorkspaceUser user,
            LocalWorkspace localWorkspace,
            DesignTimeRepository designTimeRepository,
            LockEngine projectsLockEngine,
            LockEngine deploymentsLockEngine) {
        this.user = user;
        this.localWorkspace = localWorkspace;
        this.designTimeRepository = designTimeRepository;
        this.projectsLockEngine = projectsLockEngine;
        this.deploymentsLockEngine = deploymentsLockEngine;

        userRulesProjects = new HashMap<>();
        userDProjects = new HashMap<>();
    }

    @Override
    public void activate() {
        refresh();
    }

    @Override
    public void addWorkspaceListener(UserWorkspaceListener listener) {
        listeners.add(listener);
    }

    @Override
    public void copyDDProject(ADeploymentProject project, String name) throws ProjectException {
        ADeploymentProject newProject = designTimeRepository.createDeploymentConfigurationBuilder(name)
            .user(getUser())
            .lockEngine(deploymentsLockEngine)
            .build();
        newProject.getFileData().setComment(Comments.copiedFrom(project.getName()));
        newProject.update(project, user);

        refresh();
    }

    @Override
    public ADeploymentProject createDDProject(String name) throws RepositoryException {
        if (deploymentsRefreshNeeded) {
            refreshDeploymentProjects();
        }
        ADeploymentProject ddProject = designTimeRepository.createDeploymentConfigurationBuilder(name)
            .user(getUser())
            .lockEngine(deploymentsLockEngine)
            .build();
        userDProjects.put(name, ddProject);
        return ddProject;
    }

    @Override
    public AProject createProject(String name) throws ProjectException {
        return designTimeRepository.createProject(name);
    }

    @Override
    public AProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        String projectName = artefactPath.segment(0);
        AProject uwp = getProject(projectName);

        ArtefactPath pathInProject = artefactPath.withoutFirstSegment();
        return uwp.getArtefactByPath(pathInProject);
    }

    @Override
    public ADeploymentProject getDDProject(String name) throws ProjectException {
        refreshDeploymentProjects();
        ADeploymentProject deploymentProject;
        synchronized (userDProjects) {
            deploymentProject = userDProjects.get(name);
        }
        if (deploymentProject == null) {
            throw new ProjectException("Cannot find deployment project ''{0}''", null, name);
        }
        return deploymentProject;
    }

    @Override
    public ADeploymentProject getLatestDeploymentConfiguration(String name) {
        return getDesignTimeRepository().createDeploymentConfigurationBuilder(name)
            .user(getUser())
            .lockEngine(deploymentsLockEngine)
            .build();
    }

    @Override
    public List<ADeploymentProject> getDDProjects() throws ProjectException {
        refreshDeploymentProjects();

        ArrayList<ADeploymentProject> result;
        synchronized (userDProjects) {
            result = new ArrayList<>(userDProjects.values());
        }
        Collections.sort(result, PROJECTS_COMPARATOR);

        return result;
    }

    @Override
    public DesignTimeRepository getDesignTimeRepository() {
        return designTimeRepository;
    }

    // --- protected

    @Override
    public LocalWorkspace getLocalWorkspace() {
        return localWorkspace;
    }

    @Override
    public RulesProject getProject(String name) throws ProjectException {
        return getProject(name, true);
    }

    @Override
    public RulesProject getProject(String name, boolean refreshBefore) throws ProjectException {
        if (refreshBefore || projectsRefreshNeeded) {
            refreshRulesProjects();
        }

        RulesProject uwp;
        synchronized (userRulesProjects) {
            uwp = userRulesProjects.get(name);
        }

        if (uwp == null) {
            throw new ProjectException("Cannot find project ''{0}''", null, name);
        }

        return uwp;
    }

    @Override
    public Collection<RulesProject> getProjects() {
        return getProjects(true);
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

        Collections.sort(result, PROJECTS_COMPARATOR);

        return result;
    }

    @Override
    public WorkspaceUser getUser() {
        return user;
    }

    @Override
    public boolean hasDDProject(String name) {
        if (deploymentsRefreshNeeded) {
            try {
                refreshDeploymentProjects();
            } catch (RepositoryException e) {
                // FIXME Don't wrap checked exception
                throw RuntimeExceptionWrapper.wrap(e);
            }
        }
        synchronized (userDProjects) {
            if (userDProjects.get(name) != null) {
                return true;
            }
        }
        return designTimeRepository.hasDDProject(name);
    }

    @Override
    public boolean hasProject(String name) {
        synchronized (userRulesProjects) {
            if (projectsRefreshNeeded) {
                refreshRulesProjects();
            }
            if (userRulesProjects.get(name) != null) {
                return true;
            }
        }
        return localWorkspace.hasProject(name) || designTimeRepository.hasProject(name);
    }

    @Override
    public void passivate() {
        synchronized (userRulesProjects) {
            userRulesProjects.clear();
        }

        synchronized (userDProjects) {
            userDProjects.clear();
        }
        scheduleProjectsRefresh();
        scheduleDeploymentsRefresh();
    }

    @Override
    public void refresh() {
        localWorkspace.refresh();
        scheduleProjectsRefresh();
        scheduleDeploymentsRefresh();
    }

    private void scheduleDeploymentsRefresh() {
        synchronized (userDProjects) {
            deploymentsRefreshNeeded = true;
        }
    }

    private void refreshDeploymentProjects() throws RepositoryException {
        List<ADeploymentProject> dtrProjects = designTimeRepository.getDDProjects();

        synchronized (userDProjects) {
            // add new
            HashMap<String, ADeploymentProject> dtrProjectsMap = new HashMap<>();
            for (ADeploymentProject ddp : dtrProjects) {
                String name = ddp.getName();
                dtrProjectsMap.put(name, ddp);

                ADeploymentProject userDProject = userDProjects.get(name);

                if (userDProject == null || ddp.isDeleted() != userDProject.isDeleted()) {
                    userDProject = new ADeploymentProject(user,
                        ddp.getRepository(),
                        ddp.getFileData(),
                        deploymentsLockEngine);
                    if (!userDProject.isOpened()) {
                        // Closed project can't be locked.
                        // DeployConfiguration changes aren't persisted. If it closed, it means changes are lost. We can
                        // safely unlock it
                        if (userDProject.isLockedByMe()) {
                            userDProject.unlock();
                        }
                    }

                    userDProjects.put(name, userDProject);
                } else {
                    userDProject.refresh();
                }
            }

            // remove deleted
            Iterator<ADeploymentProject> i = userDProjects.values().iterator();
            while (i.hasNext()) {
                ADeploymentProject userDProject = i.next();
                String name = userDProject.getName();

                if (!dtrProjectsMap.containsKey(name)) {
                    i.remove();
                }
            }

            deploymentsRefreshNeeded = false;
        }
    }

    private void scheduleProjectsRefresh() {
        synchronized (userRulesProjects) {
            projectsRefreshNeeded = true;
        }
    }

    private void refreshRulesProjects() {
        localWorkspace.refresh();

        synchronized (userRulesProjects) {

            userRulesProjects.clear();

            // add new
            final Repository designRepository = designTimeRepository.getRepository();
            LocalRepository localRepository = localWorkspace.getRepository();
            for (AProject rp : designTimeRepository.getProjects()) {
                String name = rp.getName();

                AProject lp = null;
                if (localWorkspace.hasProject(name)) {
                    try {
                        lp = localWorkspace.getProject(name);
                    } catch (ProjectException e) {
                        // ignore
                        log.error("refreshRulesProjects", e);
                    }
                }

                FileData local = lp == null ? null : lp.getFileData();

                Repository desRepo = designRepository;
                FileData designFileData = rp.getFileData();

                try {
                    if (designRepository.supports().branches() && local != null) {
                        BranchRepository branchRepository = (BranchRepository) designRepository;
                        String repoBranch = branchRepository.getBranch();
                        String branch = local.getBranch();
                        if (branch == null) {
                            log.warn("Unknown branch in repository supporting branches");
                        } else if (!branch.equals(repoBranch)) {
                            // We are inside alternative branch. Must change design repo info.
                            desRepo = branchRepository.forBranch(branch);
                            // Other branch - other version of file data
                            if (designFileData != null) {
                                designFileData = desRepo.check(designFileData.getName());
                            }
                        }
                    }
                } catch (IOException e) {
                    log.error("Skip workspace changes for project '{}' because of error: {}",
                        rp.getName(),
                        e.getMessage(),
                        e);
                    desRepo = designRepository;
                    designFileData = rp.getFileData();
                    local = null;
                }

                userRulesProjects.put(name,
                    new RulesProject(this, localRepository, local, desRepo, designFileData, projectsLockEngine));
            }

            // LocalProjects that hasn't corresponding project in
            // DesignTimeRepository
            for (AProject lp : localWorkspace.getProjects()) {
                String name = lp.getName();

                if (!designTimeRepository.hasProject(name)) {
                    FileData local = lp.getFileData();
                    userRulesProjects.put(name,
                        new RulesProject(this, localRepository, local, designRepository, null, projectsLockEngine));
                }
            }

            Iterator<Map.Entry<String, RulesProject>> entryIterator = userRulesProjects.entrySet().iterator();
            while (entryIterator.hasNext()) {
                Map.Entry<String, RulesProject> entry = entryIterator.next();
                if (!designTimeRepository.hasProject(entry.getKey()) && !localWorkspace.hasProject(entry.getKey())) {
                    entryIterator.remove();
                }
            }

            projectsRefreshNeeded = false;
        }
    }

    @Override
    public void release() {
        localWorkspace.release();
        synchronized (userRulesProjects) {
            userRulesProjects.clear();
        }

        synchronized (userDProjects) {
            userDProjects.clear();
        }
        scheduleProjectsRefresh();
        scheduleDeploymentsRefresh();

        for (UserWorkspaceListener listener : new ArrayList<>(listeners)) {
            listener.workspaceReleased(this);
        }
    }

    @Override
    public void removeWorkspaceListener(UserWorkspaceListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void uploadLocalProject(String name) throws ProjectException {
        try {
            AProject createdProject = createProject(name);
            AProject project = localWorkspace.getProject(name);
            project.refresh();
            createdProject.update(project, user);
            refreshRulesProjects();
        } catch (ProjectException e) {
            try {
                if (designTimeRepository.hasProject(name)) {
                    designTimeRepository.getProject(name).erase(user);
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
}
