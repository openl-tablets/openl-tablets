package org.openl.rules.uw.impl;

import org.openl.rules.WorkspaceUser;
import org.openl.rules.commons.artefacts.ArtefactPath;
import org.openl.rules.commons.projects.ProjectArtefact;
import org.openl.rules.commons.projects.ProjectException;
import org.openl.rules.commons.projects.ProjectVersion;
import org.openl.rules.dtr.DesignTimeRepository;
import org.openl.rules.dtr.RepositoryProject;
import org.openl.rules.dtr.RepositoryException;
import org.openl.rules.lw.LocalProject;
import org.openl.rules.lw.LocalWorkspace;
import org.openl.rules.uw.UserWorkspace;
import org.openl.rules.uw.UserWorkspaceProject;

import java.util.Collection;
import java.util.HashMap;

public class UserWorkspaceImpl implements UserWorkspace {
    private WorkspaceUser user;
    private LocalWorkspace localWorkspace;
    private DesignTimeRepository designTimeRepository;

    private HashMap<String, UserWorkspaceProject> userProjects;

    public UserWorkspaceImpl(WorkspaceUser user, LocalWorkspace localWorkspace, DesignTimeRepository designTimeRepository) {
        this.user = user;
        this.localWorkspace = localWorkspace;
        this.designTimeRepository = designTimeRepository;

        userProjects = new HashMap<String, UserWorkspaceProject>();
    }

    public Collection<UserWorkspaceProject> getProjects() {
        return userProjects.values();
    }

    public UserWorkspaceProject getProject(String name) throws ProjectException {
        UserWorkspaceProject uwp = userProjects.get(name);
        if (uwp == null) {
            throw new ProjectException("Cannot find project ''{0}''", name);
        }

        return uwp;
    }

    public boolean hasProject(String name) {
        return (userProjects.get(name) != null);
    }

    public ProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        String projectName = artefactPath.segment(0);
        UserWorkspaceProject uwp = getProject(projectName);

        ArtefactPath pathInProject = artefactPath.getRelativePath(1);
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

            UserWorkspaceProject uwp = userProjects.get(name);
            if (uwp == null) {
                uwp = new UserWorkspaceProjectImpl(this, lp, rp);
                userProjects.put(name, uwp);
            }
        }

        // LocalProjects that hasn't corresponding project in DesignTimeRepository
        for (LocalProject lp : localWorkspace.getProjects()) {
            String name = lp.getName();

            if (!designTimeRepository.hasProject(name)) {

                UserWorkspaceProject uwp = userProjects.get(name);
                if (uwp == null) {
                    uwp = new UserWorkspaceProjectImpl(this, lp, null);
                    userProjects.put(name, uwp);
                }
            }
        }
    }

    // --- protected

    protected LocalProject openLocalProjectFor(RepositoryProject repositoryProject) throws ProjectException {
        return localWorkspace.addProject(repositoryProject);
    }

    protected LocalProject openLocalProjectFor(RepositoryProject repositoryProject, ProjectVersion version) throws ProjectException {
        RepositoryProject oldRP = designTimeRepository.getProjectVersion(repositoryProject.getName(), version);
        return localWorkspace.addProject(oldRP);
    }

    protected void checkInProject(LocalProject localProject) throws RepositoryException {
        designTimeRepository.updateProject(localProject, user);
    }

    protected WorkspaceUser getUser() {
        return user;
    }
}
