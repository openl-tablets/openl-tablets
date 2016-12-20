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
