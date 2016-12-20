package org.openl.rules.repository;

import org.openl.rules.repository.api.ArtefactAPI;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.api.ResourceAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * Stub to use when repository cannot be initialized.
 *
 * @author Aleh Bykhavets
 *
 */
public class NullRepository implements RRepository {
    private static final List<RProject> EMPTY_LIST = new LinkedList<RProject>();

    protected void fail() throws RRepositoryException {
        throw new RRepositoryException("Failed to initialize repository!", null);
    }

    public boolean hasDeploymentProject(String name) throws RRepositoryException {
        return false;
    }

    public boolean hasProject(String name) throws RRepositoryException {
        return false;
    }

    public void release() {
        // Do nothing
    }

    public FolderAPI createDeploymentProject(String name) throws RRepositoryException {
        fail();
        // will never reach
        return null;
    }

    public FolderAPI createRulesProject(String name) throws RRepositoryException {
        fail();
        // will never reach
        return null;
    }

    public FolderAPI getDeploymentProject(String name) throws RRepositoryException {
        fail();
        // will never reach
        return null;
    }

    public List<FolderAPI> getDeploymentProjects() throws RRepositoryException {
        // empty list
        return new LinkedList<FolderAPI>();
    }

    @Override
    public String getDeploymentConfigRootPath() throws RRepositoryException {
        return null;
    }

    @Override
    public String getDeploymentsRootPath() throws RRepositoryException {
        return null;
    }

    public FolderAPI getRulesProject(String name) throws RRepositoryException {
        fail();
        // will never reach
        return null;
    }

    public List<FolderAPI> getRulesProjects() throws RRepositoryException {
        return new LinkedList<FolderAPI>();
    }

    @Override
    public String getRulesProjectsRootPath() throws RRepositoryException {
        return null;
    }

    public void addRepositoryListener(RRepositoryListener listener) {
    }

    public void removeRepositoryListener(RRepositoryListener listener) {
    }

    @Override
    public ArtefactAPI getArtefact(String name) throws RRepositoryException {
        return null;
    }

    @Override
    public ResourceAPI createResource(String name, InputStream inputStream) throws RRepositoryException {
        return null;
    }

    @Override
    public ArtefactAPI rename(String path, String destination) throws RRepositoryException {
        return null;
    }

}
