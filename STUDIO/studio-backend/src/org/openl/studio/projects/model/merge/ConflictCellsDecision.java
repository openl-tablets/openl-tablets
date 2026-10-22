package org.openl.studio.projects.model.merge;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * How a conflicting table is merged cell by cell. The cells only one branch changed, or both changed alike, are merged
 * by themselves. Each cell both branches changed differently takes its value from the version decided for the cell or
 * for the whole table.
 *
 * <p>A decision for a cell overrides the one for the table. A value typed for a cell overrides the value of the version
 * decided for it.
 *
 * @param side  the version every conflicting cell of the table takes, or {@code null} when none is decided for it
 * @param cells the decisions for single cells, or {@code null} for none
 */
@Schema(description = """
        How a conflicting table is merged cell by cell: the version each cell both branches changed \
        differently takes, and the values typed for cells""")
public record ConflictCellsDecision(
        @Parameter(description = """
                The version every conflicting cell of the table takes, `ours` or `theirs`, unless \
                the cell itself is decided otherwise""")
        @Nullable ConflictBase side,

        @Parameter(description = """
                The decisions for single cells of the merged table: the version a conflicting cell \
                takes, and the value typed for any cell""")
        @Nullable List<@Valid @NotNull ConflictCellDecision> cells
) {
}
