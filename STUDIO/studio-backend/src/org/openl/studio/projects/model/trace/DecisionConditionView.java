package org.openl.studio.projects.model.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One evaluated decision-table condition, for one rule.
 *
 * @param condition condition name
 * @param rule      rule name
 * @param matched   whether the condition matched for that rule
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "trace.type.decision-condition.desc")
public record DecisionConditionView(
        @Schema(description = "trace.field.decision-condition.condition.desc")
        String condition,

        @Schema(description = "trace.field.decision-condition.rule.desc")
        String rule,

        @Schema(description = "trace.field.decision-condition.matched.desc")
        boolean matched
) {
}
