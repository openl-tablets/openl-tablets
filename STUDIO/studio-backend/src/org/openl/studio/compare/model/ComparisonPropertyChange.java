package org.openl.studio.compare.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * A property of a table that reads differently in the two files, for example its name, its position
 * or its size.
 *
 * @param property name of the property
 * @param first    value in the first file
 * @param second   value in the second file
 */
public record ComparisonPropertyChange(

        @Schema(description = "Name of the property, for example 'name', 'location' or 'size'")
        String property,

        @Schema(description = "Value in the first file")
        @Nullable String first,

        @Schema(description = "Value in the second file")
        @Nullable String second) {
}
