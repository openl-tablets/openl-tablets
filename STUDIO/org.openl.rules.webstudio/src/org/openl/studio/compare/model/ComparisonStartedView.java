package org.openl.studio.compare.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The comparison that was accepted for execution.
 *
 * @param id identifier the comparison is read and watched by
 */
public record ComparisonStartedView(

        @Schema(description = """
                Identifier of the comparison. The result is read by it, and its progress is watched \
                on the topic '/topic/compare/{id}/status'.""")
        String id) {
}
