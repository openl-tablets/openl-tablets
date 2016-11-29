package org.openl.rules.workspace.uw.impl;

import java.util.*;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.*;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserWorkspaceImpl implements UserWorkspace {
    private final Logger log = LoggerFactory.getLogger(UserWorkspaceImpl.class);

    private static final Comparator<AProject> PROJECTS_COMPARATOR = new Comparator<AProject>() {
        public int compare(AProject o1, AProject o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    };

    private final WorkspaceUser user;
    private final LocalWorkspace localWorkspace;
    private final DesignTimeRepository designTimeRepository;

    private final HashMap<String, RulesProject> userRulesProjects;
    private final HashMap<String, ADeploymentProject> userDProjects;

    private final List<UserWorkspaceListener> listeners = new ArrayList<UserWorkspaceListener>();

    public UserWorkspaceImpl(WorkspaceUser user, LocalWorkspace localWorkspace,
                             DesignTimeRepository designTimeRepository) {
        this.user = user;
        this.localWorkspace = localWorkspace;
        this.designTimeRepository = designTimeRepository;

        userRulesProjects = new HashMap<String, RulesProject>();
        userDProjects = new HashMap<String, ADeploymentProject>();

        localWorkspace.setUserWorkspace(this);
    }

    public void activate() throws ProjectException {
        refresh();
    }

    public void addWorkspaceListener(UserWorkspaceListener listener) {
        listeners.add(listener);
    }

    public void copyDDProject(ADeploymentProject project, String name) throws ProjectException {
        designTimeRepository.copyDDProject(project, name, user);
        refresh();
    }

    public void copyProject(AProject project, String name, ResourceTransformer resourceTransformer) throws ProjectException {
        try {
            designTimeRepository.copyProject(project, name, user, resourceTransformer);
        } catch (ProjectException e) {
            try {
                if (designTimeRepository.hasProject(name)) {
                    designTimeRepository.getProject(name).erase();
                }
            } catch (ProjectException e1) {
                log.error(e1.getMessage(), e1);
            }
            throw e;
        } finally {
            refresh();
        }
    }

    public ADeploymentProject createDDProject(String name) throws RepositoryException {
        return designTimeRepository.createDDProject(name);
    }

    public AProject createProject(String name) throws ProjectException {
        return designTimeRepository.createProject(name);
    }

    public AProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        String projectName = artefactPath.segment(0);
        AProject uwp = getProject(projectName);

        ArtefactPath pathInProject = artefactPath.withoutFirstSegment();
        return uwp.getArtefactByPath(pathInProject);
    }

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

    public List<ADeploymentProject> getDDProjects() throws ProjectException {
        refreshDeploymentProjects();

        ArrayList<ADeploymentProject> result;
        synchronized (userDProjects) {
            result = new ArrayList<ADeploymentProject>(userDProjects.values());
        }
        Collections.sort(result, PROJECTS_COMPARATOR);

        return result;
    }

    public DesignTimeRepository getDesignTimeRepository() {
        return designTimeRepository;
    }

    // --- protected

    public LocalWorkspace getLocalWorkspace() {
        return localWorkspace;
    }

    public RulesProject getProject(String name) throws ProjectException {
        return getProject(name, true);
    }

    public RulesProject getProject(String name, boolean refreshBefore) throws ProjectException {
        if (refreshBefore || userRulesProjects.isEmpty()) {
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

    public Collection<RulesProject> getProjects() {
        try {
            refreshRulesProjects();
        } catch (ProjectException e) {
            // ignore
            log.error("Failed to resfresh projects!", e);
        }

        ArrayList<RulesProject> result;
        synchronized (userRulesProjects) {
            result = new ArrayList<RulesProject>(userRulesProjects.values());
        }

        Collections.sort(result, PROJECTS_COMPARATOR);

        return result;
    }

    public WorkspaceUser getUser() {
        return user;
    }

    public boolean hasDDProject(String name) {
        synchronized (userDProjects) {
            if (userDProjects.get(name) != null) {
                return true;
            }
        }
        return designTimeRepository.hasDDProject(name);
    }

    public boolean hasProject(String name) {
        synchronized (userRulesProjects) {
            if (userRulesProjects.get(name) != null) {
                return true;
            }
        }
        return localWorkspace.hasProject(name) || designTimeRepository.hasProject(name);
    }

    public void passivate() {
        localWorkspace.saveAll();

        synchronized (userRulesProjects) {
            userRulesProjects.clear();
        }

        synchronized (userDProjects) {
            userDProjects.clear();
        }
    }

    public void refresh() throws ProjectException {
        refreshRulesProjects();
        refreshDeploymentProjects();
    }

    protected void refreshDeploymentProjects() throws ProjectException {
        List<ADeploymentProject> dtrProjects = designTimeRepository.getDDProjects();

        synchronized (userDProjects) {
            // add new
            HashMap<String, ADeploymentProject> dtrProjectsMap = new HashMap<String, ADeploymentProject>();
            for (ADeploymentProject ddp : dtrProjects) {
                String name = ddp.getName();
                dtrProjectsMap.put(name, ddp);

                ADeploymentProject userDProject = userDProjects.get(name);

                if (userDProject == null) {
                    userDProject = new ADeploymentProject(user, ddp.getRepository(), ddp.getFolderPath(), ddp.getHistoryVersion());
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
        }
    }

    protected void refreshRulesProjects() throws RepositoryException {
        localWorkspace.refresh();

        synchronized (userRulesProjects) {

            userRulesProjects.clear();

            // add new
            Repository designRepository = designTimeRepository.getRepository();
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

                RulesProject uwp = userRulesProjects.get(name);
                if (uwp == null) {
                    // TODO:refactor
                    if (lp == null) {
                        uwp = new RulesProject(this, localRepository, null, designRepository, rp.getFileData());
                    } else {
                        uwp = new RulesProject(this, localRepository, lp.getFileData(), designRepository, rp.getFileData());
                    }
                    userRulesProjects.put(name, uwp);
                } else if ((uwp.isLocalOnly() || uwp.isRepositoryOnly()) && lp != null) {
                    userRulesProjects.put(name, new RulesProject(this, localRepository, lp.getFileData(), designRepository, rp.getFileData()));
                } else {
                    uwp.refresh();
                }
            }

            // LocalProjects that hasn't corresponding project in
            // DesignTimeRepository
            for (AProject lp : localWorkspace.getProjects()) {
                String name = lp.getName();

                if (!designTimeRepository.hasProject(name)) {
                    userRulesProjects.put(name, new RulesProject(this, localRepository, lp.getFileData(), null, null));
                }
            }

            Iterator<Map.Entry<String, RulesProject>> entryIterator = userRulesProjects.entrySet().iterator();
            while (entryIterator.hasNext()) {
                Map.Entry<String, RulesProject> entry = entryIterator.next();
                if (!designTimeRepository.hasProject(entry.getKey()) && !localWorkspace.hasProject(entry.getKey())) {
                    entryIterator.remove();
                }
            }
        }
    }

    public void release() {
        localWorkspace.release();
        synchronized (userRulesProjects) {
            userRulesProjects.clear();
        }

        synchronized (userDProjects) {
            userDProjects.clear();
        }

        for (UserWorkspaceListener listener : listeners) {
            listener.workspaceReleased(this);
        }
    }

    public boolean removeWorkspaceListener(UserWorkspaceListener listener) {
        return listeners.remove(listener);
    }

    public void uploadLocalProject(String name) throws ProjectException {
        try {
            AProject createdProject = createProject(name);
            createdProject.update(localWorkspace.getProject(name), user);
            refreshRulesProjects();
        } catch (ProjectException e) {
            try {
                if (designTimeRepository.hasProject(name)) {
                    designTimeRepository.getProject(name).erase();
                }
            } catch (ProjectException e1) {
                log.error(e1.getMessage(), e1);
            }
            throw e;
        }
    }
}
