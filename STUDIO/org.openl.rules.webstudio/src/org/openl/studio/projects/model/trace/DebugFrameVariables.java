package org.openl.studio.projects.model.trace;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.rules.rest.compile.MessageDescription;
import org.openl.studio.projects.model.ParameterValue;

/**
 * Frozen variables of a stack frame, captured while execution is suspended.
 *
 * @param parameters input parameters of the frame
 * @param context    runtime context, or {@code null}
 * @param result     return value when the frame has completed, otherwise {@code null}
 * @param steps       already-executed sub-steps with their computed values
 * @param gridColumns spreadsheet column names, so the UI can lay the steps out as a grid (else null)
 * @param gridRows    spreadsheet row names (else null)
 * @param decision    decision-table outcome explanation (only for decision-table frames, else null)
 * @param ruleNames   all rule names of a decision-table frame, so any rule can be armed (else null)
 * @param errors      errors when the frame failed
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "trace.type.debug-variables.desc")
public record DebugFrameVariables(
        @Schema(description = "trace.field.variables.parameters.desc")
        List<ParameterValue> parameters,

        @Schema(description = "trace.field.variables.context.desc")
        @Nullable ParameterValue context,

        @Schema(description = "trace.field.variables.result.desc")
        @Nullable ParameterValue result,

        @Schema(description = "trace.field.variables.steps.desc")
        List<StepValueView> steps,

        @Schema(description = "trace.field.variables.grid-columns.desc")
        @Nullable List<String> gridColumns,

        @Schema(description = "trace.field.variables.grid-rows.desc")
        @Nullable List<String> gridRows,

        @Schema(description = "trace.field.variables.decision.desc")
        @Nullable DecisionView decision,

        @Schema(description = "trace.field.variables.rule-names.desc")
        @Nullable List<String> ruleNames,

        @Schema(description = "trace.field.variables.errors.desc")
        List<MessageDescription> errors
) {
}
