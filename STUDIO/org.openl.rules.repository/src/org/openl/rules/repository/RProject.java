package org.openl.rules.repository;

import java.util.Collection;

import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * OpenL Rules Project.
 * Use root folder to access all folders and files of the project.
 *
 * @author Aleh Bykhavets
 *
 */
public interface RProject extends REntity, RCommonProject {
    /**
     * Returns root folder of the project.
     *
     * @return root folder
     */
    public RFolder getRootFolder();
    
    public Collection<RDependency> getDependencies() throws RRepositoryException;
    public void setDependencies(Collection<? extends RDependency> dependencies) throws RRepositoryException;

    public RProject getProjectVersion(CommonVersion version) throws RRepositoryException;
}
