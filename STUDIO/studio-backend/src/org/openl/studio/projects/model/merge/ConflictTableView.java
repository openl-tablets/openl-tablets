package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * A table both branches changed in ways the merge cannot combine. It is resolved by taking it whole from one version,
 * or, when it tells how it merges cell by cell, by deciding the version of each cell both branches changed
 * differently.
 *
 * <p>A version that removed the table has none, and so has the base version of a table both branches added.
 *
 * @param id     identifies the conflict in a resolution
 * @param name   the text of the first cell of the table, its header, or its place when the cell is empty
 * @param reason why the changes of the table cannot be combined
 * @param ours   the table in your version, or {@code null} when your version has none
 * @param base   the table in the base version, or {@code null} when it has none
 * @param theirs the table in their version, or {@code null} when their version has none
 * @param merged the table merged cell by cell, or {@code null} when it is resolved only as a whole
 */
@Schema(description = "A table both branches changed in ways the merge cannot combine")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConflictTableView(
        @Parameter(description = "Identifies the conflict in a resolution")
        String id,

        @Parameter(description = """
                The text of the first cell of the table, its header, or its place when the cell is \
                empty""")
        String name,

        @Parameter(description = "Why the changes of the table cannot be combined")
        ConflictTableReason reason,

        @Parameter(description = "The table in your version; absent when your version has none")
        @Nullable ConflictTableVersion ours,

        @Parameter(description = "The table in the base version; absent when the branches added it")
        @Nullable ConflictTableVersion base,

        @Parameter(description = "The table in their version; absent when their version has none")
        @Nullable ConflictTableVersion theirs,

        @Parameter(description = """
                The table merged cell by cell, when only some of its cells conflict: the cells only \
                one branch changed merged, and the cells both changed differently left to decide. Absent for a \
                table resolved only as a whole""")
        @Nullable ConflictTableMerge merged
) {
}
