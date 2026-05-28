package org.openl.studio.common.projection;

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;

/**
 * Assigns a Jackson filter id to every projectable response DTO so that
 * {@link FieldProjectionResponseBodyAdvice} can apply a per-request property filter without requiring a
 * {@code @JsonFilter} annotation on each DTO.
 *
 * <p>It is meant to be paired (as the primary introspector) with the default Jackson introspector;
 * for every non-projectable type it returns {@code null}, delegating to the paired introspector.
 *
 * @author Vladyslav Pikus
 */
public class FieldProjectionAnnotationIntrospector extends NopAnnotationIntrospector {

    private final transient FieldProjectionSupport support;

    public FieldProjectionAnnotationIntrospector(FieldProjectionSupport support) {
        this.support = support;
    }

    @Override
    public Object findFilterId(Annotated annotated) {
        if (annotated instanceof AnnotatedClass annotatedClass && support.isProjectable(annotatedClass.getRawType())) {
            // All projectable DTOs share one filter id so the path-aware filter can project nested levels.
            return FieldProjectionSupport.FILTER_ID;
        }
        return null;
    }
}
