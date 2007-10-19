package org.openl.rules.ui.repository;

import org.openl.rules.repository.RRepository;
import org.openl.rules.repository.RProject;
import org.openl.rules.repository.exceptions.RRepositoryException;

import java.util.LinkedList;
import java.util.List;

public class DummyRepository implements RRepository {
    private static final List<RProject> EMPTY_LIST = new LinkedList<RProject>();

    public List<RProject> getProjects() throws RRepositoryException {
        return EMPTY_LIST;
    }

    public List<RProject> getProjects4Deletion() throws RRepositoryException {
        return EMPTY_LIST;
    }

    public RProject createProject(String name) throws RRepositoryException {
        fail();
        // will never reach
        return null;
    }

    public void release() {
        // Do nothing
    }

    public String getName() {
        // TODO: may be put here something more consistent
        return "Failed to init Repository";
    }

    protected void fail() throws RRepositoryException {
        throw new RRepositoryException("Failed to initialize repository", null);
    }
}
