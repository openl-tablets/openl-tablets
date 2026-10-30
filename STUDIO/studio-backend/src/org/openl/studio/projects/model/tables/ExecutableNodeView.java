package org.openl.studio.projects.model.tables;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * A graph node that stands for a callable table — a rules table, a spreadsheet, a method, or the synthetic dispatcher
 * that selects one version of an overloaded method at runtime.
 *
 * <p>Adds the calling contract of the table and, for a version of an overloaded method, the values of the dimensions
 * it is selected by. Its dependencies are the tables it calls.
 *
 * @author Vladyslav Pikus
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "A graph node of a callable table")
public final class ExecutableNodeView extends TableNodeView {

    @Schema(description = "Return type of the table (e.g., Integer, String, etc.)")
    public final String returnType;

    @Schema(description = "Signature of the table")
    public final String signature;

    @Schema(description = """
            Values of the dimension properties this version of the table is selected by, inherited ones included, \
            by the technical name of the property, as the table list answers them. Absent unless the table has \
            more than one version""")
    public final @Nullable Map<String, Object> dimensions;

    private ExecutableNodeView(Builder builder) {
        super(builder);
        this.returnType = builder.returnType;
        this.signature = builder.signature;
        this.dimensions = builder.dimensions;
    }

    public static Builder builder() {
        return new Builder();
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Builder extends TableNodeView.Builder<Builder> {
        private String returnType;
        private String signature;
        private @Nullable Map<String, Object> dimensions;

        /**
         * Copies the shared summary fields plus the calling contract and the dimensions of a version, which only a
         * callable table has.
         */
        @Override
        public Builder summary(SummaryTableView source) {
            super.summary(source);
            this.returnType = source.returnType;
            this.signature = source.signature;
            this.dimensions = source.dimensions;
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
