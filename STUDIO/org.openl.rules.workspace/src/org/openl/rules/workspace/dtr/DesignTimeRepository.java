package org.openl.rules.workspace.dtr;

import java.util.List;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.abstracts.ProjectsContainer;

/**
 * Design Time Repository.
 * <p/>
 * Version Storage for development phase.
 * <p/>
 * Rules and Deployment projects are treated separately.
 *
 * @author Aleh Bykhavets
 *
 */
public interface DesignTimeRepository extends ProjectsContainer {

    /**
     * Creates new rules project in the Design Time Repository.
     *
     * @param name name of new rules project, must be unique
     * @throws RepositoryException if failed
     */
    AProject createProject(String name) throws RepositoryException;

    /**
     * Gets deployment project from the DTR.
     *
     * @param name name of deployment project
     * @return instance of deployment project
     */
    ADeploymentProject.Builder createDeploymentConfigurationBuilder(String name);

    /**
     * Returns list of all deployment projects from the DTR.
     *
     * @return list of deployment projects
     * @throws RepositoryException if failed
     */
    List<ADeploymentProject> getDDProjects() throws RepositoryException;

    /**
     * Gets particular version of a rules project.
     *
     * @param name name of rules project
     * @param version exact version of project
     * @return specified version of rules project
     * @throws RepositoryException if failed
     */
    AProject getProject(String name, CommonVersion version) throws RepositoryException;

    /**
     * Checks whether the DTR has deployment project with specified name.
     *
     * @param name name of deployment project to be checked
     * @return <code>true</code> if deployment project with specified name exists already
     */
    boolean hasDDProject(String name);

    void addListener(DesignTimeRepositoryListener listener);

    void removeListener(DesignTimeRepositoryListener listener);

    List<DesignTimeRepositoryListener> getListeners();

    Repository getRepository();

    String getRulesLocation();
}
