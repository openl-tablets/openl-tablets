package org.openl.studio.projects.model.trace;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Plain-language explanation of a decision table's outcome.
 *
 * <p>Says which rule fired and how each evaluated condition turned out, so a business user can see why a
 * rule produced its result without reading the grid.
 *
 * @param firedRules names of the rules that fired
 * @param conditions evaluated conditions with their per-rule match results
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "trace.type.decision.desc")
public record DecisionView(
        @Schema(description = "trace.field.decision.fired-rules.desc")
        List<String> firedRules,

        @Schema(description = "trace.field.decision.conditions.desc")
        List<DecisionConditionView> conditions
) {
}
