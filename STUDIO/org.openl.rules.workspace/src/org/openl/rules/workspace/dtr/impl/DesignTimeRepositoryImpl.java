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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

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
                    RepositoryProject project = wrapProject(rp);
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
            return wrapProject(rp);
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
        String name = project.getName();
        RepositoryProject dest = getProject(name);
        
        if (!dest.isLocked()) {
            throw new RepositoryException("Cannot update project ''{0}'' while it is not locked!", null, name);
        }
        
        String lockedBy = dest.getlLockInfo().getLockedBy();
        if (!lockedBy.equals(user.getUserId())) {
            throw new RepositoryException("Project ''{0}'' is locked by other user ({0})!", null, name, lockedBy);
        }

        try {
            dest.update(project);
        } catch (ProjectException e) {
            throw new RepositoryException("Failed to update project ''{0}''", e, name);
        }        
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
        String name = rp.getName();
        ArtefactPath ap = new ArtefactPathImpl(new String[]{name});
        // FIXME
        RepositoryVersionInfoImpl info = new RepositoryVersionInfoImpl(rp.getBaseVersion().getCreated(), "user");
        RepositoryProjectVersionImpl version = new RepositoryProjectVersionImpl(0, 0, 0, info);
        
        RepositoryProjectImpl p = new RepositoryProjectImpl(rp, ap, version);
        projects.put(name, p);
        return p;
    }
}
