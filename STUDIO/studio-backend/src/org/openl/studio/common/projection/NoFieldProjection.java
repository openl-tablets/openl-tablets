package org.openl.studio.common.projection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a response DTO that must never be reduced by the {@code fields} projection.
 *
 * <p>The projection targets a response's content elements. A summary or metadata object carried alongside
 * that content (for example the per-status counts next to a projected page) would otherwise be filtered
 * with the same content-oriented field selection and lose all of its own fields. Annotate such types to
 * keep them intact.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NoFieldProjection {
}
