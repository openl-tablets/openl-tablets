package org.openl.studio.projects.model.trace;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import org.openl.rules.rest.compile.MessageDescription;
import org.openl.studio.common.model.GenericView;
import org.openl.studio.projects.model.ParameterValue;

/**
 * Representation of a trace node for JSON response.
 */
@Builder
@Schema(description = "trace.type.node-view.desc")
public record TraceNodeView(
        @Schema(description = "trace.field.node.key.desc")
        @JsonView({GenericView.Short.class, GenericView.Full.class})
        int key,

        @Schema(description = "trace.field.node.title.desc")
        @JsonView({GenericView.Short.class, GenericView.Full.class})
        String title,

        @Schema(description = "trace.field.node.tooltip.desc")
        @JsonView({GenericView.Short.class, GenericView.Full.class})
        String tooltip,

        @Schema(description = "trace.field.node.type.desc")
        @JsonView({GenericView.Short.class, GenericView.Full.class})
        String type,

        @Schema(description = "trace.field.node.lazy.desc")
        @JsonView({GenericView.Short.class, GenericView.Full.class})
        boolean lazy,

        @Schema(description = "trace.field.node.extra-classes.desc")
        @JsonView({GenericView.Short.class, GenericView.Full.class})
        String extraClasses,

        @Schema(description = "trace.field.node.error.desc")
        @JsonView({GenericView.Short.class, GenericView.Full.class})
        boolean error,

        @Schema(description = "trace.field.node.parameters.desc")
        @JsonView(GenericView.Full.class)
        List<ParameterValue> parameters,

        @Schema(description = "trace.field.node.context.desc")
        @JsonView(GenericView.Full.class)
        ParameterValue context,

        @Schema(description = "trace.field.node.result.desc")
        @JsonView(GenericView.Full.class)
        ParameterValue result,

        @Schema(description = "trace.field.node.errors.desc")
        @JsonView(GenericView.Full.class)
        List<MessageDescription> errors
) {
}
