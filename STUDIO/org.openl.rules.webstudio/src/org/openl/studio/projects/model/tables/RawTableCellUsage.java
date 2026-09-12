package org.openl.studio.projects.model.tables;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * One piece of a cell's text that the compiler resolved to something.
 *
 * <p>The range is over the cell's value as text, so a screen can mark exactly the characters the compiler
 * matched — a rule called in a formula, a field of a datatype, the name of a type — and say what they stand
 * for. A usage that stands for a table also carries that table's identifier, so following it needs no lookup.
 *
 * @param start       index of the first character the usage covers
 * @param end         index after the last character it covers
 * @param description what the compiler says about it, as the Editor shows in a tooltip
 * @param tableId     table the usage refers to, absent when it refers to no table of its own
 * @param module      module that table is read through, absent when no module of the workspace holds it
 * @param projectId   project that module belongs to, absent for the same reasons the module is
 * @param kind        what the piece of text stands for
 * @author Vladyslav Pikus
 */
@Builder
public record RawTableCellUsage(
        @Parameter(description = "Index of the first character of the cell's text the usage covers")
        int start,

        @Parameter(description = "Index after the last character of the cell's text the usage covers")
        int end,

        @Parameter(description = "What the compiler says about the usage, shown as a tooltip")
        String description,

        @Parameter(description = """
                Identifier of the table the usage refers to, the one the Tables API addresses it by. \
                Absent for a usage that refers to no table of its own.""")
        @Nullable String tableId,

        @Parameter(description = """
                Module the table the usage refers to is read through, so a reader can be sent to it. \
                Absent when the usage refers to no table, or to one no module of the workspace holds.""")
        @Nullable String module,

        @Parameter(description = """
                Identifier of the project the module belongs to, which for a table of a dependency is not \
                the project being read. Absent for the same reasons the module is.""")
        @Nullable String projectId,

        @Parameter(description = "What the covered text stands for")
        RawTableUsageKind kind
) {
}
