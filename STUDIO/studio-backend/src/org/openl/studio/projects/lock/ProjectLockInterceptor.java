package org.openl.studio.projects.lock;

import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.stereotype.Component;

import org.openl.rules.common.ProjectException;
import org.openl.rules.lock.LockInfo;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.util.StringUtils;

/**
 * Interceptor that holds the project for the caller around a {@link LockForEditing} operation.
 *
 * <p>The project is the one the method is called about: its {@link RulesProject} argument. Whether the caller
 * may write it is settled before it is taken, because a lock stands in the name of whoever holds it — one taken
 * for a reader who may not write would turn a legitimate writer away, naming somebody who was never editing at
 * all.
 *
 * <p>A project the caller is already holding is theirs to go on working in, and a project somebody else holds
 * refuses the call. Only a free one is taken, and only such a call gives its lock back where it is refused past
 * that point — by a table that is not there, by a layout the writer will not take — so a request that wrote
 * nothing leaves the project as it found it.
 *
 * <p>An operation that succeeds keeps the lock. It is given up by saving the project, by closing it, or by the
 * editor saying the table has been put down — see {@code Docs/architecture/project-editing-lock.md}.
 *
 * @see LockForEditing
 * @see ProjectLockPostProcessor
 */
@Component
public class ProjectLockInterceptor implements MethodInterceptor {

    private static final List<Permission> WRITE = List.of(BasePermission.WRITE);

    private final RepositoryAclService designRepositoryAclService;

    public ProjectLockInterceptor(
            @Lazy @Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService) {
        this.designRepositoryAclService = designRepositoryAclService;
    }

    @Nullable
    @Override
    public Object invoke(@NonNull MethodInvocation invocation) throws Throwable {
        var project = projectOf(invocation);
        requireMayWrite(project);
        // One read answers all three states the lock can be in, and the engine reads the same file again on the
        // way in, so nothing here asks it twice.
        var lock = project.getLockInfo();
        if (lock.isLocked()) {
            if (!project.isLockedByMe(lock)) {
                throw heldByAnother(lock);
            }
            // Already the caller's: they are editing, and there is nothing to take or to give back.
            return invocation.proceed();
        }
        take(project);
        try {
            return invocation.proceed();
        } catch (Throwable refused) {
            giveBack(project, refused);
            throw refused;
        }
    }

    /** The project the call is about, which an operation done under the lock has to name. */
    private static RulesProject projectOf(MethodInvocation invocation) {
        for (var argument : invocation.getArguments()) {
            if (argument instanceof RulesProject project) {
                return project;
            }
        }
        throw new IllegalStateException(
                "@LockForEditing on " + invocation.getMethod() + " names no project to hold");
    }

    /** Only somebody who may write the project takes it up, and a reader is told so before anything is held. */
    private void requireMayWrite(RulesProject project) {
        if (!designRepositoryAclService.isGranted(project, WRITE)) {
            throw new ForbiddenException("default.message");
        }
    }

    /** Takes a project that was free a moment ago, or says who got to it first. */
    private static void take(RulesProject project) {
        try {
            project.tryLockOrThrow();
        } catch (ProjectException lost) {
            throw heldByAnother(project.getLockInfo());
        }
    }

    /**
     * Says who is holding the project, as the conflict the API documents rather than as a fault of this request.
     *
     * <p>A caller told only that the Studio broke has nothing to do but retry into the same wall, while one told
     * who is editing knows whom to wait for.
     */
    private static ConflictException heldByAnother(LockInfo lock) {
        var lockedBy = lock.getLockedBy();
        return StringUtils.isBlank(lockedBy)
                ? new ConflictException("project.locked.message")
                : new ConflictException("project.locked.by.message", lockedBy);
    }

    /**
     * Gives back the lock this call took.
     *
     * <p>Whatever the lock engine answers with here is carried alongside the refusal rather than in place of it:
     * the caller asked about the write, and a repository hiccup on the way out must not turn a plain 404 into an
     * unrelated fault.
     */
    private static void giveBack(RulesProject project, Throwable refused) {
        try {
            // Asked again rather than assumed: a lock can be broken from under its holder while the call runs,
            // and what stands there afterwards may be somebody else's work rather than this call's leftovers.
            project.releaseMyLock();
        } catch (ProjectException | RuntimeException giveBackFailed) {
            refused.addSuppressed(giveBackFailed);
        }
    }
}
