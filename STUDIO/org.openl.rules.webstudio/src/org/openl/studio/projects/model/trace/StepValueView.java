package org.openl.studio.projects.model.trace;

import java.util.List;

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
 * <p>In profiling mode an executed step also carries the tables it called, as {@code children}, so a
 * returned branch can be browsed as a collapsible sub-tree.
 *
 * @param ref      short reference of the step (for example {@code R2C3})
 * @param label    human-readable name, or {@code null}
 * @param status   one of {@code executed}, {@code current}, {@code pending}
 * @param value    the frozen computed value for an executed step, otherwise {@code null}
 * @param children the tables this step called (profiling mode only), otherwise {@code null}
 * @param durationMillis total execution time of an executed step in ms (own work plus called tables), else {@code null}
 * @param selfMillis     own execution time of an executed step in ms (total minus called tables), else {@code null}
 */
@Builder(toBuilder = true)
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
        @Nullable ParameterValue value,

        @Schema(description = "trace.field.step.children.desc")
        @Nullable List<CallNodeView> children,

        @Schema(description = "trace.field.step.duration.desc")
        @Nullable Double durationMillis,

        @Schema(description = "trace.field.step.self.desc")
        @Nullable Double selfMillis
) {
}
