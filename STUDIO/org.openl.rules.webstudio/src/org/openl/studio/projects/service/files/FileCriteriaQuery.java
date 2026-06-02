package org.openl.studio.projects.service.files;

import java.util.Set;

import lombok.Builder;
import lombok.Singular;

/**
 * Query criteria for filtering project resources.
 *
 */
@Builder
public record FileCriteriaQuery(

        String basePath,

        @Singular
        Set<String> extensions,

        String namePattern,

        boolean foldersOnly

) {

    public FileCriteriaQuery {
        extensions = extensions != null ? Set.copyOf(extensions) : Set.of();
    }
}
