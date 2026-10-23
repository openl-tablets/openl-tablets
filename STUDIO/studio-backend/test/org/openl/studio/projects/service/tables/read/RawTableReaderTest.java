package org.openl.studio.projects.service.tables.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.model.tables.MergeTarget;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableRegion;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.studio.projects.model.tables.RawTableSignaturePart;
import org.openl.studio.projects.model.tables.RawTableSignaturePiece;
import org.openl.studio.projects.model.tables.RawTableSourceAction;
import org.openl.studio.projects.model.tables.RawTableTextRun;
import org.openl.studio.projects.model.tables.RawTableValueType;
import org.openl.studio.projects.service.tables.StyledRead;
import org.openl.studio.projects.service.tables.StyledRead.StyledRun;
import org.openl.studio.projects.service.tables.TableTestProjects;
import org.openl.studio.projects.service.tables.write.RawTableWriter;

/**
 * Confirms the raw reader caps rows at {@code maxRows} and reports the full count when truncated, while the
 * default path returns the whole table unchanged (so the shared Tables API stays non-regressive).
 */
class RawTableReaderTest {

    private static final String PROJECT = "test/rules/EPBDS-16160";

    @Test
    void reportsRangeEditorForSmartLookupHorizontalConditions(@TempDir Path projectDir) throws Exception {
        var module = TableTestProjects.projectModel(projectDir, "Rules", sheet -> {
            TableTestProjects.row(sheet, 1, 1,
                    "SmartLookup Integer Shipping(Integer boxes, Integer distance, Double mass)");
            TableTestProjects.row(sheet, 2, 1, "Boxes", "1..10", "11..20");
            TableTestProjects.row(sheet, 3, 2, "<=5", ">5");
            TableTestProjects.row(sheet, 4, 1, "1..3", "2", "4");
            TableTestProjects.merge(sheet, 1, 1, 1, 3);
            TableTestProjects.merge(sheet, 2, 3, 1, 1);
        });
        var source = new RawTableReader().read(TableTestProjects.table(module, "Shipping"),
                RawTableRead.builder().withMetaInfo(true).build()).source;

        var integerCondition = TableTestProjects.cellOf(source, "1..10").metaInfo();
        var doubleCondition = TableTestProjects.cellOf(source, "<=5").metaInfo();
        assertNotNull(integerCondition);
        assertNotNull(doubleCondition);
        assertFalse(integerCondition.usages().isEmpty());
        assertFalse(doubleCondition.usages().isEmpty());
        assertEquals("range", integerCondition.editor());
        assertEquals("range", doubleCondition.editor());
    }

    @Test
    void capsRowsAndReportsTotalWhenTruncated() throws Exception {
        IOpenLTable table = multiRowTable();
        var fullHeight = new RawTableReader().read(table).source.size();

        var capped = new RawTableReader().read(table, RawTableRead.builder().maxRows(1).build());

        assertEquals(1, capped.source.size(), "the result must be capped to maxRows");
        assertNotNull(capped.totalRows, "a truncated read must report the full row count");
        assertEquals(fullHeight, capped.totalRows);
    }

    @Test
    void returnsTheWholeTableByDefault() throws Exception {
        IOpenLTable table = multiRowTable();

        var full = new RawTableReader().read(table);
        assertNull(full.totalRows, "a full read carries no truncation marker");

        // A cap at or above the height is non-regressive: same rows, no marker.
        var wide = new RawTableReader().read(table, RawTableRead.builder().maxRows(full.source.size() + 10).build());
        assertEquals(full.source.size(), wide.source.size());
        assertNull(wide.totalRows);
    }

    @Test
    void attachesExcelStylesOnlyWhenRequested() throws Exception {
        IOpenLTable table = multiRowTable();

        // Default read carries no styles, so the shared API stays plain (e.g. for MCP).
        var plain = new RawTableReader().read(table);
        assertTrue(plain.source.stream().flatMap(List::stream).allMatch(c -> c.style() == null),
                "the default read must carry no cell styles");

        // With styles requested, the shape is unchanged and Excel formatting is attached.
        var styled = new StyledRead(new RawTableReader().read(table, RawTableRead.builder().withStyles(true).build()));
        assertEquals(plain.source.size(), styled.source.size());
        assertTrue(styled.source.stream().flatMap(List::stream).anyMatch(c -> c.style() != null),
                "a styled read must attach at least one cell style");
    }

    @Test
    void readsAWindowFromStartRowKeepingAbsoluteAddresses() throws Exception {
        IOpenLTable table = multiRowTable();
        var reader = new RawTableReader();

        List<List<RawTableCell>> full = reader.read(table).source;
        var fullHeight = full.size();

        // Slice from a plain data row, so no merged region is cut at the boundary, and confirm the window
        // lines up one-to-one with the whole-table read while keeping absolute cell addresses.
        var startRow = firstPlainRow(full);
        var window = reader.read(table, RawTableRead.builder().startRow(startRow).build());

        assertEquals(fullHeight - startRow, window.source.size(), "the window must skip the rows before startRow");
        assertEquals(cellAddresses(full.get(startRow)), cellAddresses(window.source.getFirst()),
                "a sliced cell must keep the address it has in the whole table");
        assertNotNull(window.totalRows, "a windowed read must report the full row count");
        assertEquals(fullHeight, window.totalRows);
    }

    @Test
    void capsTheWindowToMaxRowsFromStartRow() throws Exception {
        IOpenLTable table = multiRowTable();
        var reader = new RawTableReader();
        List<List<RawTableCell>> full = reader.read(table).source;

        // startRow and maxRows compose into a bounded slice taken from the middle of the table.
        var startRow = firstPlainRow(full);
        var oneRow = reader.read(table, RawTableRead.builder().startRow(startRow).maxRows(1).build());

        assertEquals(1, oneRow.source.size(), "the window must be capped to maxRows counted from startRow");
        assertEquals(cellAddresses(full.get(startRow)), cellAddresses(oneRow.source.getFirst()));
        assertEquals(full.size(), oneRow.totalRows);
    }

    @Test
    void returnsAnEmptyWindowWhenStartRowIsPastTheEnd() throws Exception {
        IOpenLTable table = multiRowTable();
        var reader = new RawTableReader();
        var fullHeight = reader.read(table).source.size();

        var beyond = reader.read(table, RawTableRead.builder().startRow(fullHeight + 5).build());

        assertTrue(beyond.source.isEmpty(), "an offset past the last row yields an empty matrix");
        assertEquals(fullHeight, beyond.totalRows, "the empty window still reports the full row count");
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "1, 1", "2, 3", "5, 5", "0, 2147483647"})
    void answersAWindowPastTheEndAlikeWithOrWithoutMaxRows(int pastTheEnd, int maxRows) throws Exception {
        IOpenLTable table = multiRowTable();
        var reader = new RawTableReader();
        var fullHeight = reader.read(table).source.size();
        var startRow = fullHeight + pastTheEnd;

        var uncapped = reader.read(table, RawTableRead.builder().startRow(startRow).build());
        var capped = reader.read(table, RawTableRead.builder().startRow(startRow).maxRows(maxRows).build());

        assertTrue(capped.source.isEmpty(), "a window past the last row holds no rows");
        assertEquals(fullHeight, capped.totalRows, "the empty window still reports the full row count");
        assertEquals(uncapped.source, capped.source);
        assertEquals(uncapped.totalRows, capped.totalRows);
    }

    @Test
    void readsToTheEndWhenMaxRowsIsTheLargestCount() throws Exception {
        IOpenLTable table = multiRowTable();
        var reader = new RawTableReader();
        List<List<RawTableCell>> full = reader.read(table).source;
        var startRow = firstPlainRow(full);

        var window = reader.read(table,
                RawTableRead.builder().startRow(startRow).maxRows(Integer.MAX_VALUE).build());

        assertEquals(full.size() - startRow, window.source.size(), "a count beyond the table reads to its end");
        assertEquals(full.size(), window.totalRows);
    }

    /** The A1 addresses of a matrix row, so a slice can be checked to keep absolute cell addresses. */
    @Test
    void aCellCarriesBothWhatItComputedAndWhatItWasWrittenWith(@TempDir Path tempDir) throws Exception {
        // Showing formulas is the screen's choice, so the read hands it both and asks nothing.
        var project = TableTestProjects.writeProject(tempDir.resolve("formulas"), "formulas", "Rules", new String[][]{
                {"Datatype Greeting", null, null},
                {"String", "code", "alpha"},
                {"int", "hour", "=1+2"}
        });

        var read = new RawTableReader().read(firstTable(project));

        var cells = read.source.stream().flatMap(List::stream).toList();
        var written = cells.stream().filter(cell -> cell.formula() != null).toList();
        assertEquals(1, written.size(), "only the cell written as a formula carries one");
        assertEquals("=1+2", written.getFirst().formula(), "the formula reads as Excel writes it");
        assertNotNull(written.getFirst().value(), "the value the formula computed stays beside it");
        assertTrue(cells.stream().noneMatch(cell -> String.valueOf(cell.value()).startsWith("=")),
                "a value is what the cell computed, never the formula behind it");
    }

    @Test
    void tellsHowManyRowsTheHeaderTakesSoAScreenCanHideIt(@TempDir Path tempDir) throws Exception {
        var project = TableTestProjects.writeProject(tempDir.resolve("header"), "header", "Rules", new String[][]{
                {"Datatype Greeting", null},
                {"String", "code"},
                {"int", "hour"}
        });

        var read = new RawTableReader().read(firstTable(project));

        // The engine's own business view of this table drops the header line, and nothing else.
        assertEquals(1, read.headerHeight);
        assertEquals(3, read.source.size(), "the read itself still carries the header");
    }

    @Test
    void readsAMergedCellWholeWhereAWindowWouldHaveCutIt(@TempDir Path tempDir) throws Exception {
        var project = TableTestProjects.writeProject(tempDir.resolve("grouped"), "grouped", "Rules", new String[][]{
                {"Datatype Greeting", null},
                {"String", "a"},
                {"same", "b"},
                {"same", "c"},
                {"int", "d"}
        });
        // The two middle rows are one cell in the first column, the way a rules table groups its rules.
        new RawTableWriter(firstTable(project))
                .apply(new RawTableSourceAction.Merge(new MergeTarget.Cells(2, 0, 2, 1)));

        var window = new RawTableReader().read(firstTable(project), RawTableRead.builder().maxRows(3).build());

        // Three rows would end halfway down the group. Answered so, the group would come back twice — clamped
        // here and rooted in the next window at a cell that holds nothing — and a screen reading the table
        // window by window would hold two groups where the workbook holds one.
        assertEquals(4, window.source.size(), "the window reaches the end of the merge it would have cut");
        assertEquals(Integer.valueOf(2), window.source.get(2).getFirst().rowspan());
        assertEquals(Boolean.TRUE, window.source.get(3).getFirst().covered());
        assertNotNull(window.totalRows, "the window still says how many rows the table has");

        // The next window starts where this one ended, so it starts on no merge either.
        var next = new RawTableReader().read(firstTable(project), RawTableRead.builder()
                .startRow(4)
                .maxRows(3)
                .build());
        assertEquals(1, next.source.size());
        assertNull(next.source.getFirst().getFirst().rowspan(), "the row after the group stands on its own");

        // A window placed by hand halfway down the group opens on the group instead: answered from where it
        // was asked for, its first cell would stand for the whole group while holding only half of it.
        var halfway = new RawTableReader().read(firstTable(project), RawTableRead.builder()
                .startRow(3)
                .maxRows(2)
                .build());
        assertEquals(Integer.valueOf(2), halfway.source.getFirst().getFirst().rowspan());
        assertEquals("same", halfway.source.getFirst().getFirst().value());
    }

    @Test
    void readsTheExactRowsAskedForCuttingTheMergedCellsAtTheEdges(@TempDir Path tempDir) throws Exception {
        var project = TableTestProjects.writeProject(tempDir.resolve("cut"), "cut", "Rules", new String[][]{
                {"Datatype Greeting", null},
                {"String", "a"},
                {"same", "b"},
                {"same", "c"},
                {"int", "d"}
        });
        new RawTableWriter(firstTable(project))
                .apply(new RawTableSourceAction.Merge(new MergeTarget.Cells(2, 0, 2, 1)));
        var whole = new RawTableReader().read(firstTable(project), RawTableRead.builder().build());
        var first = whole.source.get(2).getFirst().cell();
        var second = first.replaceAll("\\d", "") + (Integer.parseInt(first.replaceAll("\\D", "")) + 1);
        var area = first + ":" + second;
        assertNull(whole.source.get(2).getFirst().mergedArea(), "a merged cell read whole names no area");

        // A window ending halfway down the group holds the rows asked for and cuts the group at its bottom edge.
        var top = new RawTableReader().read(firstTable(project), RawTableRead.builder()
                .maxRows(3)
                .cutMerges(true)
                .build());
        assertEquals(3, top.source.size(), "the window holds exactly the rows asked for");
        var cutAtBottom = top.source.get(2).getFirst();
        assertEquals(first, cutAtBottom.cell());
        assertEquals("same", cutAtBottom.value());
        assertNull(cutAtBottom.rowspan(), "the part of the group inside the window is one row");
        assertEquals(area, cutAtBottom.mergedArea(), "the cut cell names the whole group");

        // A window opening halfway down the group holds the rest of it, with the value of the group.
        var bottom = new RawTableReader().read(firstTable(project), RawTableRead.builder()
                .startRow(3)
                .maxRows(1)
                .cutMerges(true)
                .build());
        assertEquals(1, bottom.source.size());
        var cutAtTop = bottom.source.getFirst().getFirst();
        assertEquals(second, cutAtTop.cell(), "the cut cell stands where the window shows it");
        assertEquals("same", cutAtTop.value(), "the cut cell holds the value of the group");
        assertEquals(area, cutAtTop.mergedArea());
        assertNotNull(bottom.totalRows);

        var beyond = new RawTableReader().read(firstTable(project), RawTableRead.builder()
                .startRow(10)
                .cutMerges(true)
                .build());
        assertTrue(beyond.source.isEmpty(), "a window past the last row holds no rows");
        assertEquals(Integer.valueOf(whole.source.size()), beyond.totalRows);
    }

    @Test
    void readsTheBordersAndTheFontsOfThePiecesOfATextTheWorkbookDraws(@TempDir Path tempDir) throws Exception {
        var model = TableTestProjects.projectModel(tempDir.resolve("drawn"), "Drawn", sheet -> {
            var workbook = (XSSFWorkbook) sheet.getWorkbook();
            var grey = workbook.createFont();
            grey.setColor(new XSSFColor(new byte[]{(byte) 0x80, (byte) 0x80, (byte) 0x80}));
            var bold = workbook.createFont();
            bold.setBold(true);
            var header = new XSSFRichTextString("Datatype Greeting");
            header.applyFont(0, 9, grey);
            header.applyFont(9, 17, bold);
            sheet.createRow(1).createCell(1).setCellValue(header);
            TableTestProjects.row(sheet, 2, 1, "String", "code");
            var closed = workbook.createCellStyle();
            closed.setBorderBottom(BorderStyle.THIN);
            closed.setBottomBorderColor(new XSSFColor(new byte[]{(byte) 0xFF, 0, 0}));
            sheet.getRow(2).getCell(1).setCellStyle(closed);
        });

        var source = new StyledRead(new RawTableReader().read(TableTestProjects.table(model, "Greeting"), RawTableRead.builder()
                .withStyles(true)
                .build())).source;

        var runs = source.getFirst().getFirst().runs();
        assertEquals(List.of("Datatype ", "Greeting"), runs.stream().map(StyledRun::text).toList());
        assertEquals("#808080", runs.getFirst().style().color());
        assertEquals(Boolean.TRUE, runs.get(1).style().bold());
        var bottom = source.get(1).getFirst().style().border().bottom();
        assertEquals("#ff0000", bottom.color());
        assertNull(source.get(1).getFirst().style().border().top(),
                "A side the workbook draws nothing on has no border");
        assertNull(source.get(1).get(1).style(), "A cell the workbook draws no border around carries none");
    }

    @Test
    void namesEachStyleOnceAndEachPieceOfTextByWhereItEnds(@TempDir Path tempDir) throws Exception {
        var model = TableTestProjects.projectModel(tempDir.resolve("shared"), "Shared", sheet -> {
            var workbook = (XSSFWorkbook) sheet.getWorkbook();
            var bold = workbook.createFont();
            bold.setBold(true);
            var header = new XSSFRichTextString("Datatype Greeting");
            header.applyFont(9, 17, bold);
            sheet.createRow(1).createCell(1).setCellValue(header);
            var filled = workbook.createCellStyle();
            filled.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0xDD, (byte) 0xEB, (byte) 0xF7}));
            filled.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            for (var row = 2; row < 5; row++) {
                TableTestProjects.row(sheet, row, 1, "String", "code" + row);
                sheet.getRow(row).getCell(2).setCellStyle(filled);
            }
        });
        var table = TableTestProjects.table(model, "Greeting");

        var view = new RawTableReader().read(table, RawTableRead.builder().withStyles(true).build());

        var names = view.source.subList(1, 4).stream().map(row -> row.get(1).style()).distinct().toList();
        assertEquals(1, names.size(), "Three cells of one style name one entry");
        assertEquals("#ddebf7", view.styles.get(names.getFirst()).background());
        assertNull(view.source.get(1).getFirst().style(), "A cell in no style names none");
        var runs = view.source.getFirst().getFirst().runs();
        assertEquals(List.of(9, 17), runs.stream().map(RawTableTextRun::to).toList(), "Each piece ends where it ends");
        assertNull(runs.getFirst().style(), "A piece in the font of the cell names no style");
        assertEquals(Boolean.TRUE, view.styles.get(runs.get(1).style()).bold());
        assertEquals(view.styles.size(), view.styles.stream().distinct().count(), "No style is named twice");

        var plain = new RawTableReader().read(table, RawTableRead.builder().build());
        assertNull(plain.styles, "A read of the values names no styles");
        assertNull(plain.source.getFirst().getFirst().runs());
        var cells = new RawTableReader().readCells(table, true);
        assertEquals(view.styles, cells.styles(), "A matrix read alone names its styles the same way");
        assertNull(new RawTableReader().readCells(table, false).styles());
    }

    @Test
    void readsALineTwoCellsShareOnEachCellThatDrawsIt(@TempDir Path tempDir) throws Exception {
        var model = TableTestProjects.projectModel(tempDir.resolve("lines"), "Lines", sheet -> {
            var workbook = sheet.getWorkbook();
            TableTestProjects.row(sheet, 1, 1, "Datatype Lines");
            TableTestProjects.row(sheet, 2, 1, "String", "first");
            TableTestProjects.row(sheet, 3, 1, "String", "second");
            var over = workbook.createCellStyle();
            over.setBorderTop(BorderStyle.MEDIUM);
            sheet.getRow(3).getCell(1).setCellStyle(over);
            var under = workbook.createCellStyle();
            under.setBorderBottom(BorderStyle.THIN);
            sheet.getRow(2).getCell(2).setCellStyle(under);
        });

        var source = new StyledRead(new RawTableReader().read(TableTestProjects.table(model, "Lines"),
                RawTableRead.builder().withStyles(true).build())).source;

        assertEquals(2, source.get(2).getFirst().style().border().top().width(), "The lower cell draws its line");
        assertNull(source.get(1).getFirst().style(), "The upper cell draws none, so it reports none");
        assertEquals(1, source.get(1).get(1).style().border().bottom().width(), "The upper cell draws its line");
        assertNull(source.get(2).get(1).style(), "The lower cell draws none, so it reports none");
    }

    @Test
    void readsTheRolesOfTheWholeTableWithEveryWindowOfItsRows(@TempDir Path tempDir) throws Exception {
        var model = TableTestProjects.projectModel(tempDir.resolve("roles"), "Roles", sheet -> {
            TableTestProjects.row(sheet, 1, 1, "Datatype Limits");
            TableTestProjects.row(sheet, 2, 1, "Integer", "low", "1");
            TableTestProjects.row(sheet, 3, 1, "Integer", "high", "9");
            TableTestProjects.row(sheet, 4, 1, "String", "name", "top");
        });
        var table = TableTestProjects.table(model, "Limits");

        var window = new RawTableReader().read(table, RawTableRead.builder()
                .startRow(2)
                .maxRows(2)
                .withStyles(true)
                .build());

        assertEquals(new RawTableRegion(RawTableRole.VALUE, RawTableValueType.NUMBER, 1, 2, 3, 2),
                window.regions.getLast(), "The regions of the whole table, whatever window is read");
        assertEquals(List.of(new RawTableSignaturePiece(RawTableSignaturePart.KEYWORD, 8),
                new RawTableSignaturePiece(RawTableSignaturePart.NAME, 15)), window.signature);
        assertNull(window.groups, "A table without groups sends none");
        assertNull(window.source.getFirst().get(2).type(), "A number takes the type of its region");
        assertEquals(RawTableValueType.STRING, window.source.get(1).get(2).type(), "A text names its own type");

        var plain = new RawTableReader().read(table, RawTableRead.builder().build());
        assertNull(plain.regions, "A read of the values alone reports no roles");
        assertNull(plain.source.get(3).get(2).type());
    }

    @Test
    void leavesTheNameAndTheSizeOfAFontToTheScreen(@TempDir Path tempDir) throws Exception {
        var model = TableTestProjects.projectModel(tempDir.resolve("fonts"), "Fonts", sheet -> {
            var workbook = sheet.getWorkbook();
            TableTestProjects.row(sheet, 1, 1, "Datatype Fonts");
            TableTestProjects.row(sheet, 2, 1, "String", "name");
            var light = workbook.createFont();
            light.setFontName("Calibri Light");
            light.setFontHeightInPoints((short) 14);
            var titled = workbook.createCellStyle();
            titled.setFont(light);
            sheet.getRow(1).getCell(1).setCellStyle(titled);
            var bold = workbook.createFont();
            bold.setBold(true);
            var strong = workbook.createCellStyle();
            strong.setFont(bold);
            sheet.getRow(2).getCell(2).setCellStyle(strong);
        });

        var view = new RawTableReader().read(TableTestProjects.table(model, "Fonts"),
                RawTableRead.builder().withStyles(true).build());
        var source = new StyledRead(view).source;

        assertNull(source.getFirst().getFirst().style(), "A font of another name and size is no style of the cell");
        assertEquals(Boolean.TRUE, source.get(1).get(1).style().bold(), "The weight of a font is");
        assertNull(source.get(1).getFirst().style(), "A cell in the font of the workbook has no style");
    }

    private static List<String> cellAddresses(List<RawTableCell> row) {
        return row.stream().map(RawTableCell::cell).toList();
    }

    /** The first plain data row (every cell a simple 1x1 origin), so slicing there cuts no merged region. */
    private static int firstPlainRow(List<List<RawTableCell>> matrix) {
        for (var r = 1; r < matrix.size(); r++) {
            if (matrix.get(r).stream().allMatch(c -> c.cell() != null && c.colspan() == null && c.rowspan() == null)) {
                return r;
            }
        }
        throw new IllegalStateException("no plain data row to slice at");
    }

    /** The first table of a project written for this test. */
    private static IOpenLTable firstTable(Path project) {
        for (TableSyntaxNode tsn : TableTestProjects.projectModel(project).getAllTableSyntaxNodes()) {
            var table = new TableSyntaxNodeAdapter(tsn);
            if (table.getGridTable(IXlsTableNames.VIEW_DEVELOPER) != null) {
                return table;
            }
        }
        throw new IllegalStateException("no table resolved in " + project);
    }

    /** The first table in the test project with more than one row, so the cap is observable. */
    private static IOpenLTable multiRowTable() throws Exception {
        var modules = ProjectResolver.getInstance().resolve(Path.of(PROJECT)).getModules();
        var projectModel = new ProjectModel(mock(WebStudio.class), null);
        projectModel.setModuleInfo(modules.getFirst());
        var reader = new RawTableReader();
        for (TableSyntaxNode tsn : projectModel.getAllTableSyntaxNodes()) {
            var table = new TableSyntaxNodeAdapter(tsn);
            if (table.getGridTable(IXlsTableNames.VIEW_DEVELOPER) != null && reader.read(table).source.size() > 1) {
                return table;
            }
        }
        throw new IllegalStateException("no multi-row table found in " + PROJECT);
    }
}
