package org.openl.rules.workspace.dtr.impl;

import org.openl.rules.repository.RProject;
import org.openl.rules.repository.RRepository;
import org.openl.rules.repository.RulesRepositoryFactory;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.abstracts.impl.ArtefactPathImpl;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.dtr.RepositoryProject;
import org.openl.util.Log;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class DesignTimeRepositoryImpl implements DesignTimeRepository {
    private RRepository rulesRepository;

    public DesignTimeRepositoryImpl() throws RepositoryException {
        try {
            rulesRepository = RulesRepositoryFactory.getRepositoryInstance();
        } catch (RRepositoryException e) {
            throw new RepositoryException("Cannot get Repository", e);
        }        
    }

    public Collection<RepositoryProject> getProjects() {
        List<RepositoryProject> result = new LinkedList<RepositoryProject>();
        
        try {
            for (RProject rp : rulesRepository.getProjects()) {
                RepositoryProject project = wrapProject(rp);
                result.add(project);
            }
        } catch (RRepositoryException e) {
            // TODO: re throw exception ?
            Log.error("Cannot list projects", e);
        }        
        return result;
    }

    public RepositoryProject getProject(String name) throws RepositoryException {
        try {
            for (RProject rp : rulesRepository.getProjects()) {
                String s = rp.getName();
                if (name.equals(s)) {
                    return wrapProject(rp);
                }
            }
        } catch (RRepositoryException e) {
            throw new RepositoryException("Cannot find project ''{0}''", e, name);
        }
        
        throw new RepositoryException("Cannot find project ''{0}''", null, name);
    }

    public boolean hasProject(String name) {
        try {
            for (RProject rp : rulesRepository.getProjects()) {
                String s = rp.getName();
                if (name.equals(s)) {
                    return true;
                }
            }
        } catch (RRepositoryException e) {
            Log.error("Failed to check whether project ''{0}'' exists", e, name);
        }
        
        return false;
    }

    public ProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        String projectName = artefactPath.segment(0);
        RepositoryProject rp = getProject(projectName);

        ArtefactPath pathInProject = artefactPath.getRelativePath(1);
        return rp.getArtefactByPath(pathInProject);
    }

    public RepositoryProject getProjectVersion(String name, ProjectVersion version) throws RepositoryException {
        return getProject(name);
    }

    public void updateProject(Project project, WorkspaceUser user) throws RepositoryException {
    }

    public void copyProject(Project project, String name) throws RepositoryException {
    }
    
    public void createProject(String name) throws RepositoryException {
        try {
            rulesRepository.createProject(name);
        } catch (RRepositoryException e) {
            throw new RepositoryException("Failed to create project ''{0}''", e, name);
        }        
    }

    // --- private
    
    private RepositoryProjectImpl wrapProject(RProject rp) {
        ArtefactPath ap = new ArtefactPathImpl(new String[]{rp.getName()});
        // FIXME
        RepositoryVersionInfoImpl info = new RepositoryVersionInfoImpl(rp.getBaseVersion().getCreated(), "user");
        RepositoryProjectVersionImpl version = new RepositoryProjectVersionImpl(0, 0, 0, info);
        
        return new RepositoryProjectImpl(rp, ap, version);
    }
}
