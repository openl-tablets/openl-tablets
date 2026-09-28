package org.openl.studio.projects.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.common.ProjectException;
import org.openl.rules.lock.LockInfo;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;

/**
 * What {@link LockForEditing} means, proved through the proxy that gives it its meaning.
 */
class ProjectLockInterceptorTest {

    /** An operation of the kind the annotation is put on, so a test can say what it did. */
    static class Written {

        boolean ran;
        boolean heldWhileRunning;

        @LockForEditing
        public String write(RulesProject project) {
            ran = true;
            heldWhileRunning = project.isLockedByMe();
            return "written";
        }

        @LockForEditing
        public String refuse(RulesProject project) {
            ran = true;
            heldWhileRunning = project.isLockedByMe();
            throw new IllegalStateException("the writer would not take it");
        }

        @LockForEditing
        public String nameless() {
            ran = true;
            return "nothing to hold";
        }

        public String readOnly(RulesProject project) {
            ran = true;
            heldWhileRunning = project.isLockedByMe();
            return "read";
        }
    }

    /** The ACL as it answers a user who may write whatever they are asked about. */
    private static RepositoryAclService mayWrite() {
        var acl = mock(RepositoryAclService.class);
        when(acl.isGranted(any(), eq(List.of(BasePermission.WRITE)))).thenReturn(true);
        return acl;
    }

    private static Written locking(Written target) {
        return locking(target, mayWrite());
    }

    /** The target proxied as its own class, which is how the post-processor proxies it. */
    private static Written locking(Written target, RepositoryAclService acl) {
        var factory = new ProxyFactory(target);
        factory.addAdvisor(new DefaultPointcutAdvisor(
                AnnotationMatchingPointcut.forMethodAnnotation(LockForEditing.class),
                new ProjectLockInterceptor(acl)));
        return (Written) factory.getProxy();
    }

    /** A project nobody is holding, which takes the lock the moment it is asked. */
    private static RulesProject free() {
        var project = mock(RulesProject.class);
        when(project.getLockInfo()).thenReturn(LockInfo.NO_LOCK);
        when(project.isLockedByMe()).thenReturn(true);
        return project;
    }

    /** A project under a lock, held by the caller or by somebody else. */
    private static RulesProject held(String by, boolean byMe) {
        var project = mock(RulesProject.class);
        var lock = mock(LockInfo.class);
        when(lock.isLocked()).thenReturn(true);
        when(lock.getLockedBy()).thenReturn(by);
        when(project.getLockInfo()).thenReturn(lock);
        when(project.isLockedByMe(lock)).thenReturn(byMe);
        return project;
    }

    @Test
    void the_project_is_held_before_the_operation_runs_and_after_it_answers() throws Exception {
        var target = new Written();
        var project = free();
        var locked = locking(target);

        assertEquals("written", locked.write(project));

        verify(project).tryLockOrThrow();
        // The lock is there for the work the operation leaves behind, so it outlives the call.
        assertTrue(target.heldWhileRunning);
        verify(project, never()).releaseMyLock();
    }

    @Test
    void a_project_the_caller_is_already_holding_is_neither_taken_again_nor_given_back() throws Exception {
        var target = new Written();
        var project = held("admin", true);
        var locked = locking(target);

        assertEquals("written", locked.write(project));

        // They are editing: nothing to take, and nothing this call could give back.
        verify(project, never()).tryLockOrThrow();
        verify(project, never()).releaseMyLock();
    }

    @Test
    void a_project_another_user_is_holding_refuses_the_operation_and_names_them() throws Exception {
        var target = new Written();
        var project = held("user2", false);
        var locked = locking(target);

        var conflict = assertThrows(ConflictException.class, () -> locked.write(project));

        assertEquals("openl.error.409.project.locked.by.message", conflict.getErrorCode());
        assertEquals(List.of("user2"), List.of(conflict.getArgs()));
        // Nothing of the operation runs, and the lock is read once to say all of that.
        assertFalse(target.ran);
        verify(project, never()).tryLockOrThrow();
    }

    @Test
    void a_lock_whose_holder_cannot_be_read_still_refuses_the_operation() {
        var target = new Written();
        var project = held("", false);
        var locked = locking(target);

        var conflict = assertThrows(ConflictException.class, () -> locked.write(project));

        assertEquals("openl.error.409.project.locked.message", conflict.getErrorCode());
        assertFalse(target.ran);
    }

    @Test
    void a_project_taken_by_somebody_else_in_the_meantime_refuses_the_operation_too() throws Exception {
        var target = new Written();
        // Free when it was read, and gone by the time it was asked for.
        var project = mock(RulesProject.class);
        var taken = mock(LockInfo.class);
        when(taken.getLockedBy()).thenReturn("user2");
        when(project.getLockInfo()).thenReturn(LockInfo.NO_LOCK, taken);
        doThrow(new ProjectException("The project is locked by other user")).when(project).tryLockOrThrow();
        var locked = locking(target);

        var conflict = assertThrows(ConflictException.class, () -> locked.write(project));

        assertEquals("openl.error.409.project.locked.by.message", conflict.getErrorCode());
        assertFalse(target.ran);
    }

    @Test
    void a_reader_who_may_not_write_the_project_is_turned_away_before_anything_is_read() throws Exception {
        var target = new Written();
        var project = mock(RulesProject.class);
        // The ACL grants nothing, which is what a reader who may only read the project is answered with.
        var acl = mock(RepositoryAclService.class);
        var locked = locking(target, acl);

        assertThrows(ForbiddenException.class, () -> locked.write(project));

        // A lock stands in the name of whoever holds it: one taken for a reader who may not write would turn a
        // legitimate writer away, naming somebody who was never editing at all.
        verify(project, never()).getLockInfo();
        verify(project, never()).tryLockOrThrow();
        assertFalse(target.ran);
    }

    @Test
    void a_refused_operation_gives_back_the_lock_it_took_itself() throws Exception {
        var target = new Written();
        var project = free();
        var locked = locking(target);

        assertThrows(IllegalStateException.class, () -> locked.refuse(project));

        // The request wrote nothing, so it leaves the project free rather than reserved for a write that never
        // happened — clearing a lock its owner never meant to take needs an administrator.
        verify(project).releaseMyLock();
    }

    @Test
    void a_refused_operation_leaves_a_lock_the_caller_was_already_holding_alone() throws Exception {
        var target = new Written();
        var project = held("admin", true);
        var locked = locking(target);

        assertThrows(IllegalStateException.class, () -> locked.refuse(project));

        verify(project, never()).releaseMyLock();
    }

    @Test
    void a_give_back_that_fails_is_carried_alongside_the_refusal_rather_than_in_place_of_it() throws Exception {
        var target = new Written();
        var project = free();
        doThrow(new IllegalStateException("the lock engine is away")).when(project).releaseMyLock();
        var locked = locking(target);

        var refused = assertThrows(IllegalStateException.class, () -> locked.refuse(project));

        // The caller asked about the write, so that is what they are told about.
        assertEquals("the writer would not take it", refused.getMessage());
        assertEquals("the lock engine is away", refused.getSuppressed()[0].getMessage());
    }

    @Test
    void an_operation_marked_but_naming_no_project_is_a_mistake_rather_than_an_unguarded_write() {
        var target = new Written();
        var locked = locking(target);

        assertThrows(IllegalStateException.class, () -> locked.nameless());

        // Failing open here would be a write that looks guarded and is not.
        assertFalse(target.ran);
    }

    @Test
    void an_operation_that_is_not_marked_holds_nothing() throws Exception {
        var target = new Written();
        var project = mock(RulesProject.class);
        var locked = locking(target);

        assertEquals("read", locked.readOnly(project));

        verify(project, never()).getLockInfo();
        verify(project, never()).tryLockOrThrow();
        verify(project, never()).releaseMyLock();
    }
}
