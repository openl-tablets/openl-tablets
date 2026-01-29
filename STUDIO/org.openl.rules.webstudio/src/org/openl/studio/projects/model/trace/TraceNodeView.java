package org.openl.studio.projects.model.trace;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.rules.rest.compile.MessageDescription;
import org.openl.studio.common.model.GenericView;

/**
 * Representation of a trace node for JSON response.
 */
@Schema(description = "trace.field.node.title.desc")
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
        List<TraceParameterValue> parameters,

        @Schema(description = "trace.field.node.context.desc")
        @JsonView(GenericView.Full.class)
        TraceParameterValue context,

        @Schema(description = "trace.field.node.result.desc")
        @JsonView(GenericView.Full.class)
        TraceParameterValue result,

        @Schema(description = "trace.field.node.errors.desc")
        @JsonView(GenericView.Full.class)
        List<MessageDescription> errors
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int key;
        private String title;
        private String tooltip;
        private String type;
        private boolean lazy;
        private String extraClasses;
        private boolean error;
        private List<TraceParameterValue> parameters;
        private TraceParameterValue context;
        private TraceParameterValue result;
        private List<MessageDescription> errors;

        private Builder() {
        }

        public Builder key(int key) {
            this.key = key;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder tooltip(String tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder lazy(boolean lazy) {
            this.lazy = lazy;
            return this;
        }

        public Builder extraClasses(String extraClasses) {
            this.extraClasses = extraClasses;
            return this;
        }

        public Builder error(boolean error) {
            this.error = error;
            return this;
        }

        public Builder parameters(List<TraceParameterValue> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder context(TraceParameterValue context) {
            this.context = context;
            return this;
        }

        public Builder result(TraceParameterValue result) {
            this.result = result;
            return this;
        }

        public Builder errors(List<MessageDescription> errors) {
            this.errors = errors;
            return this;
        }

        public TraceNodeView build() {
            return new TraceNodeView(key,
                    title,
                    tooltip,
                    type,
                    lazy,
                    extraClasses,
                    error,
                    parameters,
                    context,
                    result,
                    errors);
        }
    }
}
