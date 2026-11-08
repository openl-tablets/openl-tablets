package org.openl.studio.common.model;

import java.util.Comparator;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The direction a list is sorted in: from the least to the greatest, or back.
 *
 * <p>An API names the field it sorts by on its own; this one only tells the direction.
 */
@Schema(description = "The direction a list is sorted in")
public enum SortOrder {

    @Schema(description = "From the least to the greatest.")
    @JsonProperty("asc")
    ASC,

    @Schema(description = "From the greatest to the least.")
    @JsonProperty("desc")
    DESC;

    /**
     * The comparator that sorts in this direction.
     *
     * @param ascending the comparator from the least to the greatest
     * @return the comparator given, or the reversed one for {@link #DESC}
     */
    public <T> Comparator<T> apply(Comparator<T> ascending) {
        return this == ASC ? ascending : ascending.reversed();
    }
}
