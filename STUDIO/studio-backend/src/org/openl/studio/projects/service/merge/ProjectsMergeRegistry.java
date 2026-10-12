package org.openl.studio.projects.service.merge;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.merge.MergeOpMode;
import org.openl.studio.session.ClientSessionScope;

/**
 * The branch merge a client has started: a browser session, or the credentials of a stateless client.
 *
 * <p>A client runs one merge at a time. The same merge asked for again while it runs is the merge already running,
 * and another merge is refused until it ends. A finished merge stays until the next one starts, or until the
 * conflicts it left are resolved or cancelled, so its result can be read again.
 */
@Component
@ClientSessionScope
public class ProjectsMergeRegistry {

    private final Lock starting = new ReentrantLock();
    private final AtomicReference<StartedMerge> current = new AtomicReference<>();

    /**
     * The lock a start of a merge by this client holds from its checks until the merge is taken, so two requests of
     * the client never start merges at once.
     *
     * @return the lock of this client
     */
    Lock startLock() {
        return starting;
    }

    /**
     * Whether the same merge of this client is running.
     *
     * @param projectId   the project to merge
     * @param otherBranch the other branch of the merge
     * @param mode        the direction of the merge
     * @return {@code true} while the same merge runs; {@code false} when no merge of this client runs
     * @throws ConflictException when another merge of this client is still running
     */
    boolean isRunning(ProjectIdModel projectId, String otherBranch, MergeOpMode mode) {
        var merge = current.get();
        if (merge == null || !merge.isRunning()) {
            return false;
        }
        if (merge.isSameAs(projectId, otherBranch, mode)) {
            return true;
        }
        throw new ConflictException("project.merge.running.message");
    }

    /**
     * Takes a merge as the one of this client. Called with the {@link #startLock()} held, once no merge of the
     * client runs.
     *
     * @param merge the merge, not running yet
     */
    void set(StartedMerge merge) {
        current.set(merge);
    }

    /**
     * Lets go of the ended merge of the project. A merge that runs, or one of another project, stays.
     *
     * @param projectId the project
     */
    void forget(ProjectIdModel projectId) {
        current.updateAndGet(merge -> merge != null && !merge.isRunning() && merge.projectId().equals(projectId)
                ? null
                : merge);
    }

    /**
     * The merge of the project this client started last.
     *
     * @param projectId the project
     * @return the merge, or nothing when the last merge of this client was of another project, or there was none
     */
    Optional<StartedMerge> find(ProjectIdModel projectId) {
        return current().filter(merge -> merge.projectId().equals(projectId));
    }

    /**
     * The merge this client started last, of any project.
     *
     * @return the merge, or nothing when this client started none
     */
    Optional<StartedMerge> current() {
        return Optional.ofNullable(current.get());
    }
}
