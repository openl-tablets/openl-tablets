package org.openl.studio.projects.model.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.studio.projects.model.ParameterValue;

/**
 * One sub-step of a frame: a spreadsheet cell or a decision-table rule.
 *
 * <p>Carries the step's status (already executed, currently executing, or pending) and, for executed
 * steps, the frozen computed value. The {@code ref} is the breakpoint key suffix ({@code uri#ref}).
 *
 * @param ref    short reference of the step (for example {@code R2C3})
 * @param label  human-readable name, or {@code null}
 * @param status one of {@code executed}, {@code current}, {@code pending}
 * @param value  the frozen computed value for an executed step, otherwise {@code null}
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "trace.type.step-value.desc")
public record StepValueView(
        @Schema(description = "trace.field.step.ref.desc")
        String ref,

        @Schema(description = "trace.field.step.label.desc")
        @Nullable String label,

        @Schema(description = "trace.field.step.status.desc")
        String status,

        @Schema(description = "trace.field.step.value.desc")
        @Nullable ParameterValue value
) {
}
