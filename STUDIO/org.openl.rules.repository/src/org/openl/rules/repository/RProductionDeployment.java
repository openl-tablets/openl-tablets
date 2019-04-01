package org.openl.rules.repository;

import java.util.Collection;

import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Represents a set of deployed projects to production repository.
 */
public interface RProductionDeployment extends REntity {
    RProject createProject(String projectName) throws RRepositoryException;

    RProject getProject(String name) throws RRepositoryException;

    Collection<RProject> getProjects() throws RRepositoryException;

    boolean hasProject(String name) throws RRepositoryException;

    void save() throws RRepositoryException;
}
