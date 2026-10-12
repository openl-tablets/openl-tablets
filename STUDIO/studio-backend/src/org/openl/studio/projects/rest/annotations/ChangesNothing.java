package org.openl.studio.projects.rest.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint about a project that changes nothing in it, though it is no {@code GET}: it compiles, runs,
 * traces, compares or searches the project, or lets go of what it was holding for the client.
 *
 * <p>Such a request goes on while the client merges the project, as a read does: the editor keeps showing and
 * running the project for the minutes a merge takes. Every other request about the project but a read is refused
 * meanwhile — see {@link org.openl.studio.projects.rest.MergeInProgressInterceptor}.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChangesNothing {
}
