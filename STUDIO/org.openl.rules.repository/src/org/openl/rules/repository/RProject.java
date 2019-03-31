package org.openl.rules.repository;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * OpenL Rules Project. Use root folder to access all folders and files of the project.
 *
 * @author Aleh Bykhavets
 *
 */
public interface RProject extends RFolder, RCommonProject {

    RProject getProjectVersion(CommonVersion version) throws RRepositoryException;

    /**
     * Returns root folder of the project.
     *
     * @return root folder
     */
    RFolder getRootFolder();

}
