package org.openl.studio.projects.model.tables;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A graph node that stands for a datatype table — a datatype or a vocabulary.
 *
 * <p>Adds the data model around the datatype: the datatype it extends and the fields it declares. Its dependencies are
 * the datatypes it is built from, so they always hold the parent and every field type that is itself a datatype of
 * this graph.
 *
 * @author Vladyslav Pikus
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "A graph node of a datatype table")
public class DatatypeNodeView extends TableNodeView {

    @Schema(description = "Identifier of the datatype this one extends, absent when it extends nothing of this graph")
    @JsonProperty("extends")
    public final String extendz;

    @Schema(description = """
            Fields the datatype declares itself, in the order the table declares them; the inherited ones belong to \
            the parent node""")
    public final List<DatatypeNodeFieldView> fields;

    private DatatypeNodeView(Builder builder) {
        super(builder);
        this.extendz = builder.extendz;
        this.fields = builder.fields;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends TableNodeView.Builder<Builder> {
        private String extendz;
        private List<DatatypeNodeFieldView> fields;

        private Builder() {
        }

        public Builder extendz(String extendz) {
            this.extendz = extendz;
            return this;
        }

        public Builder fields(List<DatatypeNodeFieldView> fields) {
            this.fields = fields;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public DatatypeNodeView build() {
            return new DatatypeNodeView(this);
        }
    }
}
