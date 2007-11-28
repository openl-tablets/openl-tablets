package org.openl.rules.workspace.uw.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openl.rules.repository.CommonVersion;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.DeploymentDescriptorProject;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectDescriptor;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.deploy.ProductionDeployer;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryDDProject;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.dtr.RepositoryProject;
import org.openl.rules.workspace.lw.LocalProject;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceDeploymentProject;
import org.openl.rules.workspace.uw.UserWorkspaceListener;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.util.Log;

public class UserWorkspaceImpl implements UserWorkspace {
    
    private static final Comparator<UserWorkspaceProject> PROJECTS_COMPARATOR
    = new Comparator<UserWorkspaceProject>(){
        public int compare(UserWorkspaceProject o1, UserWorkspaceProject o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };
    
    private final WorkspaceUser user;
    private final LocalWorkspace localWorkspace;
    private final DesignTimeRepository designTimeRepository;
    private final ProductionDeployer deployer;

    private HashMap<String, UserWorkspaceProject> userProjects;

    private List<UserWorkspaceListener> listeners = new ArrayList<UserWorkspaceListener>();

    public UserWorkspaceImpl(WorkspaceUser user, LocalWorkspace localWorkspace,
            DesignTimeRepository designTimeRepository, ProductionDeployer deployer) {
        this.user = user;
        this.localWorkspace = localWorkspace;
        this.designTimeRepository = designTimeRepository;
        this.deployer = deployer;

        userProjects = new HashMap<String, UserWorkspaceProject>();
    }

    public Collection<UserWorkspaceProject> getProjects() {
        try {
            refresh();
        } catch (ProjectException e) {
            // ignore
            Log.error("Failed to resfresh projects", e);
        }
        
        LinkedList<UserWorkspaceProject> result = new LinkedList<UserWorkspaceProject>(userProjects.values());
        
        Collections.sort(result, PROJECTS_COMPARATOR);
        
        return result;
    }

    public UserWorkspaceProject getProject(String name) throws ProjectException {
        refresh();
        UserWorkspaceProject uwp = userProjects.get(name);

        if (uwp == null) {
            throw new ProjectException("Cannot find project ''{0}''", null, name);
        }

        return uwp;
    }

    public boolean hasProject(String name) {
        if (userProjects.get(name) != null) return true;
        if (localWorkspace.hasProject(name)) return true;
        if (designTimeRepository.hasProject(name)) return true;

        return false;
    }

    public ProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        String projectName = artefactPath.segment(0);
        UserWorkspaceProject uwp = getProject(projectName);

        ArtefactPath pathInProject = artefactPath.withoutFirstSegment();
        return uwp.getArtefactByPath(pathInProject);
    }

    public void activate() throws ProjectException {
        refresh();
    }

    public void passivate() {
        localWorkspace.saveAll();

        userProjects.clear();
    }

    public void release() {
        localWorkspace.release();
        userProjects.clear();

        for (UserWorkspaceListener listener : listeners)
            listener.workspaceReleased(this);
    }

    public void refresh() throws ProjectException {
        localWorkspace.refresh();

        // add new
        for (RepositoryProject rp : designTimeRepository.getProjects()) {
            String name = rp.getName();

            LocalProject lp = null;
            if (localWorkspace.hasProject(name)) {
                try {
                    lp = localWorkspace.getProject(name);
                } catch (ProjectException e) {
                    // ignore
                }
            }

            UserWorkspaceProjectImpl uwp = (UserWorkspaceProjectImpl) userProjects.get(name);
            if (uwp == null) {
                uwp = new UserWorkspaceProjectImpl(this, lp, rp);
                userProjects.put(name, uwp);
            } else if (uwp.isLocalOnly()) {
                uwp.updateArtefact(lp, rp);
            }
        }

        // LocalProjects that hasn't corresponding project in DesignTimeRepository
        for (LocalProject lp : localWorkspace.getProjects()) {
            String name = lp.getName();

            if (!designTimeRepository.hasProject(name)) {

                UserWorkspaceProjectImpl uwp = (UserWorkspaceProjectImpl) userProjects.get(name);
                if (uwp == null) {
                    uwp = new UserWorkspaceProjectImpl(this, lp, null);
                    userProjects.put(name, uwp);
                } else {
                    uwp.updateArtefact(lp, null);
                }
            }
        }

        Iterator<Map.Entry<String,UserWorkspaceProject>> entryIterator = userProjects.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<String, UserWorkspaceProject> entry = entryIterator.next();
            if (!designTimeRepository.hasProject(entry.getKey()) && !localWorkspace.hasProject(entry.getKey())) {
                entryIterator.remove();
            }
        }
    }

    public File getLocalWorkspaceLocation() {
        return localWorkspace.getLocation();
    }

    public void deploy(DeploymentDescriptorProject deployProject) throws DeploymentException, RepositoryException {
        Collection<ProjectDescriptor> projectDescriptors = deployProject.getProjectDescriptors();
        Collection<Project> projects = new ArrayList<Project>();
        for (ProjectDescriptor descriptor : projectDescriptors) {
            projects.add(designTimeRepository.getProject(descriptor.getProjectName(), descriptor.getProjectVersion()));
        }

        deployer.deploy(projects);
    }

    public void createProject(String name) throws ProjectException {
        designTimeRepository.createProject(name);
        
        refresh();
    }

    // --- protected

    protected LocalProject openLocalProjectFor(RepositoryProject repositoryProject) throws ProjectException {
        return localWorkspace.addProject(repositoryProject);
    }

    protected LocalProject openLocalProjectFor(RepositoryProject repositoryProject, CommonVersion version) throws ProjectException {
        RepositoryProject oldRP = designTimeRepository.getProject(repositoryProject.getName(), version);
        return localWorkspace.addProject(oldRP);
    }

    protected void checkInProject(LocalProject localProject, int major, int minor) throws RepositoryException {
        designTimeRepository.updateProject(localProject, user, major, minor);
    }

    public WorkspaceUser getUser() {
        return user;
    }
    
    public void createDDProject(String name) throws RepositoryException {
        designTimeRepository.createDDProject(name);
    }

    public void copyProject(UserWorkspaceProject project, String name) throws ProjectException {
        designTimeRepository.copyProject(project, name, user);
        refresh();
    }

    public void addWorkspaceListener(UserWorkspaceListener listener) {
        listeners.add(listener);
    }

    public boolean removeWorkspaceListener(UserWorkspaceListener listener) {
        return listeners.remove(listener);
    }

    public UserWorkspaceDeploymentProject getDDProject(String name) throws RepositoryException {
        RepositoryDDProject ddp = designTimeRepository.getDDProject(name);
        return new UserWorkspaceDeploymentProjectImpl(this, ddp);
    }

    public List<UserWorkspaceDeploymentProject> getDDProjects() throws RepositoryException {
        LinkedList<UserWorkspaceDeploymentProject> result = new LinkedList<UserWorkspaceDeploymentProject>();
        
        for (RepositoryDDProject ddp : designTimeRepository.getDDProjects()) {
            result.add(new UserWorkspaceDeploymentProjectImpl(this, ddp));
        }
        
        Collections.sort(result, PROJECTS_COMPARATOR);
        
        return result;
    }
}
