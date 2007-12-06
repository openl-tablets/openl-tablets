package org.openl.rules.workspace.dtr.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.RDeploymentDescriptorProject;
import org.openl.rules.repository.RProject;
import org.openl.rules.repository.RRepository;
import org.openl.rules.repository.RulesRepositoryFactory;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.DeploymentDescriptorProject;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.impl.ArtefactPathImpl;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryDDProject;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.dtr.RepositoryProject;
import org.openl.util.Log;

public class DesignTimeRepositoryImpl implements DesignTimeRepository {
    /** Rules Repository */
    private RRepository rulesRepository;
    /** Project Cache */
    private HashMap<String, RepositoryProjectImpl> projects;

    public DesignTimeRepositoryImpl() throws RepositoryException {
        try {
            rulesRepository = RulesRepositoryFactory.getRepositoryInstance();
        } catch (RRepositoryException e) {
            throw new RepositoryException("Cannot get Repository", e);
        }
        
        projects = new HashMap<String, RepositoryProjectImpl>();
    }

    public Collection<RepositoryProject> getProjects() {
        List<RepositoryProject> result = new LinkedList<RepositoryProject>();

        try {
            for (RProject rp : rulesRepository.getProjects()) {
                String name = rp.getName();
                RepositoryProject cached = projects.get(name);
                
                if (cached != null) {
                    // use cached
                    result.add(cached);
                } else {
                    // get from the repository
                    RepositoryProject project = wrapProject(rp, true);
                    result.add(project);
                }
            }
        } catch (RRepositoryException e) {
            // TODO: re throw exception ?
            Log.error("Cannot list projects", e);
        }        
        return result;
    }

    public RepositoryProject getProject(String name) throws RepositoryException {
        if (!hasProject(name)) {
            throw new RepositoryException("Cannot find project ''{0}''", null, name);
        }
        
        RepositoryProject cached = projects.get(name);
        if (cached != null) {
            return cached;
        }
            
        try {
            RProject rp = rulesRepository.getProject(name);
            return wrapProject(rp, true);
        } catch (RRepositoryException e) {
            throw new RepositoryException("Cannot find project ''{0}''", e, name);
        }
    }

    public boolean hasProject(String name) {
        RepositoryProject cached = projects.get(name);
        boolean inCache = (cached != null);
        
        try {
            boolean repHas = rulesRepository.hasProject(name);
            if (repHas != inCache) {
                if (!repHas) {
                    // ???
                    projects.remove(name);
                }
            }
            return repHas;
        } catch (RRepositoryException e) {
            Log.error("Failed to check project ''{0}'' in the repository", e, name);
        }
        
        return inCache;
    }

    public boolean hasDDProject(String name) {
	try {
	    return rulesRepository.hasDDProject(name);
        } catch (RRepositoryException e) {
            Log.error("Failed to check deployment project ''{0}'' in the repository", e, name);
            return false;
        }
    }

    public ProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        String projectName = artefactPath.segment(0);
        RepositoryProject rp = getProject(projectName);

        ArtefactPath pathInProject = artefactPath.withoutFirstSegment();
        return rp.getArtefactByPath(pathInProject);
    }

    public RepositoryProject getProject(String name, CommonVersion version) throws RepositoryException {
        try {
            RProject rp = rulesRepository.getProject(name);
            RProject oldProject = rp.getProjectVersion(version);
            return wrapProject(oldProject, false);
        } catch (RRepositoryException e) {
            throw new RepositoryException("Cannot find project ''{0}'' or its version ''{1}''", e, name, version.getVersionName());
        }        
    }

    public void updateProject(Project project, WorkspaceUser user, int major, int minor) throws RepositoryException {
        String name = project.getName();
        RepositoryProject dest = getProject(name);
        
        if (!dest.isLocked()) {
            throw new RepositoryException("Cannot update project ''{0}'' while it is not locked!", null, name);
        }
        
        WorkspaceUser lockedBy = dest.getlLockInfo().getLockedBy();
        if (!lockedBy.equals(user)) {
            throw new RepositoryException("Project ''{0}'' is locked by other user ({0})!", null, name, lockedBy.getUserName());
        }

        try {
            if (major != 0 || minor != 0) {
                dest.riseVersion(major, minor);
            }
            
            dest.commit(project, user);
        } catch (ProjectException e) {
            throw new RepositoryException("Failed to update project ''{0}''", e, name);
        }        
    }

    public void copyProject(Project project, String name, WorkspaceUser user) throws ProjectException {
        createProject(name);
        RepositoryProject newProject = getProject(name);
        newProject.commit(project, user);
    }
    
    public void createProject(String name) throws RepositoryException {
        try {
            rulesRepository.createProject(name);
        } catch (RRepositoryException e) {
            throw new RepositoryException("Failed to create project ''{0}''", e, name);
        }        
    }

    public void createDDProject(String name) throws RepositoryException {
        try {
            rulesRepository.createDDProject(name);
        } catch (RRepositoryException e) {
            throw new RepositoryException("Failed to create deployment project ''{0}''", e, name);
        }        
    }
    
    public void copyDDProject(DeploymentDescriptorProject project, String name, WorkspaceUser user) throws ProjectException {
	createDDProject(name);
	RepositoryDDProject newProject = getDDProject(name);
	newProject.commit((Project)project, user);
    }

    public RepositoryDDProject getDDProject(String name) throws RepositoryException {
        try {
            return wrapDDProject(rulesRepository.getDDProject(name));
        } catch (RRepositoryException e) {
            throw new RepositoryException("Cannot find project ''{0}''", e, name);
        }        
    }

    public RepositoryDDProject getDDProject(String name, CommonVersion version) throws RepositoryException {
        try {
            RDeploymentDescriptorProject p = rulesRepository.getDDProject(name);
            RDeploymentDescriptorProject oldProject = p.getProjectVersion(version);
            return wrapDDProject(oldProject);
        } catch (RRepositoryException e) {
            throw new RepositoryException("Cannot find project ''{0}'' or its version ''{1}''", e, name, version.getVersionName());
        }        
    }

    public List<RepositoryDDProject> getDDProjects() throws RepositoryException {
        LinkedList<RepositoryDDProject> result = new LinkedList<RepositoryDDProject>();
        
        try {
            for (RDeploymentDescriptorProject rddp : rulesRepository.getDDProjects()) {
                RepositoryDDProject ddp = wrapDDProject(rddp);
                result.add(ddp);
            }
        } catch (RRepositoryException e) {
            // TODO: re throw exception ?
            Log.error("Cannot list deployments projects", e);
        }        
        return result;
    }

    // --- private
    
    private RepositoryProjectImpl wrapProject(RProject rp, boolean cacheIt) {
        String name = rp.getName();
        ArtefactPath ap = new ArtefactPathImpl(new String[]{name});
        
        RepositoryProjectImpl p = new RepositoryProjectImpl(rp, ap);
        if (cacheIt) {
            projects.put(name, p);
        }

        return p;
    }
    
    private RepositoryDDProject wrapDDProject(RDeploymentDescriptorProject rddp) {
        return new RepositoryDeploymentDescriptorProjectImpl(rddp);
    }
}
