package org.openl.rules.xls.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.openl.rules.xls.merge.MergeFixture.RULES;
import static org.openl.rules.xls.merge.MergeFixture.cell;
import static org.openl.rules.xls.merge.MergeFixture.clear;
import static org.openl.rules.xls.merge.MergeFixture.move;
import static org.openl.rules.xls.merge.MergeFixture.number;
import static org.openl.rules.xls.merge.MergeFixture.set;
import static org.openl.rules.xls.merge.MergeFixture.text;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import org.openl.rules.xls.merge.diff.Resolution;
import org.openl.rules.xls.merge.diff.TableConflict;
import org.openl.rules.xls.merge.diff.TableConflictReason;

/**
 * Covers tables both revisions moved, one of them only pushing its tables up or down by rows it inserted or deleted
 * above them.
 */
class XlsTablePushTest {

    /** Three tables one under another: rules at A1, drivers at A6 and cars at A10. */
    private static final MergeFixture STACKED = new MergeFixture(XlsTablePushTest::stacked);
    /** The drivers and the cars under a table at A1, which both revisions remove. */
    private static final Consumer<Workbook> TOPPED_BASE = workbook -> {
        var sheet = workbook.createSheet(RULES);
        set(sheet, 0, 0, "Data Old olds");
        set(sheet, 1, 0, "name");
        set(sheet, 2, 0, "Old");
        drivers(sheet);
        cars(sheet);
    };
    private static final MergeFixture TOPPED = new MergeFixture(TOPPED_BASE);
    /** The stacked tables with a note at D3, beside the US row of the rules. */
    private static final MergeFixture NOTED = new MergeFixture(workbook -> {
        stacked(workbook);
        set(workbook.getSheet(RULES), 2, 3, "reviewed");
    });

    /** Inserts a row into the rules table the way Excel does: every row below moves down. */
    private static final Consumer<Sheet> GROW_RULES = sheet -> {
        move(sheet, "A4:F20", 1, 0);
        set(sheet, 3, 0, "MX");
        set(sheet, 3, 1, 3);
    };

    /**
     * Inserts two rows into the rules the way Excel does, one above the note beside them and one under it: the note
     * moves by one row, while the rules grow by two.
     */
    private static final Consumer<Sheet> GROW_AROUND_NOTE = sheet -> {
        move(sheet, "A3:F20", 1, 0);
        set(sheet, 2, 0, "MX");
        set(sheet, 2, 1, 3);
        move(sheet, "A6:F30", 1, 0);
        set(sheet, 5, 0, "UK");
        set(sheet, 5, 1, 4);
    };

    /** Inserts two empty rows above every table the way Excel does. */
    private static final Consumer<Sheet> PUSH_ALL = sheet -> move(sheet, "A1:F30", 2, 0);

    /** Moves the cars to E1 and the drivers three rows down: tables moved on purpose, out of their order. */
    private static final Consumer<Sheet> REARRANGE = sheet -> {
        move(sheet, "A10:B12", -9, 4);
        move(sheet, "A6:B8", 3, 0);
    };

    @Test
    void rowsOursInsertedAboveTheTables_pushTheTablesTheirsMoved() throws IOException {
        var merged = STACKED.mergeXlsx(GROW_RULES, REARRANGE.andThen(sheet -> {
            set(sheet, 13, 0, "Data Plan plans");
            set(sheet, 14, 0, "name");
        }));

        assertEquals("MX", text(merged, 3, 0));
        assertEquals("CA", text(merged, 4, 0));
        assertNull(cell(merged, 8, 0));
        assertEquals("Data Driver drivers", text(merged, 9, 0), "Where their branch put the table, one row down");
        assertEquals(30, number(merged, 11, 1));
        assertEquals("Data Car cars", text(merged, 0, 4), "Above the rules that grew, so not pushed");
        assertEquals("Data Plan plans", text(merged, 14, 0), "The table their branch created is pushed too");
    }

    @Test
    void rowsTheirsInsertedAboveTheTables_pushTheTablesOursMoved() throws IOException {
        var merged = STACKED.mergeXlsx(REARRANGE.andThen(sheet -> {
            set(sheet, 13, 0, "Data Plan plans");
            set(sheet, 14, 0, "name");
        }), GROW_RULES);

        assertEquals("MX", text(merged, 3, 0));
        assertNull(cell(merged, 8, 0));
        assertEquals("Data Driver drivers", text(merged, 9, 0), "Where our branch put the table, one row down");
        assertEquals("Data Car cars", text(merged, 0, 4));
        assertEquals("Data Plan plans", text(merged, 14, 0), "The table our branch created is pushed too");
    }

    @Test
    void tablesUnderOneBothRevisionsRemoved_followTheFirstOneThatMovedOnPurpose() throws Exception {
        Consumer<Sheet> our = sheet -> {
            clear(sheet, "A1:A3");
            move(sheet, "A5:B12", -4, 0);
        };
        Consumer<Sheet> their = sheet -> {
            clear(sheet, "A1:A3");
            move(sheet, "A6:B12", -5, 0);
        };

        var conflicts = TOPPED.conflicts(XSSFWorkbook::new, our, their).sheets().getFirst();
        assertEquals(List.of(new TableConflict(RULES + "/A6:B8",
                CellRangeAddress.valueOf("A2:B4"),
                CellRangeAddress.valueOf("A6:B8"),
                CellRangeAddress.valueOf("A1:B3"),
                TableConflictReason.MOVED)), conflicts.tables(), "The cars only follow the drivers");

        var merged = TOPPED.resolveXlsx(our, their, Map.of(RULES + "/A6:B8", Resolution.OURS)).getSheet(RULES);
        assertEquals("Data Driver drivers", text(merged, 1, 0));
        assertEquals("Data Car cars", text(merged, 5, 0));
    }

    @Test
    void noteBesideATable_movesWithIt() throws Exception {
        Consumer<Sheet> note = sheet -> set(sheet, 5, 3, "reviewed");
        var fixture = new MergeFixture(workbook -> {
            TOPPED_BASE.accept(workbook);
            note.accept(workbook.getSheet(RULES));
        });
        Consumer<Sheet> our = sheet -> {
            clear(sheet, "A1:A3");
            move(sheet, "A5:D12", -4, 0);
        };
        Consumer<Sheet> their = sheet -> {
            clear(sheet, "A1:A3");
            move(sheet, "A6:D12", -5, 0);
        };

        var tables = fixture.conflicts(XSSFWorkbook::new, our, their).sheets().getFirst().tables();
        assertEquals(List.of(RULES + "/A6:B8"), tables.stream().map(TableConflict::id).toList());
    }

    @Test
    void combinedPlacesThatDoNotFit_leaveTheTablesBothRevisionsMovedConflicts() throws IOException {
        // Our branch creates a table where their drivers land once pushed.
        Consumer<Sheet> our = GROW_RULES.andThen(sheet -> {
            set(sheet, 14, 0, "Data Pet pets");
            set(sheet, 15, 0, "name");
        });
        Consumer<Sheet> their = sheet -> {
            move(sheet, "A10:B12", -9, 4);
            move(sheet, "A6:B8", 7, 0);
        };

        var sheet = STACKED.conflicts(XSSFWorkbook::new, our, their).sheets().getFirst();
        assertNull(sheet.reason());
        assertFalse(sheet.tables().isEmpty());
        sheet.tables().forEach(table -> assertEquals(TableConflictReason.MOVED, table.reason()));
    }

    @Test
    void rowsBothInsertedAlike_pushTheTablesBelowOnce() throws IOException {
        // Our branch changes the rate of the US too, so the rules differ beyond the row both branches inserted.
        var merged = STACKED.mergeXlsx(GROW_RULES.andThen(PUSH_ALL).andThen(sheet -> set(sheet, 4, 1, 10)),
                GROW_RULES);

        assertEquals(10, number(merged, 4, 1));
        assertEquals("MX", text(merged, 5, 0));
        assertNull(cell(merged, 7, 0));
        assertEquals("Data Driver drivers", text(merged, 8, 0), "Where our branch put the table");
        assertEquals("Data Car cars", text(merged, 12, 0));
    }

    @Test
    void rowsBothInsertedAtOnePlace_pushTheTablesBelowByTheRowsKept() throws IOException {
        // Our branch inserts MX and FR into the rules the way Excel does; their branch inserts MX into them in place,
        // and moves the drivers and the cars on purpose.
        Consumer<Sheet> our = sheet -> {
            move(sheet, "A4:F20", 2, 0);
            set(sheet, 3, 0, "MX");
            set(sheet, 3, 1, 3);
            set(sheet, 4, 0, "FR");
            set(sheet, 4, 1, 4);
        };
        Consumer<Sheet> their = REARRANGE.andThen(sheet -> {
            move(sheet, "A4:B4", 1, 0);
            set(sheet, 3, 0, "MX");
            set(sheet, 3, 1, 3);
        });

        var merged = STACKED.mergeXlsx(our, their);

        assertEquals("MX", text(merged, 3, 0));
        assertEquals("FR", text(merged, 4, 0));
        assertEquals("CA", text(merged, 5, 0));
        assertNull(cell(merged, 8, 0));
        assertEquals("Data Driver drivers", text(merged, 9, 0), "Where their branch put the table, one row down");
        assertEquals("Data Car cars", text(merged, 0, 4));
    }

    @Test
    void noteBesideATableBothGrewAlike_staysBesideItsRow() throws IOException {
        // Our branch changes the rate of the CA too, so the rules differ beyond the rows both branches inserted.
        var merged = NOTED.mergeXlsx(GROW_AROUND_NOTE.andThen(PUSH_ALL).andThen(sheet -> set(sheet, 6, 1, 20)),
                GROW_AROUND_NOTE);

        assertEquals("US", text(merged, 5, 0));
        assertEquals("reviewed", text(merged, 5, 3));
        assertEquals(20, number(merged, 6, 1));
        assertEquals("UK", text(merged, 7, 0));
        assertEquals("Data Driver drivers", text(merged, 9, 0));
    }

    @Test
    void noteBesideATableBothGrewDifferently_isConflictWhenBothMovedIt() throws IOException {
        // Their branch inserts another row above the note.
        Consumer<Sheet> their = sheet -> {
            move(sheet, "A2:F30", 1, 0);
            set(sheet, 1, 0, "BR");
            set(sheet, 1, 1, 5);
            move(sheet, "A6:F30", 1, 0);
            set(sheet, 5, 0, "UK");
            set(sheet, 5, 1, 4);
        };

        var tables = NOTED.conflicts(XSSFWorkbook::new, GROW_AROUND_NOTE.andThen(PUSH_ALL), their)
                .sheets()
                .getFirst()
                .tables();
        assertEquals(List.of(RULES + "/D3"), tables.stream().map(TableConflict::id).toList());
        assertEquals(TableConflictReason.MOVED, tables.getFirst().reason());
    }

    @Test
    void tableBothCreatedAtDifferentPlaces_isKeptOnceWhereOursPutIt() throws IOException {
        // Our branch creates the plans above every table, which pushes them down; their branch creates them under the
        // cars, where our cars land.
        var merged = STACKED.mergeXlsx(GROW_RULES.andThen(sheet -> {
            move(sheet, "A1:F30", 3, 0);
            plans(sheet, 0);
        }), GROW_RULES.andThen(sheet -> plans(sheet, 14)));

        assertEquals("Data Plan plans", text(merged, 0, 0));
        assertEquals("Data Driver drivers", text(merged, 9, 0));
        assertEquals("Data Car cars", text(merged, 13, 0));
        assertEquals("model", text(merged, 14, 0), "Their plans are left out");
        assertNull(cell(merged, 16, 0));
    }

    private static void plans(Sheet sheet, int row) {
        set(sheet, row, 0, "Data Plan plans");
        set(sheet, row + 1, 0, "name");
    }

    private static void stacked(Workbook workbook) {
        var sheet = workbook.createSheet(RULES);
        set(sheet, 0, 0, "Rules String rate(String country)");
        set(sheet, 1, 0, "C1");
        set(sheet, 1, 1, "RET1");
        set(sheet, 2, 0, "US");
        set(sheet, 2, 1, 1);
        set(sheet, 3, 0, "CA");
        set(sheet, 3, 1, 2);
        drivers(sheet);
        cars(sheet);
    }

    private static void drivers(Sheet sheet) {
        set(sheet, 5, 0, "Data Driver drivers");
        set(sheet, 6, 0, "name");
        set(sheet, 6, 1, "age");
        set(sheet, 7, 0, "Ann");
        set(sheet, 7, 1, 30);
    }

    private static void cars(Sheet sheet) {
        set(sheet, 9, 0, "Data Car cars");
        set(sheet, 10, 0, "model");
        set(sheet, 10, 1, "year");
        set(sheet, 11, 0, "Ford");
        set(sheet, 11, 1, 2020);
    }
}
