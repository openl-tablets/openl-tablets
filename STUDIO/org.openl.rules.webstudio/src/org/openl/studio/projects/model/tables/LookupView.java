package org.openl.studio.projects.model.tables;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * View model for SmartLookup and SimpleLookup tables.
 * <p>
 * Both SmartLookup and SimpleLookup tables are used to perform lookups based on dimension parameters,
 * returning a result from a multi-dimensional array. The table structure consists of:
 * <ul>
 *   <li><b>Headers:</b> Hierarchical header structure where headers can have child headers.
 *     The first level contains dimension parameters and result column headers (which may have children).
 *     For SmartLookup: [Species(rowspan=2), Breed(rowspan=2), AccidentOnly[Purebred, MixedBreed], AccidentIllness[Purebred, MixedBreed]]
 *     For SimpleLookup: Similar structure but with simpler hierarchy.
 *   <li><b>Body rows:</b> Data rows with hierarchical structure matching header hierarchy.
 *     Leaf headers map to their values, parent headers map to nested LinkedHashMaps.
 * </ul>
 * <p>
 * The difference between SmartLookup and SimpleLookup is determined by the {@link #tableType} field:
 * <ul>
 *   <li>{@value #SMART_TABLE_TYPE}: SmartLookup table with multi-dimensional hierarchy support
 *   <li>{@value #SIMPLE_TABLE_TYPE}: SimpleLookup table with simpler structure
 * </ul>
 * <p>
 * Example SmartLookup table:
 * <pre>
 * SmartLookup Double PrimaryBreedFactor (Species, Breed, CoverageType, BreedPurity)
 *   Species | Breed            | AccidentOnly      | AccidentIllness
 *           |                  | Purebred | Mixed  | Purebred | Mixed
 *   --------|------------------|----------|--------|----------|--------
 *   Dog     | Affenpinscher    | 1.04     | 1.04   | 0.80     | 0.80
 *   Dog     | AfghanHound      | 1.34     | 1.34   | 1.53     | 1.53
 * </pre>
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = LookupView.Builder.class)
public class LookupView extends ExecutableView {

    public static final String SMART_TABLE_TYPE = "SmartLookup";
    public static final String SIMPLE_TABLE_TYPE = "SimpleLookup";

    /**
     * Headers in hierarchical structure. First-level headers represent dimensions and result columns.
     * Result column headers may have child headers for multi-level column grouping.
     * <p>
     * For SmartLookup: Can have multiple levels of nesting for complex multi-dimensional structures.
     * For SimpleLookup: Typically has a simpler hierarchy structure.
     */
    @Schema(description = "Headers in hierarchical structure representing dimensions and result columns")
    public final List<LookupHeaderView> headers;

    /**
     * Data rows with hierarchical structure. Each row is a LinkedHashMap where:
     * <ul>
     *   <li>Leaf headers map to their actual values (Object)</li>
     *   <li>Parent headers map to LinkedHashMap containing nested child values</li>
     * </ul>
     * <p>
     * The structure matches the header hierarchy defined in {@link #headers}.
     * <p>
     * Example row matching header hierarchy:
     * <pre>
     * {
     *   "Species": "Dog",
     *   "Breed": "Affenpinscher",
     *   "AccidentOnly": {
     *     "Purebred": "1.04",
     *     "MixedBreed": "1.04"
     *   },
     *   "AccidentIllness": {
     *     "Purebred": "0.80",
     *     "MixedBreed": "0.80"
     *   }
     * }
     * </pre>
     */
    @Schema(description = "Data rows with hierarchical structure matching the header hierarchy")
    public final List<LinkedHashMap<String, Object>> rows;

    public LookupView(Builder builder) {
        super(builder);
        this.headers = Optional.ofNullable(builder.headers).map(List::copyOf).orElse(List.of());
        this.rows = Optional.ofNullable(builder.rows).map(List::copyOf).orElse(List.of());
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ExecutableView.Builder<Builder> {

        private List<LookupHeaderView> headers;
        private List<LinkedHashMap<String, Object>> rows;

        public Builder() {
        }

        public Builder headers(List<LookupHeaderView> headers) {
            this.headers = headers;
            return this;
        }

        public Builder rows(List<LinkedHashMap<String, Object>> rows) {
            this.rows = rows;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public LookupView build() {
            return new LookupView(this);
        }
    }
}
