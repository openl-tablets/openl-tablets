package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.properties.inherit.InheritanceLevel;

/**
 * Where a property that applies to a table is defined.
 *
 * <p>Only the levels a table inherits from are named here; a property written on the table itself carries no
 * level at all.
 */
@Schema(description = "Level a table property is inherited from")
public enum PropertyInheritance {

    @Schema(description = "Defined by the properties table of the table's category")
    @JsonProperty("category")
    CATEGORY,

    @Schema(description = "Defined by the properties table of the module")
    @JsonProperty("module")
    MODULE,

    @Schema(description = "Defined outside the project, by what runs it")
    @JsonProperty("external")
    EXTERNAL;

    /**
     * The level as this API names it, or nothing when the property is the table's own.
     *
     * <p>A property written on the table, and one the engine reports no level for, are both the table's own as
     * far as a reader is concerned — which is how the Table Details panel has always shown them.
     */
    public static @Nullable PropertyInheritance of(@Nullable InheritanceLevel level) {
        if (level == null) {
            return null;
        }
        return switch (level) {
            case CATEGORY -> CATEGORY;
            case MODULE -> MODULE;
            case EXTERNAL -> EXTERNAL;
            default -> null;
        };
    }
}
