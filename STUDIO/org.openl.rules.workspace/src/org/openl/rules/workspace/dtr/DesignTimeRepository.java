package org.openl.rules.workspace.dtr;

import java.io.IOException;
import java.util.List;

import org.openl.rules.common.CommonVersion;
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
 */
public interface DesignTimeRepository extends ProjectsContainer {

    /**
     * Gets particular version of a rules project.
     *
     * @param name    name of rules project
     * @param version exact version of project
     * @return specified version of rules project
     * @deprecated This method is used for backward compatibility with old version of deploy configs. Use
     * getProjectByPath() instead.
     */
    @Deprecated
    AProject getProject(String repositoryId, String name, CommonVersion version);

    AProject getProjectByPath(String repositoryId, String branch, String path, String version) throws IOException;

    void refresh();

    void addListener(DesignTimeRepositoryListener listener);

    void removeListener(DesignTimeRepositoryListener listener);

    Repository getRepository(String id);

    List<Repository> getRepositories();

    String getRulesLocation();

    List<String> getExceptions();
}
