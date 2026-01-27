package org.openl.studio.projects.model.trace;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representation of a trace node for JSON response.
 */
@Schema(description = "Trace node view")
public record TraceNodeView(
        @Schema(description = "Unique key for this node, used for lazy loading children")
        int key,

        @Schema(description = "Display title of the node")
        String title,

        @Schema(description = "Tooltip text for the node")
        String tooltip,

        @Schema(description = "Type of the node (e.g., 'rule', 'condition', 'spreadsheet')")
        String type,

        @Schema(description = "If true, this node has children that can be loaded lazily")
        boolean lazy,

        @Schema(description = "CSS classes for styling (e.g., 'rule', 'condition result', 'condition fail')")
        String extraClasses,

        @Schema(description = "Input parameters for this trace node")
        List<TraceParameterValue> parameters,

        @Schema(description = "Runtime context")
        TraceParameterValue context,

        @Schema(description = "Return result of the traced method")
        TraceParameterValue result
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
        private List<TraceParameterValue> parameters;
        private TraceParameterValue context;
        private TraceParameterValue result;

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

        public TraceNodeView build() {
            return new TraceNodeView(key, title, tooltip, type, lazy, extraClasses, parameters, context, result);
        }
    }
}
