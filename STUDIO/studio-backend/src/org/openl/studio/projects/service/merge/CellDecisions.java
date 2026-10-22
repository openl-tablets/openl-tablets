package org.openl.studio.projects.service.merge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.openl.rules.xls.merge.diff.CellResolution;
import org.openl.rules.xls.merge.diff.CellResolutionException;
import org.openl.rules.xls.merge.diff.Resolution;
import org.openl.rules.xls.merge.diff.TableCell;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.model.merge.ConflictCellDecision;
import org.openl.studio.projects.model.merge.ConflictCellsDecision;

/**
 * Reads how a client merges a conflicting table cell by cell, and tells why the merge refuses it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class CellDecisions {

    /**
     * How a table is resolved cell by cell, as a client decided it. A message of a refusal names the table, the
     * workbook, and the row and the column of the cell it is about.
     *
     * @param path     the conflicted workbook
     * @param id       the identifier of the table conflict
     * @param decision the decision of the client
     * @throws BadRequestException when a cell decision gives neither a version nor a value, a cell is decided twice, or
     *                             the base version is decided for a cell
     */
    static CellResolution resolutionOf(String path, String id, ConflictCellsDecision decision) {
        var sides = new HashMap<TableCell, Resolution>();
        var values = new HashMap<TableCell, String>();
        var decided = new HashMap<TableCell, ConflictCellDecision>();
        for (ConflictCellDecision cell : Objects.requireNonNullElse(decision.cells(), List.<ConflictCellDecision>of())) {
            var place = new TableCell(cell.row(), cell.column());
            var side = cell.side();
            var value = cell.value();
            if (side == null && value == null) {
                throw new BadRequestException("project.merge.conflict.workbook.cell.empty.message",
                        new Object[]{id, path, cell.row(), cell.column()});
            } else if (decided.put(place, cell) != null) {
                throw new BadRequestException("project.merge.conflict.workbook.cell.duplicate.message",
                        new Object[]{id, path, cell.row(), cell.column()});
            }
            if (side != null) {
                sides.put(place, sideOf(path, id, side));
            }
            if (value != null) {
                values.put(place, value);
            }
        }
        var whole = decision.side();
        var table = whole == null ? null : sideOf(path, id, whole);
        return new CellResolution(table, sides, values);
    }

    /**
     * The version a conflicting cell takes: your or their version only.
     */
    private static Resolution sideOf(String path, String id, ConflictBase side) {
        return switch (side) {
            case OURS -> Resolution.OURS;
            case THEIRS -> Resolution.THEIRS;
            case BASE -> throw new BadRequestException("project.merge.conflict.workbook.cell.base.message",
                    new Object[]{id, path});
        };
    }

    /**
     * Tells why a table cannot be merged cell by cell as decided. The message names the table, the workbook, and the
     * row and the column of the cell.
     *
     * @param path the conflicted workbook
     */
    static BadRequestException refusalOf(String path, CellResolutionException e) {
        var key = switch (e.getKind()) {
            case OUTSIDE -> "project.merge.conflict.workbook.cell.outside.message";
            case COVERED -> "project.merge.conflict.workbook.cell.covered.message";
            case FORMULA -> "project.merge.conflict.workbook.cell.formula.message";
        };
        return new BadRequestException(key, new Object[]{e.getConflict(), path, e.getRow(), e.getColumn()});
    }

    /**
     * The resolutions of the tables a client merges cell by cell, by the identifiers of their conflicts.
     */
    static Map<String, CellResolution> resolutionsOf(String path, Map<String, ConflictCellsDecision> byCells) {
        var resolutions = new HashMap<String, CellResolution>();
        byCells.forEach((id, decision) -> resolutions.put(id, resolutionOf(path, id, decision)));
        return resolutions;
    }
}
