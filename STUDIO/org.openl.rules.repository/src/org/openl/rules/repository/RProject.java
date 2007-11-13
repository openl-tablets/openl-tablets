package org.openl.rules.repository;

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
}
