package org.openl.studio.projects.service.merge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.rules.xls.merge.diff.CellResolution;
import org.openl.rules.xls.merge.diff.CellResolutionException;
import org.openl.rules.xls.merge.diff.Resolution;
import org.openl.rules.xls.merge.diff.TableCell;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.model.merge.ConflictCellDecision;
import org.openl.studio.projects.model.merge.ConflictCellsDecision;

class CellDecisionsTest {

    private static final String PATH = "rules/Rates.xlsx";
    private static final String TABLE = "Rules/A1:B3";

    @Test
    void readsTheDecisionsOfATable() {
        var resolution = CellDecisions.resolutionOf(PATH, TABLE, new ConflictCellsDecision(ConflictBase.THEIRS,
                List.of(new ConflictCellDecision(1, 1, ConflictBase.THEIRS, null),
                        new ConflictCellDecision(2, 0, ConflictBase.OURS, "typed"),
                        new ConflictCellDecision(2, 1, null, ""))));

        assertEquals(new CellResolution(Resolution.THEIRS,
                Map.of(new TableCell(1, 1), Resolution.THEIRS, new TableCell(2, 0), Resolution.OURS),
                Map.of(new TableCell(2, 0), "typed", new TableCell(2, 1), "")), resolution);
    }

    @Test
    void readsATableWithNothingDecided() {
        assertEquals(new CellResolution(null, Map.of(), Map.of()),
                CellDecisions.resolutionsOf(PATH, Map.of(TABLE, new ConflictCellsDecision(null, null)))
                        .get(TABLE));
    }

    @Test
    void refusesACellDecidedWithNothing() {
        var decision = new ConflictCellsDecision(null, List.of(new ConflictCellDecision(1, 2, null, null)));

        var exception = assertThrows(BadRequestException.class,
                () -> CellDecisions.resolutionOf(PATH, TABLE, decision));

        assertEquals("openl.error.400.project.merge.conflict.workbook.cell.empty.message", exception.getErrorCode());
        assertArrayEquals(new Object[]{TABLE, PATH, 1, 2}, exception.getArgs());
    }

    @Test
    void refusesACellDecidedTwice() {
        var decision = new ConflictCellsDecision(null, List.of(
                new ConflictCellDecision(1, 1, ConflictBase.OURS, null),
                new ConflictCellDecision(1, 1, null, "2")));

        var exception = assertThrows(BadRequestException.class,
                () -> CellDecisions.resolutionOf(PATH, TABLE, decision));

        assertEquals("openl.error.400.project.merge.conflict.workbook.cell.duplicate.message",
                exception.getErrorCode());
        assertArrayEquals(new Object[]{TABLE, PATH, 1, 1}, exception.getArgs());
    }

    @Test
    void refusesTheBaseVersionForCells() {
        for (var decision : List.of(new ConflictCellsDecision(ConflictBase.BASE, null),
                new ConflictCellsDecision(null, List.of(new ConflictCellDecision(0, 0, ConflictBase.BASE, null))))) {
            var exception = assertThrows(BadRequestException.class,
                    () -> CellDecisions.resolutionOf(PATH, TABLE, decision));

            assertEquals("openl.error.400.project.merge.conflict.workbook.cell.base.message",
                    exception.getErrorCode());
            assertArrayEquals(new Object[]{TABLE, PATH}, exception.getArgs());
        }
    }

    @Test
    void tellsWhatTheMergedTableCannotTake() {
        assertRefusal("openl.error.400.project.merge.conflict.workbook.cell.outside.message",
                CellResolutionException.Kind.OUTSIDE, 4, 1);
        assertRefusal("openl.error.400.project.merge.conflict.workbook.cell.covered.message",
                CellResolutionException.Kind.COVERED, 0, 1);
        assertRefusal("openl.error.400.project.merge.conflict.workbook.cell.formula.message",
                CellResolutionException.Kind.FORMULA, 2, 1);
    }

    private static void assertRefusal(String code, CellResolutionException.Kind kind, int row, int column) {
        var refusal = CellDecisions.refusalOf(PATH, new CellResolutionException(TABLE, kind, row, column));

        assertEquals(code, refusal.getErrorCode());
        assertArrayEquals(new Object[]{TABLE, PATH, row, column}, refusal.getArgs());
    }
}
