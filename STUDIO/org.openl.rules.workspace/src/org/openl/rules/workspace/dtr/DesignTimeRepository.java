package org.openl.rules.workspace.dtr;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
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
     * Returns whether a logical project exists, including entries hidden from the current security view.
     *
     * <p>This method is intended for name-collision checks. Callers must report only a generic conflict and must not
     * expose the hidden branch or path.
     */
    default boolean hasProjectInAnyBranch(String repositoryId, String name) {
        return hasProject(repositoryId, name);
    }

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

    /**
     * Returns the project entry verified in the requested branch.
     *
     * @throws ProjectException when the project does not exist in that branch
     */
    default AProject getProject(String repositoryId, String name, String branch) throws ProjectException {
        return getBranchedProject(repositoryId, name)
                .flatMap(project -> project.entry(branch))
                .map(BranchedProject.BranchEntry::project)
                .orElseThrow(() -> new ProjectException(
                        "Project ''{0}'' is not found in branch ''{1}''.", null, name, branch));
    }

    /**
     * Returns the logical project with every branch entry visible through this repository view.
     *
     * <p>A repository without branches returns an empty value.
     */
    default Optional<BranchedProject> getBranchedProject(String repositoryId, String name) {
        return Optional.empty();
    }

    /**
     * Whether the branch may be the only one holding the project, so deleting that branch would delete it.
     *
     * <p>A project the index does not know yet answers {@code true}: nothing proves another branch holds it, and
     * the caller is about to remove content. The answer describes the repository, not what the caller may see,
     * so it counts every branch holding the project.
     */
    default boolean isLastProjectBranch(String repositoryId, String name, String branch) {
        return getBranchedProject(repositoryId, name)
                .map(project -> project.heldOnlyBy(branch))
                .orElse(true);
    }

    /**
     * Returns the projects that no branch other than the given one holds, so deleting that branch deletes them.
     *
     * <p>Deleting a branch is a repository-wide operation: it removes every such project, not only the one the
     * caller addressed. The answer describes the repository, not what the caller may see.
     */
    default List<AProject> getProjectsHeldOnlyBy(String repositoryId, String branch) {
        return List.of();
    }

    /**
     * Returns the current background-index health for a branch-capable repository.
     */
    default Optional<BranchedProjectIndexService.IndexHealth> getProjectIndexHealth(String repositoryId) {
        return Optional.empty();
    }

    void refresh();

    /**
     * Refreshes one branch and completes after its new project membership is published.
     *
     * <p>Repositories without branches use their normal synchronous refresh.
     */
    default CompletionStage<Void> refreshBranch(String repositoryId, String branch) {
        refresh();
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Refreshes one repository and completes after its branch-wide project membership is published.
     *
     * <p>Repositories without branches use their normal synchronous refresh.
     */
    default CompletionStage<Void> refreshRepository(String repositoryId) {
        refresh();
        return CompletableFuture.completedFuture(null);
    }

    void addListener(DesignTimeRepositoryListener listener);

    void removeListener(DesignTimeRepositoryListener listener);

    Repository getRepository(String id);

    List<Repository> getRepositories();

    String getRulesLocation();

    List<String> getExceptions();
}
