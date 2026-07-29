package org.openl.studio.projects.model.trace;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.openl.rules.rest.compile.MessageDescription;
import org.openl.studio.projects.model.ParameterValue;

/**
 * A focused spreadsheet step, self-contained for the business view.
 *
 * <p>Carries the values the step's formula consumed, the step's own returned value, and the A1 address of
 * its cell. This is everything the step panel shows, in one payload, so a step click need not also fetch
 * the frame's full variables — every cell value, the grid, the decision breakdown — that the panel omits.
 *
 * @param inputs the values the step's formula read, named as the formula writes them
 * @param result the step's own returned value, or {@code null} for a formula cell that has not run yet
 * @param cell   A1 address of the step's source cell in the raw table, or {@code null}
 * @param errors the errors this step raised, present only when this is the step the run failed on
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "trace.type.step-inputs.desc")
public record StepInputsView(
        @Schema(description = "trace.field.step-inputs.inputs.desc")
        List<ParameterValue> inputs,

        @Parameter(description = "trace.field.step-inputs.result.desc")
        @Nullable ParameterValue result,

        @Schema(description = "trace.field.step-inputs.cell.desc")
        @Nullable String cell,

        @Schema(description = "trace.field.step-inputs.errors.desc")
        @Nullable List<MessageDescription> errors
) {
}
