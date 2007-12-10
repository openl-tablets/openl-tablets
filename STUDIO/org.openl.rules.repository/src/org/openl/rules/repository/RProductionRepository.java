package org.openl.rules.repository;

import org.openl.rules.repository.exceptions.RRepositoryException;

import java.util.List;

public interface RProductionRepository extends RRepository {
    boolean hasDeployment(String name) throws RRepositoryException;

    RProductionDeployment createDeployment(String name) throws RRepositoryException;

    RProductionDeployment getDeployment(String name) throws RRepositoryException;
}
