package org.openl.studio.projects.model.tables;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import org.jspecify.annotations.Nullable;

/**
 * Lightweight table description for the copy dialog: the table's name, kind and its own properties, without the body.
 * <p>
 * The copy dialog uses this to prefill the copy's name and properties. It carries none of the table's rows, so a
 * table of any size is read cheaply.
 *
 * @param name       name of the table
 * @param kind       kind of the table object
 * @param properties properties defined on the table
 * @param versions   versions of the table, absent for a kind of table that carries none
 * @author Vladyslav Pikus
 */
public record TablePropertiesView(
        @Parameter(description = "Name of the table")
        String name,

        @Parameter(description = "Kind of the table object")
        TableKind kind,

        @Parameter(description = "Properties defined on the table")
        List<TableProperty> properties,

        @Parameter(description = "Versions of the table, absent for a kind of table that carries none")
        @Nullable TableVersionsView versions
) {
}
