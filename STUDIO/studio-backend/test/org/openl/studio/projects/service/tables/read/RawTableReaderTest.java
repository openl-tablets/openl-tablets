package org.openl.studio.projects.service.tables.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
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
import org.openl.studio.projects.model.tables.RawTableSourceAction;
import org.openl.studio.projects.model.tables.RawTableTextRun;
import org.openl.studio.projects.service.tables.TableTestProjects;
import org.openl.studio.projects.service.tables.write.RawTableWriter;

/**
 * Confirms the raw reader caps rows at {@code maxRows} and reports the full count when truncated, while the
 * default path returns the whole table unchanged (so the shared Tables API stays non-regressive).
 */
class RawTableReaderTest {

    private static final String PROJECT = "test/rules/EPBDS-16160";

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
        var styled = new RawTableReader().read(table, RawTableRead.builder().withStyles(true).build());
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

        var source = new RawTableReader().read(TableTestProjects.table(model, "Greeting"), RawTableRead.builder()
                .withStyles(true)
                .build()).source;

        var runs = source.getFirst().getFirst().runs();
        assertEquals(List.of("Datatype ", "Greeting"), runs.stream().map(RawTableTextRun::text).toList());
        assertEquals("#808080", runs.getFirst().style().color());
        assertEquals(Boolean.TRUE, runs.get(1).style().bold());
        var bottom = source.get(1).getFirst().style().border().bottom();
        assertEquals("#ff0000", bottom.color());
        assertNull(source.get(1).getFirst().style().border().top(),
                "A side the workbook draws nothing on has no border");
        assertNull(source.get(1).get(1).style(), "A cell the workbook draws no border around carries none");
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
