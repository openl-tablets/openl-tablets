package org.openl.studio.projects.model.tables;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * One property that applies to a table, as the Table Details panel shows it.
 *
 * <p>A property is either written on the table itself or inherited from the properties table of its module or
 * its category, and the answer says which — a reader looking at a table whose header is hidden has no other way
 * of telling where a value comes from.
 *
 * @param name             name the property is known by in the property dictionary
 * @param displayName      name as it is shown to a reader
 * @param value            value the property carries — a date in ISO-8601, a list of values comma separated
 * @param inheritedFrom    level the value is defined on, absent when it is written on the table itself
 * @param inheritedTableId id of the properties table the value comes from, when it is inherited from one
 * @author Vladyslav Pikus
 */
@Builder
public record TablePropertyDetailView(
        @Parameter(description = "Name of the property in the property dictionary")
        String name,

        @Parameter(description = "Name of the property as it is shown to a reader")
        String displayName,

        @Parameter(description = "Value the property carries: a date in ISO-8601, a list of values comma separated")
        String value,

        @Parameter(description = """
                Level the value is defined on. Absent when the property is written on the table itself; \
                a module or category property is inherited by every table that does not override it.""")
        @Nullable PropertyInheritance inheritedFrom,

        @Parameter(description = """
                Identifier of the properties table the value comes from, so a reader can open it. \
                Absent when the property is written on the table itself.""")
        @Nullable String inheritedTableId
) {
}
