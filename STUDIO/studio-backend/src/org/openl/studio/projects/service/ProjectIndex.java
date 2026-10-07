package org.openl.studio.projects.service;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;

/**
 * Waits until the project index publishes what a write changed.
 *
 * <p>A project of a repository with branches is read from the published index snapshot, so a write answers only once
 * the snapshot holds its result. Otherwise the next request reads the project as it was before the write.
 *
 * <p>A repository without branches is read directly: it is refreshed, and nothing is awaited.
 *
 * @author Yury Molchan
 */
@Slf4j
public final class ProjectIndex {

    /** How long a write waits for the index before it reports the indexing as incomplete. */
    private static final long TIMEOUT_SECONDS = 30;

    private ProjectIndex() {
    }

    /**
     * Refreshes one branch of the repository and waits until the project index publishes it.
     *
     * @return {@code false} when the index did not publish the branch in time or the wait was interrupted
     */
    public static boolean awaitBranch(DesignTimeRepository designTimeRepository,
                                      Repository repository,
                                      @Nullable String branch) {
        if (!supportsBranches(repository)) {
            designTimeRepository.refresh();
            return true;
        }
        var published = designTimeRepository.refreshBranch(repository.getId(), Objects.requireNonNull(branch));
        return await(published, "branch '%s' in repository '%s'".formatted(branch, repository.getId()));
    }

    /**
     * Refreshes every branch of the repository and waits until the project index publishes them.
     *
     * @return {@code false} when the index did not publish the repository in time or the wait was interrupted
     */
    public static boolean awaitRepository(DesignTimeRepository designTimeRepository, Repository repository) {
        var published = designTimeRepository.refreshRepository(repository.getId());
        return await(published, "repository '%s'".formatted(repository.getId()));
    }

    private static boolean supportsBranches(Repository repository) {
        return repository instanceof BranchRepository && repository.supports().branches();
    }

    private static boolean await(CompletionStage<Void> published, String what) {
        try {
            published.toCompletableFuture().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            log.warn("Project index did not publish {}.", what, e);
        }
        return false;
    }
}
