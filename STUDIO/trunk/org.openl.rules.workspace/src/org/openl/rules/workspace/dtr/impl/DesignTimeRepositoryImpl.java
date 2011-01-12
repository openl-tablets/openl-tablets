package org.openl.rules.workspace.dtr.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.repository.NullRepository;
import org.openl.rules.repository.RRepository;
import org.openl.rules.repository.RRepositoryListener;
import org.openl.rules.repository.RulesRepositoryFactory;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener.DTRepositoryEvent;
import org.openl.util.MsgHelper;

/**
 *
 * @author Aleh Bykhavets
 *
 */
public class DesignTimeRepositoryImpl implements DesignTimeRepository, RRepositoryListener{
    private static final Log log = LogFactory.getLog(DesignTimeRepositoryImpl.class);

    /** Rules Repository */
    private RRepository rulesRepository;
    /** Project Cache */
    private HashMap<String, AProject> projects;
    private CommonUser user;
    
    private List<DesignTimeRepositoryListener> listeners = new ArrayList<DesignTimeRepositoryListener>();
    
    public DesignTimeRepositoryImpl() {
        try {
            rulesRepository = RulesRepositoryFactory.getRepositoryInstance();
        } catch (RRepositoryException e) {
            e.printStackTrace();
            log.error("Cannot init DTR! " + e.getMessage());
            rulesRepository = new NullRepository();
        }
        
        rulesRepository.addRepositoryListener(this);

        projects = new HashMap<String, AProject>();
    }

	public CommonUser getUser() {
		return user;
	}

	public void setUser(CommonUser user) {
		this.user = user;
	}

	public void copyDDProject(ADeploymentProject project, String name, WorkspaceUser user)
            throws ProjectException {
        createDDProject(name);
        ADeploymentProject newProject = getDDProject(name);
        newProject.update(project, user, 0, 0);
        newProject.checkIn(user);
    }

    public void copyProject(AProject project, String name, WorkspaceUser user) throws ProjectException {
        if (hasProject(name)) {
            throw new ProjectException("Project ''{0}'' is already exist in the repository!", null, name);
        }

        RRepository writeRep = null;

        try {
            log.debug("Opening temporary write session...");
            writeRep = RulesRepositoryFactory.getRepositoryInstance();
            log.debug("Wrapping temporary write project...");
            AProject newProject = wrapProject(writeRep.createRulesProject(name), false);

            newProject.update(project, user, 0, 0);
        } catch (RRepositoryException e) {
            throw new RepositoryException("Failed to create project ''{0}''!", e, name);
        } catch (Exception e) {
            throw new RepositoryException("Failed to copy project ''{0}''!", e, name);
        } finally {
            if (writeRep != null) {
                log.debug("Releasing temporary write session...");
                writeRep.release();
            }
            // invalidate cache (rules projects)
            projects.remove(name);
        }
    }

    public void createDDProject(String name) throws RepositoryException {
        try {
            rulesRepository.createDeploymentProject(name);
        } catch (RRepositoryException e) {
            throw new RepositoryException("Failed to create deployment project ''{0}''!", e, name);
        }
    }

    public void createProject(String name) throws RepositoryException {
        try {
            rulesRepository.createRulesProject(name);
        } catch (Exception e) {
            throw new RepositoryException("Failed to create project ''{0}''!", e, name);
        }
    }

    public AProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        String projectName = artefactPath.segment(0);
        AProject ralProject = getProject(projectName);

        ArtefactPath pathInProject = artefactPath.withoutFirstSegment();
        return ralProject.getArtefactByPath(pathInProject);
    }

    public ADeploymentProject getDDProject(String name) throws RepositoryException {
        try {
            return wrapDDProject(rulesRepository.getDeploymentProject(name));
        } catch (RRepositoryException e) {
            throw new RepositoryException("Cannot find project ''{0}''!", e, name);
        }
    }

    public ADeploymentProject getDDProject(String name, CommonVersion version) throws RepositoryException {
        try {
            FolderAPI ralDeploymentProject = rulesRepository.getDeploymentProject(name);
            if(ralDeploymentProject.getVersions().get(ralDeploymentProject.getVersions().size()-1).compareTo(version) == 0){
                return wrapDDProject(ralDeploymentProject);
            }
            return wrapDDProject(ralDeploymentProject.getVersion(version));
        } catch (ProjectException e) {
            throw new RepositoryException("Cannot find project ''{0}'' or its version ''{1}''!", e, name, version
                    .getVersionName());
        }
    }

    public List<ADeploymentProject> getDDProjects() throws RepositoryException {
        LinkedList<ADeploymentProject> result = new LinkedList<ADeploymentProject>();

        try {
            for (FolderAPI ralDeploymentProject : rulesRepository.getDeploymentProjects()) {
                ADeploymentProject dtrDeploymentProject = wrapDDProject(ralDeploymentProject);
                result.add(dtrDeploymentProject);
            }
        } catch (RRepositoryException e) {
            // TODO: re throw exception ?
            log.error("Cannot list deployments projects!", e);
        }
        return result;
    }

    public AProject getProject(String name) throws RepositoryException {
        if (!hasProject(name)) {
            throw new RepositoryException("Cannot find project ''{0}''!", null, name);
        }

        AProject cached = projects.get(name);
        if (cached != null) {
            return cached;
        }

        try {
            FolderAPI ralProject = rulesRepository.getRulesProject(name);
            return wrapProject(ralProject, true);
        } catch (RRepositoryException e) {
            throw new RepositoryException("Cannot find project ''{0}''!", e, name);
        }
    }

    public AProject getProject(String name, CommonVersion version) throws RepositoryException {
        try {
            FolderAPI ralProject = rulesRepository.getRulesProject(name);
            if (ralProject.getVersions().get(ralProject.getVersions().size() - 1).compareTo(version) == 0) {
                return wrapProject(ralProject, true);
            }
            // do not cache old version of project
            return wrapProject(ralProject.getVersion(version), false);
        } catch (ProjectException e) {
            throw new RepositoryException("Cannot find project ''{0}'' or its version ''{1}''!", e, name, version
                    .getVersionName());
        }
    }

    public Collection<AProject> getProjects() {
        List<AProject> result = new LinkedList<AProject>();

        try {
            for (FolderAPI ralProject : rulesRepository.getRulesProjects()) {
                String name = ralProject.getName();
                AProject cached = projects.get(name);

                if (cached != null) {
                    // use cached
                    result.add(cached);
                } else {
                    // get from the repository
                    AProject project = wrapProject(ralProject, true);
                    result.add(project);
                }
            }
        } catch (RRepositoryException e) {
            // TODO: re throw exception ?
            log.error("Cannot list projects!", e);
        }
        return result;
    }

    public boolean hasDDProject(String name) {
        try {
            return rulesRepository.hasDeploymentProject(name);
        } catch (RRepositoryException e) {
            String msg = MsgHelper.format("Failed to check deployment project ''{0}'' in the repository!", name);
            log.error(msg, e);
            return false;
        }
    }

    public boolean hasProject(String name) {
        AProject cached = projects.get(name);
        boolean inCache = (cached != null);

        try {
            boolean inRAL = rulesRepository.hasProject(name);
            if (inRAL != inCache) {
                if (!inRAL) {
                    // ???
                    projects.remove(name);
                }
            }
            return inRAL;
        } catch (RRepositoryException e) {
            String msg = MsgHelper.format("Failed to check project ''{0}'' in the repository!", name);
            log.error(msg, e);
        }

        return inCache;
    }

    public void updateProject(AProject sourceProject, WorkspaceUser user, int major, int minor)
            throws RepositoryException {
        String name = sourceProject.getName();
        AProject dest = getProject(name);

        if (!dest.isLocked()) {
            throw new RepositoryException("Cannot update project ''{0}'' while it is not locked!", null, name);
        }

        //FIXME
        WorkspaceUser lockedBy = (WorkspaceUser)dest.getLockInfo().getLockedBy();
        if (!lockedBy.equals(user)) {
            throw new RepositoryException("Project ''{0}'' is locked by other user ({0})!", null, name, lockedBy
                    .getUserName());
        }

        RRepository writeRep = null;

        try {
            log.debug("Opening temporary write session...");
            writeRep = RulesRepositoryFactory.getRepositoryInstance();
            log.debug("Wrapping temporary write project...");
            AProject project4Write = wrapProject(writeRep.getRulesProject(name), false);

            if (major != 0 || minor != 0) {
                String msg = MsgHelper.format("Raising project version (''{0}'' -> {1}.{2})...", name, major, minor);
                log.debug(msg);
            }

            project4Write.update(sourceProject, user, 0, 0);
            project4Write.checkIn(major, minor);
        } catch (Exception e) {
            throw new RepositoryException("Failed to update project ''{0}''.", e, name);
        } finally {
            if (writeRep != null) {
                log.debug("Releasing temporary write session...");
                writeRep.release();
            }
            // invalidate cache (rules projects)
            projects.remove(name);
        }
    }

    // --- private

    private ADeploymentProject wrapDDProject(FolderAPI folder) {
        return new ADeploymentProject(folder, user);
    }

    private AProject wrapProject(FolderAPI folder, boolean cacheIt) {
        AProject dtrRulesProject = new AProject(folder, user);
        if (cacheIt) {
            projects.put(folder.getName(), dtrRulesProject);
        }

        return dtrRulesProject;
    }

    public void addListener(DesignTimeRepositoryListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DesignTimeRepositoryListener listener) {
        listeners.remove(listener);
    }

    public List<DesignTimeRepositoryListener> getListeners() {
        return listeners;
    }

    public void onEventInRulesProjects(RRepositoryEvent event) {
        DTRepositoryEvent repositoryEvent = new DTRepositoryEvent(event.getProjectName());
        for (DesignTimeRepositoryListener listener : listeners) {
            listener.onRulesProjectModified(repositoryEvent);
        }
    }

    public void onEventInDeploymentProjects(RRepositoryEvent event) {
        DTRepositoryEvent repositoryEvent = new DTRepositoryEvent(event.getProjectName());
        for (DesignTimeRepositoryListener listener : listeners) {
            listener.onDeploymentProjectModified(repositoryEvent);
        }
    }
}
