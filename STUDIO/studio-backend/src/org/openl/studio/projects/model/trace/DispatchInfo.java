package org.openl.studio.projects.model.trace;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Records that a frame's table was selected at runtime from a group of versions overloaded by dimension
 * properties (a dispatcher). The chosen version is the frame itself; the candidates explain what it was
 * chosen from, so the dispatch is shown as a badge on the selected version rather than as an extra tree level.
 *
 * @param candidates the overloaded versions, each labelled by its dimension properties, with the chosen one flagged
 */
@Schema(description = "trace.type.dispatch.desc")
public record DispatchInfo(
        @Schema(description = "trace.field.dispatch.candidates.desc")
        List<Candidate> candidates) {

    /**
     * One overloaded version of the dispatched rule.
     *
     * @param label  the version's dimension properties (for example {@code effectiveDate: 01/01/2020}), or its name
     * @param chosen whether the dispatcher selected this version for the current runtime context
     */
    @Schema(description = "trace.type.dispatch-candidate.desc")
    public record Candidate(
            @Schema(description = "trace.field.dispatch-candidate.label.desc")
            String label,
            @Schema(description = "trace.field.dispatch-candidate.chosen.desc")
            boolean chosen) {
    }
}
