package org.openl.rules.workspace.uw.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalFolderAPI;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.deploy.ProductionDeployer;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceListener;

public class UserWorkspaceImpl implements UserWorkspace {
    private static final Log log = LogFactory.getLog(UserWorkspaceImpl.class);

    private static final Comparator<AProject> PROJECTS_COMPARATOR = new Comparator<AProject>() {
        public int compare(AProject o1, AProject o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

    private final WorkspaceUser user;
    private final LocalWorkspace localWorkspace;
    private final DesignTimeRepository designTimeRepository;
    private final ProductionDeployer deployer;

    private final HashMap<String, RulesProject> userRulesProjects;
    private final HashMap<String, ADeploymentProject> userDProjects;

    private final List<UserWorkspaceListener> listeners = new ArrayList<UserWorkspaceListener>();

    public UserWorkspaceImpl(WorkspaceUser user, LocalWorkspace localWorkspace,
            DesignTimeRepository designTimeRepository, ProductionDeployer deployer) {
        this.user = user;
        this.localWorkspace = localWorkspace;
        this.designTimeRepository = designTimeRepository;
        this.deployer = deployer;

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

    public void copyProject(AProject project, String name) throws ProjectException {
        try {
            designTimeRepository.copyProject(project, name, user);
        } catch (ProjectException e) {
            if (designTimeRepository.hasProject(name)) {
                designTimeRepository.getProject(name).erase(user);
            }
            throw e;
        } finally {
            refresh();
        }
    }

    public void createDDProject(String name) throws RepositoryException {
        designTimeRepository.createDDProject(name);
    }

    public void createProject(String name) throws ProjectException {
        designTimeRepository.createProject(name);

        refresh();
    }

    public DeployID deploy(ADeploymentProject deploymentProject) throws DeploymentException, RepositoryException {
        Collection<ProjectDescriptor> projectDescriptors = deploymentProject.getProjectDescriptors();
        Collection<AProject> projects = new ArrayList<AProject>();
        for (ProjectDescriptor descriptor : projectDescriptors) {
            projects.add(designTimeRepository.getProject(descriptor.getProjectName(), descriptor.getProjectVersion()));
        }

        // TODO DeployID id = RepositoryUtils.getDeployID(project);
        DeployID id = new DeployID(deploymentProject.getName() + "#" + deploymentProject.getVersion().getVersionName());
        deployer.deploy(deploymentProject, id, projects);
        return id;
    }

    public AProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        String projectName = artefactPath.segment(0);
        AProject uwp = getProject(projectName);

        ArtefactPath pathInProject = artefactPath.withoutFirstSegment();
        return uwp.getArtefactByPath(pathInProject);
    }

    public ADeploymentProject getDDProject(String name) throws ProjectException {
        refreshDeploymentProjects();
        ADeploymentProject deploymentProject = userDProjects.get(name);
        if (deploymentProject == null) {
            throw new ProjectException("Cannot find deployment project ''{0}''", null, name);
        }
        return deploymentProject;
    }

    protected ADeploymentProject getDDProjectFor(ADeploymentProject deploymentProject, CommonVersion version)
            throws ProjectException {
        ADeploymentProject oldDP = designTimeRepository.getDDProject(deploymentProject.getName(), version);
        return oldDP;
    }

    public List<ADeploymentProject> getDDProjects() throws ProjectException {
        refreshDeploymentProjects();

        ArrayList<ADeploymentProject> result = new ArrayList<ADeploymentProject>(userDProjects.values());
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
        refreshRulesProjects();
        RulesProject uwp = userRulesProjects.get(name);

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

        ArrayList<RulesProject> result = new ArrayList<RulesProject>(userRulesProjects.values());

        Collections.sort(result, PROJECTS_COMPARATOR);

        return result;
    }

    public WorkspaceUser getUser() {
        return user;
    }

    public boolean hasDDProject(String name) {
        if (userDProjects.get(name) != null) {
            return true;
        }
        if (designTimeRepository.hasDDProject(name)) {
            return true;
        }

        return false;
    }

    public boolean hasProject(String name) {
        if (userRulesProjects.get(name) != null) {
            return true;
        }
        if (localWorkspace.hasProject(name)) {
            return true;
        }
        if (designTimeRepository.hasProject(name)) {
            return true;
        }

        return false;
    }

    public void passivate() {
        localWorkspace.saveAll();

        userRulesProjects.clear();
    }

    public void refresh() throws ProjectException {
        refreshRulesProjects();
        refreshDeploymentProjects();
    }

    protected void refreshDeploymentProjects() throws ProjectException {
        List<ADeploymentProject> dtrProjects = designTimeRepository.getDDProjects();

        // add new
        HashMap<String, ADeploymentProject> dtrProjectsMap = new HashMap<String, ADeploymentProject>();
        for (ADeploymentProject ddp : dtrProjects) {
            String name = ddp.getName();
            dtrProjectsMap.put(name, ddp);

            ADeploymentProject userDProject = userDProjects.get(name);

            userDProject = new ADeploymentProject(ddp.getAPI(), user);
            userDProjects.put(name, userDProject);
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

    protected void refreshRulesProjects() throws RepositoryException {
        localWorkspace.refresh();

        // add new
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
                    uwp = new RulesProject(null, rp.getAPI(), this);
                } else {
                    uwp = new RulesProject((LocalFolderAPI) lp.getAPI(), rp.getAPI(), this);
                }
                userRulesProjects.put(name, uwp);
            } else if (uwp.isLocalOnly() || (uwp.isRepositoryOnly() && lp!= null)) {
                userRulesProjects.put(name, new RulesProject((LocalFolderAPI) lp.getAPI(), rp.getAPI(), this));
            } else {
                uwp.refresh();
            }
        }

        // LocalProjects that hasn't corresponding project in
        // DesignTimeRepository
        for (AProject lp : localWorkspace.getProjects()) {
            String name = lp.getName();

            if (!designTimeRepository.hasProject(name)) {

                RulesProject uwp = userRulesProjects.get(name);
                if (uwp == null) {
                    uwp = new RulesProject((LocalFolderAPI) lp.getAPI(), null, this);
                    userRulesProjects.put(name, uwp);
                } else {
                    userRulesProjects.put(name, new RulesProject((LocalFolderAPI) lp.getAPI(), null, this));
                }
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

    public void release() {
        localWorkspace.release();
        userRulesProjects.clear();

        for (UserWorkspaceListener listener : listeners) {
            listener.workspaceReleased(this);
        }
    }

    public boolean removeWorkspaceListener(UserWorkspaceListener listener) {
        return listeners.remove(listener);
    }

    public void uploadLocalProject(String name) throws ProjectException {
        try {
            createProject(name);
            designTimeRepository.getProject(name).lock(user);
            designTimeRepository.getProject(name).update(localWorkspace.getProject(name), user, 0, 0);
            AProject createdProject = getProject(name);
            designTimeRepository.getProject(name).unlock(user);
            createdProject.checkOut(user);
        } catch (ProjectException e) {
            if (designTimeRepository.hasProject(name)) {
                designTimeRepository.getProject(name).erase(user);
            }
            throw e;
        }
    }
}
