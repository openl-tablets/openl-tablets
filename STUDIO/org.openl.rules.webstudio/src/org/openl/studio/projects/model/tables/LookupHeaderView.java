package org.openl.studio.projects.model.tables;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Header model for {@link LookupView} table (SmartLookup or SimpleLookup).
 * <p>
 * Represents a single header in the table structure which may have child headers to support
 * hierarchical column grouping. Headers with children represent grouped columns, while
 * headers without children represent actual data columns (leaf headers).
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = LookupHeaderView.Builder.class)
public class LookupHeaderView extends ARuleHeaderView {

    /**
     * Child headers for hierarchical header structures (e.g., sub-columns under a header).
     * Empty list means this is a leaf header that directly maps to data values.
     * <p>
     * For example, if a header "AccidentOnly" has children ["Purebred", "MixedBreed"],
     * then the data will have nested structure:
     * <pre>
     * "AccidentOnly": {
     *   "Purebred": value1,
     *   "MixedBreed": value2
     * }
     * </pre>
     */
    @Schema(description = "Child headers for hierarchical header structures")
    public final List<LookupHeaderView> children;

    private LookupHeaderView(Builder builder) {
        super(builder);
        this.children = Optional.ofNullable(builder.children).map(List::copyOf).orElse(List.of());
    }

    @JsonIgnore
    public int getWidth() {
        if (children.isEmpty()) {
            return 1;
        }
        return children.stream()
                .mapToInt(LookupHeaderView::getWidth)
                .sum();
    }

    @JsonIgnore
    public int getHeight() {
        if (children.isEmpty()) {
            return 1;
        }
        return 1 + children.stream()
                .mapToInt(LookupHeaderView::getHeight)
                .max()
                .orElse(0);
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ARuleHeaderView.Builder<Builder> {

        private List<LookupHeaderView> children;

        public Builder() {
        }

        public Builder children(List<LookupHeaderView> children) {
            this.children = children;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public LookupHeaderView build() {
            return new LookupHeaderView(this);
        }
    }

}
