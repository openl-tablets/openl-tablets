package org.openl.studio.common.projection;

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import lombok.RequiredArgsConstructor;

/**
 * Tags every projectable response DTO with the shared field-projection filter id.
 *
 * <p>Projection works without a {@code @JsonFilter} annotation on each DTO -- this introspector
 * supplies the id at runtime.
 *
 * <p>Pair it as the primary introspector with the default Jackson one: non-projectable types return
 * {@code null} and fall through to the delegate.
 *
 * @author Vladyslav Pikus
 */
@RequiredArgsConstructor
public class FieldProjectionAnnotationIntrospector extends NopAnnotationIntrospector {

    private final transient FieldProjectionSupport support;

    @Override
    public Object findFilterId(Annotated annotated) {
        if (annotated instanceof AnnotatedClass annotatedClass && support.isProjectable(annotatedClass.getRawType())) {
            // All projectable DTOs share one filter id so the path-aware filter can project nested levels.
            return FieldProjectionSupport.FILTER_ID;
        }
        return null;
    }
}
