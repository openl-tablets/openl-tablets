package org.openl.studio.projects.lock;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an operation that takes the project up for editing, and so holds its lock while it runs.
 *
 * <p>The lock is there for one thing: the user has changes waiting in their workspace, and another user must not
 * write the same project over them. So every operation that leaves such a change behind — a table written, a
 * table taken up to be written — is done under the lock, and this annotation is how it says so. Nothing in the
 * method body takes it: the project named by the method's {@code RulesProject} argument is locked before the
 * call and, where the call was refused, given back after it.
 *
 * <p>A project another user is already holding refuses the call with a conflict naming them, so a caller is
 * never left to guess whom to wait for.
 *
 * @author Vladyslav Pikus
 * @see ProjectLockInterceptor
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LockForEditing {
}
