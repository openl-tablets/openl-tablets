package org.openl.studio.projects.model.tables;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A graph node that stands for a callable table — a rules table, a spreadsheet, a method, or the synthetic dispatcher
 * that selects one version of an overloaded method at runtime.
 *
 * <p>Adds the calling contract of the table and, for a version of an overloaded method, the dimensions it is selected
 * by. Its dependencies are the tables it calls.
 *
 * @author Vladyslav Pikus
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "A graph node of a callable table")
public class ExecutableNodeView extends TableNodeView {

    @Schema(description = "Return type of the table (e.g., Integer, String, etc.)")
    public final String returnType;

    @Schema(description = "Signature of the table")
    public final String signature;

    @Schema(description = """
            Dimension properties this version of the table is selected by — the versioning rules the \
            dispatcher uses (e.g. state, lob, dates), resolved from the module name pattern or the table itself""")
    public final Map<String, String> dimensionProperties;

    private ExecutableNodeView(Builder builder) {
        super(builder);
        this.returnType = builder.returnType;
        this.signature = builder.signature;
        this.dimensionProperties = builder.dimensionProperties;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends TableNodeView.Builder<Builder> {
        private String returnType;
        private String signature;
        private Map<String, String> dimensionProperties;

        private Builder() {
        }

        /** Copies the shared summary fields plus the calling contract, which only a callable table has. */
        @Override
        public Builder summary(SummaryTableView source) {
            super.summary(source);
            this.returnType = source.returnType;
            this.signature = source.signature;
            return this;
        }

        public Builder dimensionProperties(Map<String, String> dimensionProperties) {
            this.dimensionProperties = dimensionProperties;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public ExecutableNodeView build() {
            return new ExecutableNodeView(this);
        }
    }
}
