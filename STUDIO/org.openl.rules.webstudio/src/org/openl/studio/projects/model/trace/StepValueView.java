package org.openl.studio.projects.model.trace;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
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
 * returned branch can be browsed as a collapsible sub-tree. A step in a large loop can call very many
 * tables; {@code children} is capped and {@code childrenTotal} then reports how many actually ran.
 *
 * @param ref      short reference of the step (for example {@code R2C3})
 * @param label    human-readable name, or {@code null}
 * @param cell     A1 address of the step's source cell in the raw table (spreadsheet cells only), or {@code null}
 * @param constant true for a plain value or constant cell — static content that never executes, or {@code null}
 * @param status   whether the step has executed, is executing, or is still pending
 * @param value    the frozen computed value for an executed step, otherwise {@code null}
 * @param children the tables this step called (profiling mode only), capped, otherwise {@code null}
 * @param childrenTotal total number of tables this step called, set only when {@code children} was capped, otherwise {@code null}
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

        @Schema(description = "trace.field.step.cell.desc")
        @Nullable String cell,

        @Schema(description = "trace.field.step.constant.desc")
        @Nullable Boolean constant,

        @Schema(description = "trace.field.step.status.desc")
        StepStatus status,

        @Parameter(description = "trace.field.step.value.desc")
        @Nullable ParameterValue value,

        @Schema(description = "trace.field.step.children.desc")
        @Nullable List<CallNodeView> children,

        @Schema(description = "trace.field.step.children-total.desc")
        @Nullable Integer childrenTotal,

        @Schema(description = "trace.field.step.duration.desc")
        @Nullable Double durationMillis,

        @Schema(description = "trace.field.step.self.desc")
        @Nullable Double selfMillis
) {
}
