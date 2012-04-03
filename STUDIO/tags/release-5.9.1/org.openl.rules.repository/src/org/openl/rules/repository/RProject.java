package org.openl.rules.repository;

import java.util.Collection;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectDependency;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * OpenL Rules Project. Use root folder to access all folders and files of the
 * project.
 *
 * @author Aleh Bykhavets
 *
 */
public interface RProject extends RFolder, RCommonProject {

    Collection<ProjectDependency> getDependencies() throws RRepositoryException;

    RProject getProjectVersion(CommonVersion version) throws RRepositoryException;

    /**
     * Returns root folder of the project.
     *
     * @return root folder
     */
    RFolder getRootFolder();

    void setDependencies(Collection<? extends ProjectDependency> dependencies) throws RRepositoryException;

}
