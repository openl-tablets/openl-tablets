package org.openl.studio.projects.service.resources;

import java.util.Set;

import lombok.Builder;
import lombok.Singular;

/**
 * Query criteria for filtering project resources.
 *
 */
@Builder
public record ResourceCriteriaQuery(

        String basePath,

        @Singular
        Set<String> extensions,

        String namePattern,

        boolean foldersOnly

) {

    public ResourceCriteriaQuery {
        extensions = extensions != null ? Set.copyOf(extensions) : Set.of();
    }
}
