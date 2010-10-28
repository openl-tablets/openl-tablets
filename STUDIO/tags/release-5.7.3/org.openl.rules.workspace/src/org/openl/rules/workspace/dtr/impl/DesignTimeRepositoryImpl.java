package org.openl.rules.workspace.dtr.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.NullRepository;
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
import org.openl.util.MsgHelper;

/**
 *
 * @author Aleh Bykhavets
 *
 */
public class DesignTimeRepositoryImpl implements DesignTimeRepository {
    private static final Log log = LogFactory.getLog(DesignTimeRepositoryImpl.class);

    /** Rules Repository */
    private RRepository rulesRepository;
    /** Project Cache */
    private HashMap<String, RepositoryProjectImpl> projects;

    public DesignTimeRepositoryImpl() {
        try {
            rulesRepository = RulesRepositoryFactory.getRepositoryInstance();
        } catch (RRepositoryException e) {
            log.error("Cannot init DTR! " + e.getMessage());
            rulesRepository = new NullRepository();
        }

        projects = new HashMap<String, RepositoryProjectImpl>();
    }

    public void copyDDProject(DeploymentDescriptorProject project, String name, WorkspaceUser user)
            throws ProjectException {
        createDDProject(name);
        RepositoryDDProject newProject = getDDProject(name);
        newProject.commit((Project) project, user);
    }

    public void copyProject(Project project, String name, WorkspaceUser user) throws ProjectException {
        if (hasProject(name)) {
            throw new ProjectException("Project ''{0}'' is already exist in the repository!", null, name);
        }

        RRepository writeRep = null;

        try {
            log.debug("Opening temporary write session...");
            writeRep = RulesRepositoryFactory.getRepositoryInstance();
            log.debug("Wrapping temporary write project...");
            RepositoryProject newProject = wrapProject(writeRep.createProject(name), false);

            newProject.commit(project, user);
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
            rulesRepository.createDDProject(name);
        } catch (RRepositoryException e) {
            throw new RepositoryException("Failed to create deployment project ''{0}''!", e, name);
        }
    }

    public void createProject(String name) throws RepositoryException {
        try {
            rulesRepository.createProject(name);
        } catch (Exception e) {
            throw new RepositoryException("Failed to create project ''{0}''!", e, name);
        }
    }

    public ProjectArtefact getArtefactByPath(ArtefactPath artefactPath) throws ProjectException {
        String projectName = artefactPath.segment(0);
        RepositoryProject ralProject = getProject(projectName);

        ArtefactPath pathInProject = artefactPath.withoutFirstSegment();
        return ralProject.getArtefactByPath(pathInProject);
    }

    public RepositoryDDProject getDDProject(String name) throws RepositoryException {
        try {
            return wrapDDProject(rulesRepository.getDDProject(name));
        } catch (RRepositoryException e) {
            throw new RepositoryException("Cannot find project ''{0}''!", e, name);
        }
    }

    public RepositoryDDProject getDDProject(String name, CommonVersion version) throws RepositoryException {
        try {
            RDeploymentDescriptorProject ralDeploymentProject = rulesRepository.getDDProject(name);
            RDeploymentDescriptorProject oldProject = ralDeploymentProject.getProjectVersion(version);
            return wrapDDProject(oldProject);
        } catch (RRepositoryException e) {
            throw new RepositoryException("Cannot find project ''{0}'' or its version ''{1}''!", e, name, version
                    .getVersionName());
        }
    }

    public List<RepositoryDDProject> getDDProjects() throws RepositoryException {
        LinkedList<RepositoryDDProject> result = new LinkedList<RepositoryDDProject>();

        try {
            for (RDeploymentDescriptorProject ralDeploymentProject : rulesRepository.getDDProjects()) {
                RepositoryDDProject dtrDeploymentProject = wrapDDProject(ralDeploymentProject);
                result.add(dtrDeploymentProject);
            }
        } catch (RRepositoryException e) {
            // TODO: re throw exception ?
            log.error("Cannot list deployments projects!", e);
        }
        return result;
    }

    public RepositoryProject getProject(String name) throws RepositoryException {
        if (!hasProject(name)) {
            throw new RepositoryException("Cannot find project ''{0}''!", null, name);
        }

        RepositoryProject cached = projects.get(name);
        if (cached != null) {
            return cached;
        }

        try {
            RProject ralProject = rulesRepository.getProject(name);
            return wrapProject(ralProject, true);
        } catch (RRepositoryException e) {
            throw new RepositoryException("Cannot find project ''{0}''!", e, name);
        }
    }

    public RepositoryProject getProject(String name, CommonVersion version) throws RepositoryException {
        try {
            RProject ralProject = rulesRepository.getProject(name);
            RProject oldProject = ralProject.getProjectVersion(version);

            // do not cache old version of project
            return wrapProject(oldProject, false);
        } catch (RRepositoryException e) {
            throw new RepositoryException("Cannot find project ''{0}'' or its version ''{1}''!", e, name, version
                    .getVersionName());
        }
    }

    public Collection<RepositoryProject> getProjects() {
        List<RepositoryProject> result = new LinkedList<RepositoryProject>();

        try {
            for (RProject ralProject : rulesRepository.getProjects()) {
                String name = ralProject.getName();
                RepositoryProject cached = projects.get(name);

                if (cached != null) {
                    // use cached
                    result.add(cached);
                } else {
                    // get from the repository
                    RepositoryProject project = wrapProject(ralProject, true);
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
            return rulesRepository.hasDDProject(name);
        } catch (RRepositoryException e) {
            String msg = MsgHelper.format("Failed to check deployment project ''{0}'' in the repository!", name);
            log.error(msg, e);
            return false;
        }
    }

    public boolean hasProject(String name) {
        RepositoryProject cached = projects.get(name);
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

    public void updateProject(Project sourceProject, WorkspaceUser user, int major, int minor)
            throws RepositoryException {
        String name = sourceProject.getName();
        RepositoryProject dest = getProject(name);

        if (!dest.isLocked()) {
            throw new RepositoryException("Cannot update project ''{0}'' while it is not locked!", null, name);
        }

        WorkspaceUser lockedBy = dest.getlLockInfo().getLockedBy();
        if (!lockedBy.equals(user)) {
            throw new RepositoryException("Project ''{0}'' is locked by other user ({0})!", null, name, lockedBy
                    .getUserName());
        }

        RRepository writeRep = null;

        try {
            log.debug("Opening temporary write session...");
            writeRep = RulesRepositoryFactory.getRepositoryInstance();
            log.debug("Wrapping temporary write project...");
            RepositoryProject project4Write = wrapProject(writeRep.getProject(name), false);

            if (major != 0 || minor != 0) {
                String msg = MsgHelper.format("Raising project version (''{0}'' -> {1}.{2})...", name, major, minor);
                log.debug(msg);
                project4Write.riseVersion(major, minor);
            }

            project4Write.commit(sourceProject, user);
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

    private RepositoryDDProject wrapDDProject(RDeploymentDescriptorProject ralDeploymentProject) {
        return new RepositoryDeploymentDescriptorProjectImpl(ralDeploymentProject);
    }

    private RepositoryProjectImpl wrapProject(RProject ralRulesProject, boolean cacheIt) {
        String name = ralRulesProject.getName();
        ArtefactPath projectPath = new ArtefactPathImpl(new String[] { name });

        RepositoryProjectImpl dtrRulesProject = new RepositoryProjectImpl(ralRulesProject, projectPath);
        if (cacheIt) {
            projects.put(name, dtrRulesProject);
        }

        return dtrRulesProject;
    }
}
