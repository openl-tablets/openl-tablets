package org.openl.studio.projects.service.tables.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.AppendTarget;
import org.openl.studio.projects.model.tables.DeleteTarget;
import org.openl.studio.projects.model.tables.InsertTarget;
import org.openl.studio.projects.model.tables.MergeTarget;
import org.openl.studio.projects.model.tables.RawCellInput;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableSourceAction;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.model.tables.UnmergeTarget;
import org.openl.studio.projects.model.tables.UpdateTarget;
import org.openl.studio.projects.service.tables.read.RawTableReader;

/**
 * Verifies the raw-source edits applied by {@link RawTableWriter#apply}. Each test starts from a freshly written
 * spreadsheet, applies one action, reloads the project from disk, and asserts against the reloaded matrix — the
 * in-memory grid is stale after a save.
 */
class RawTableWriterTest {

    private static final String HEADER = "Datatype Greeting";

    @TempDir
    Path tempDir;

    private Path mainProject;

    @BeforeEach
    void setUp() throws IOException {
        mainProject = writeProject("main", new String[][]{
                {HEADER, null, null},
                {"String", "code", "alpha"},
                {"String", "text", "beta"},
                {"int", "hour", "gamma"}
        });
    }

    @Test
    void appendsRowToTheEnd() {
        apply(appendRow(row("double", "rate", "delta")));

        var source = reload(mainProject);
        assertEquals(5, source.size());
        assertEquals("double", value(source, 4, 0));
        assertEquals("rate", value(source, 4, 1));
        assertEquals("delta", value(source, 4, 2));
    }

    @Test
    void insertsRowAtPosition() {
        apply(insertRow(1, row("long", "id", "epsilon")));

        var source = reload(mainProject);
        assertEquals(5, source.size());
        assertEquals("long", value(source, 1, 0));
        assertEquals("id", value(source, 1, 1));
        // the row that previously sat at index 1 is shifted down
        assertEquals("String", value(source, 2, 0));
        assertEquals("code", value(source, 2, 1));
    }

    @Test
    void deletesRow() {
        apply(deleteRow(1));

        var source = reload(mainProject);
        assertEquals(3, source.size());
        // the row that previously sat at index 2 is shifted up
        assertEquals("text", value(source, 1, 1));
    }

    @Test
    void appendsColumnToTheEnd() {
        apply(appendColumn(row("a", "b", "c", "d")));

        var source = reload(mainProject);
        assertEquals(4, width(source));
        assertEquals("a", value(source, 0, 3));
        assertEquals("b", value(source, 1, 3));
        assertEquals("d", value(source, 3, 3));
    }

    @Test
    void insertsColumnAtPosition() {
        apply(insertColumn(1, row("p", "q", "r", "s")));

        var source = reload(mainProject);
        assertEquals(4, width(source));
        assertEquals("p", value(source, 0, 1));
        assertEquals("q", value(source, 1, 1));
        // the column that previously sat at index 1 is shifted right
        assertEquals("code", value(source, 1, 2));
    }

    @Test
    void insertsRowsBlock() {
        // insert two valid datatype rows at position 1, shifting the rest down
        apply(insertRows(1, List.of(row("String", "f1", "d1"), row("String", "f2", "d2"))));

        var source = reload(mainProject);
        assertEquals(6, source.size());
        assertEquals("f1", value(source, 1, 1));
        assertEquals("f2", value(source, 2, 1));
        // the row that previously sat at index 1 is shifted down by two
        assertEquals("code", value(source, 3, 1));
    }

    @Test
    void insertsColumnsBlock() {
        // insert two full-height columns at position 1, shifting the rest right
        apply(insertColumns(1, List.of(row("p", "q", "r", "s"), row("t", "u", "v", "w"))));

        var source = reload(mainProject);
        assertEquals(5, width(source));
        assertEquals("p", value(source, 0, 1));
        assertEquals("t", value(source, 0, 2));
        // the column that previously sat at index 1 is shifted right by two
        assertEquals("code", value(source, 1, 3));
    }

    @Test
    void rejectsRowBlockNotMatchingWidth() {
        // each row of the block must be as wide as the table
        assertBadRequest(insertRows(1, List.of(row("a", "b"), row("c", "d"))));
    }

    @Test
    void appendsRowsBlock() {
        apply(appendRows(List.of(row("String", "g1", "x1"), row("String", "g2", "x2"))));

        var source = reload(mainProject);
        assertEquals(6, source.size());
        assertEquals("g1", value(source, 4, 1));
        assertEquals("g2", value(source, 5, 1));
    }

    @Test
    void appendsColumnsBlock() {
        apply(appendColumns(List.of(row("a", "b", "c", "d"), row("e", "f", "g", "h"))));

        var source = reload(mainProject);
        assertEquals(5, width(source));
        assertEquals("a", value(source, 0, 3));
        assertEquals("e", value(source, 0, 4));
    }

    @Test
    void deletesRowsBlock() {
        apply(deleteRows(1, 2));

        var source = reload(mainProject);
        assertEquals(2, source.size());
        // rows 1-2 removed; the row that was at index 3 shifts up to 1
        assertEquals("int", value(source, 1, 0));
    }

    @Test
    void deletesColumnsBlock() {
        apply(deleteColumns(1, 2));

        var source = reload(mainProject);
        assertEquals(1, width(source));
        assertEquals("String", value(source, 1, 0));
    }

    @Test
    void deletingABlockClearsAFullyContainedMerge() {
        // merge two stacked body cells, then delete exactly that 2-row block; the merge must not linger as an orphan
        apply(merge(1, 0, 2, 1));
        apply(deleteRows(1, 2));

        var source = reload(mainProject);
        assertEquals(2, source.size());
        assertNull(source.get(1).get(0).rowspan(), "a fully-contained merge must be removed, not left stale");
        assertNull(source.get(1).get(0).covered());
        assertEquals("int", value(source, 1, 0));
    }

    @Test
    void deletesColumn() {
        apply(deleteColumn(2));

        var source = reload(mainProject);
        assertEquals(2, width(source));
        assertEquals("code", value(source, 1, 1));
    }

    @Test
    void updatesRow() {
        apply(updateRow(2, row("BIGINT", "renamed", "newDefault")));

        var source = reload(mainProject);
        assertEquals(4, source.size());
        assertEquals("BIGINT", value(source, 2, 0));
        assertEquals("renamed", value(source, 2, 1));
        assertEquals("newDefault", value(source, 2, 2));
        // a neighbouring row is left untouched
        assertEquals("String", value(source, 1, 0));
    }

    @Test
    void updatesColumn() {
        apply(updateColumn(1, row("h", "c1", "c2", "c3")));

        var source = reload(mainProject);
        assertEquals(3, width(source));
        assertEquals("c1", value(source, 1, 1));
        assertEquals("c2", value(source, 2, 1));
        assertEquals("c3", value(source, 3, 1));
        // a neighbouring column is left untouched
        assertEquals("String", value(source, 1, 0));
    }

    @Test
    void updatesCell() {
        apply(updateCell(2, 1, "updated"));

        var source = reload(mainProject);
        assertEquals("updated", value(source, 2, 1));
    }

    @Test
    void updatesRange() {
        // overwrite the 2x2 block at (1,1) in place; the table keeps its size and neighbours are untouched
        apply(updateRange(1, 1, List.of(row("X1", "X2"), row("Y1", "Y2"))));

        var source = reload(mainProject);
        assertEquals(4, source.size());
        assertEquals(3, width(source));
        assertEquals("X1", value(source, 1, 1));
        assertEquals("X2", value(source, 1, 2));
        assertEquals("Y1", value(source, 2, 1));
        assertEquals("Y2", value(source, 2, 2));
        assertEquals("String", value(source, 1, 0));
        assertEquals("int", value(source, 3, 0));
    }

    @Test
    void rejectsRangeOutOfBounds() {
        // a 3x3 block at (2,1) runs past the 4x3 table
        assertBadRequest(updateRange(2, 1, List.of(row("a", "b", "c"), row("d", "e", "f"), row("g", "h", "i"))));
    }

    @Test
    void rejectsSingleCellRange() {
        // a 1x1 range is the cell update's job
        assertBadRequest(updateRange(1, 1, List.of(row("x"))));
    }

    @Test
    void rejectsRaggedRange() {
        // rows of unequal length are not a rectangle
        assertBadRequest(updateRange(1, 1, List.of(row("a", "b"), row("c"))));
    }

    @Test
    void rejectsAllBlankRange() {
        // a range that blanks an entire row would split the table, like an all-empty updateRow
        assertBadRequest(updateRange(1, 0, List.of(row(null, null, null))));
    }

    @Test
    void updateRangeClearsAStaleMerge() {
        // merge the two stacked "String" cells, then overwrite that 2x1 block with plain cells
        apply(merge(1, 0, 2, 1));
        apply(updateRange(1, 0, List.of(row("p"), row("q"))));

        var source = reload(mainProject);
        assertNull(source.get(1).get(0).rowspan(), "stale merge must be cleared on a range update");
        assertNull(source.get(2).get(0).covered());
        assertEquals("p", value(source, 1, 0));
        assertEquals("q", value(source, 2, 0));
    }

    @Test
    void appendsRowWithInlineMerge() {
        apply(appendRow(List.of(
                new RawCellInput("MERGED", 2, null, null),
                new RawCellInput(null, null, null, true),
                new RawCellInput("tail", null, null, null))));

        var source = reload(mainProject);
        int last = source.size() - 1;
        assertEquals("MERGED", value(source, last, 0));
        assertEquals(Integer.valueOf(2), source.get(last).get(0).colspan());
        assertEquals(Boolean.TRUE, source.get(last).get(1).covered());
        assertEquals("tail", value(source, last, 2));
    }

    @Test
    void mergesCellRange() {
        // merge the two leftmost cells of the header row (header + blank) into one spanning two columns
        apply(merge(0, 0, 1, 2));

        var source = reload(mainProject);
        assertEquals(Integer.valueOf(2), source.get(0).get(0).colspan());
        assertEquals(Boolean.TRUE, source.get(0).get(1).covered());
    }

    @Test
    void rejectsInvalidMergeRange() {
        // a single cell is not a range
        assertBadRequest(merge(1, 0, 1, 1));
        // a range that leaves the table bounds
        assertBadRequest(merge(0, 0, 99, 99));
    }

    @Test
    void rejectsMergeThatWouldDiscardData() {
        // the two leftmost cells of row 1 hold different values ("String" and "code"); merging would drop one
        assertBadRequest(merge(1, 0, 1, 2));
    }

    @Test
    void mergesEqualAdjacentValues() {
        // rows 1 and 2 of the first column both hold "String", so merging them loses nothing
        apply(merge(1, 0, 2, 1));

        var source = reload(mainProject);
        assertEquals(Integer.valueOf(2), source.get(1).get(0).rowspan());
        assertEquals(Boolean.TRUE, source.get(2).get(0).covered());
    }

    @Test
    void mergeBlanksCoveredCells() {
        // merging two equal "String" cells then unmerging must leave the covered cell empty, not a hidden orphan
        apply(merge(1, 0, 2, 1));
        apply(unmerge(1, 0));

        var source = reload(mainProject);
        assertEquals("String", value(source, 1, 0));
        assertNull(source.get(2).get(0).value(), "the covered cell's orphan value must be cleared by the merge");
    }

    @Test
    void unmergesCellRange() {
        apply(merge(0, 0, 1, 2));
        // pointing at any cell of the merged region unmerges it
        apply(unmerge(0, 1));

        var source = reload(mainProject);
        assertNull(source.get(0).get(0).colspan());
        assertNull(source.get(0).get(1).covered());
    }

    @Test
    void rejectsUnmergeOfPlainCell() {
        assertBadRequest(unmerge(2, 2));
    }

    @Test
    void updateRowClearsAStaleMerge() {
        // merge the two leftmost cells of the header row, then overwrite that row with plain (merge-free) cells
        apply(merge(0, 0, 1, 2));
        // keep a recognized keyword in the header cell so the action does not invalidate the table
        apply(updateRow(0, row("Datatype", "q", "r")));

        var source = reload(mainProject);
        // the merge dropped from the new cells must not linger, and the row holds the new values
        assertNull(source.get(0).get(0).colspan(), "stale merge must be cleared on update");
        assertNull(source.get(0).get(1).covered());
        assertEquals("Datatype", value(source, 0, 0));
        assertEquals("q", value(source, 0, 1));
        assertEquals("r", value(source, 0, 2));
    }

    @Test
    void rejectsPositionOutOfRange() {
        assertBadRequest(insertRow(99, row("x", "y", "z")));
    }

    @Test
    void rejectsInsertingBeforeTheHeader() {
        // The header must remain the first row, so position 0 is not allowed for insertion.
        assertBadRequest(insertRow(0, row("x", "y", "z")));
        assertBadRequest(insertColumn(0, row("x", "y", "z", "w")));
    }

    @Test
    void rejectsRowWiderThanTable() {
        assertBadRequest(appendRow(row("a", "b", "c", "d")));
    }

    @Test
    void rejectsColumnTallerThanTable() {
        assertBadRequest(appendColumn(row("a", "b", "c", "d", "e")));
    }

    @Test
    void rejectsRowNarrowerThanTable() {
        // fewer cells than the 3 columns must fail rather than leaving the trailing cell empty
        assertBadRequest(appendRow(row("a", "b")));
    }

    @Test
    void rejectsColumnShorterThanTable() {
        // fewer cells than the 4 rows must fail rather than leaving the trailing cell empty
        assertBadRequest(appendColumn(row("a", "b", "c")));
    }

    @Test
    void rejectsInsertColumnShorterThanTable() {
        assertBadRequest(insertColumn(1, row("a", "b", "c")));
    }

    @Test
    void rejectsEmptyCells() {
        // an empty block (no lines) is rejected
        assertBadRequest(appendRows(List.of()));
    }

    @Test
    void rejectsEmptyUpdateCells() {
        // an empty cell list for a row update is rejected
        assertBadRequest(updateRow(1, List.of()));
    }

    @Test
    void rejectsAllEmptyWrittenLine() {
        // a fully blank inserted/appended line would become a table-splitting blank line
        assertBadRequest(insertRows(1, List.of(row(null, null, null))));
        assertBadRequest(appendRow(row("", "", "")));
    }

    @Test
    void rejectsAllCoveredWrittenLine() {
        // a line of only covered placeholders writes nothing: the writer skips covered cells, so an append is a no-op
        // and an insert leaves a table-splitting blank line. At least one non-covered value/span cell is required.
        var allCovered = List.of(
                new RawCellInput(null, null, null, true),
                new RawCellInput(null, null, null, true),
                new RawCellInput(null, null, null, true));
        assertBadRequest(appendRow(allCovered));
        assertBadRequest(insertRows(1, List.of(allCovered)));
    }

    @Test
    void rejectsNullLineInBlock() {
        // a null row inside a block is rejected with 400, not a NullPointerException (500)
        assertBadRequest(insertRows(1, Arrays.asList(row("a", "b", "c"), null)));
        assertBadRequest(updateRange(1, 1, Arrays.asList(row("a", "b"), null)));
    }

    @Test
    void rejectsActionThatBreaksTheHeader() {
        // an action that rewrites the header to an unrecognized keyword is rejected (mirrors create/update validation)
        assertBadRequest(updateCell(0, 0, "NotATableType"));
    }

    @Test
    void rejectsCellOutOfBounds() {
        assertBadRequest(updateCell(99, 0, "x"));
    }

    @Test
    void deletesColumnUnderMergedHeader() throws IOException {
        // Header row spans all 3 columns (a common rule/datatype layout); deleting a column must still shrink the
        // table to 2 columns and resize the header merge, not just blank the column.
        var project = writeProjectWithMergedHeader("merged", new String[][]{
                {"Header Spanning All", null, null},
                {"a", "b", "c"},
                {"d", "e", "f"}
        });
        new RawTableWriter(load(project)).apply(deleteColumn(2));

        var source = reload(project);
        assertEquals(2, width(source), "the column must be removed, leaving 2 columns");
        assertEquals("a", value(source, 1, 0));
        assertEquals("b", value(source, 1, 1));
        // header still spans the remaining columns
        assertEquals(Integer.valueOf(2), source.getFirst().getFirst().colspan());
    }

    @Test
    void rejectsUpdatingACellHiddenByASpanningHeader() throws IOException {
        // The row-0 cell of column 2 is hidden under the spanning header; writing a value there must be rejected
        // (consistent with updateCell), not silently dropped. Such positions are marked with "covered": true instead.
        var project = writeProjectWithMergedHeader("masked", new String[][]{
                {"Header Spanning All", null, null},
                {"a", "b", "c"},
                {"d", "e", "f"}
        });
        assertBadRequest(project, updateColumn(2, row("masked-top", "C2", "F2")));
    }

    @Test
    void rejectsDeletingTheLastLine() throws IOException {
        var single = writeProject("single", new String[][]{{"Environment"}});

        assertBadRequest(single, deleteRow(0));
        assertBadRequest(single, deleteColumn(0));
    }

    @Test
    void rejectsDeletingHeaderLine() {
        // the header row and the leading-label column are protected, symmetric with insert rejecting position 0
        assertBadRequest(deleteRow(0));
        assertBadRequest(deleteColumn(0));
    }

    @Test
    void deletesTable() throws Exception {
        var project = writeProject("two-tables", new String[][]{
                {"Datatype First", null, null},
                {"String", "a", "x"},
                {null, null, null},
                {"Datatype Second", null, null},
                {"String", "b", "y"}
        });
        assertEquals(2, loadTables(project).size(), "project must start with two tables");

        new RawTableWriter(tableByHeader(project, "Datatype First")).delete();

        var headers = loadTables(project).stream()
                .map(table -> readSource(table).getFirst().getFirst().value())
                .toList();
        assertEquals(List.of("Datatype Second"), headers, "only the other table must remain");
    }

    @Test
    void deletesTableWithMergedHeader() throws Exception {
        var project = writeProjectWithMergedHeader("merged-delete", new String[][]{
                {"Header Spanning All", null, null},
                {"a", "b", "c"}
        });
        new RawTableWriter(load(project)).delete();

        try (var in = Files.newInputStream(project.resolve("merged-delete.xlsx"));
                var workbook = new XSSFWorkbook(in)) {
            assertEquals(0, workbook.getSheetAt(0).getNumMergedRegions(),
                    "the header merge must be removed together with the table");
        }
    }

    private void apply(RawTableSourceAction action) {
        new RawTableWriter(load(mainProject)).apply(action);
    }

    private void apply(Path project, RawTableSourceAction action) {
        new RawTableWriter(load(project)).apply(action);
    }

    private void assertBadRequest(RawTableSourceAction action) {
        assertThrows(BadRequestException.class, () -> apply(action));
    }

    private void assertBadRequest(Path project, RawTableSourceAction action) {
        assertThrows(BadRequestException.class, () -> apply(project, action));
    }

    private List<List<RawTableCell>> reload(Path project) {
        return readSource(load(project));
    }

    private static List<List<RawTableCell>> readSource(IOpenLTable table) {
        RawTableView view = new RawTableReader().read(table);
        return view.source;
    }

    private static Object value(List<List<RawTableCell>> source, int row, int col) {
        return source.get(row).get(col).value();
    }

    private static int width(List<List<RawTableCell>> source) {
        return source.getFirst().size();
    }

    private static List<RawCellInput> row(Object... values) {
        return Arrays.stream(values)
                .map(v -> new RawCellInput(v, null, null, null))
                .toList();
    }

    // Thin factories so the call sites read clearly. The single-line helpers wrap one line into a one-line block, so
    // the existing single-line tests now exercise the block variants.

    private static RawTableSourceAction appendRow(List<RawCellInput> cells) {
        return appendRows(List.of(cells));
    }

    private static RawTableSourceAction appendColumn(List<RawCellInput> cells) {
        return appendColumns(List.of(cells));
    }

    private static RawTableSourceAction appendRows(List<List<RawCellInput>> cells) {
        return new RawTableSourceAction.Append(new AppendTarget.Rows(cells));
    }

    private static RawTableSourceAction appendColumns(List<List<RawCellInput>> cells) {
        return new RawTableSourceAction.Append(new AppendTarget.Columns(cells));
    }

    private static RawTableSourceAction insertRow(int position, List<RawCellInput> cells) {
        return insertRows(position, List.of(cells));
    }

    private static RawTableSourceAction insertColumn(int position, List<RawCellInput> cells) {
        return insertColumns(position, List.of(cells));
    }

    private static RawTableSourceAction insertRows(int position, List<List<RawCellInput>> cells) {
        return new RawTableSourceAction.Insert(new InsertTarget.Rows(position, cells));
    }

    private static RawTableSourceAction insertColumns(int position, List<List<RawCellInput>> cells) {
        return new RawTableSourceAction.Insert(new InsertTarget.Columns(position, cells));
    }

    private static RawTableSourceAction deleteRow(int position) {
        return deleteRows(position, 1);
    }

    private static RawTableSourceAction deleteColumn(int position) {
        return deleteColumns(position, 1);
    }

    private static RawTableSourceAction deleteRows(int position, int count) {
        return new RawTableSourceAction.Delete(new DeleteTarget.Rows(position, count));
    }

    private static RawTableSourceAction deleteColumns(int position, int count) {
        return new RawTableSourceAction.Delete(new DeleteTarget.Columns(position, count));
    }

    private static RawTableSourceAction updateRow(int position, List<RawCellInput> cells) {
        return new RawTableSourceAction.Update(new UpdateTarget.Row(position, cells));
    }

    private static RawTableSourceAction updateColumn(int position, List<RawCellInput> cells) {
        return new RawTableSourceAction.Update(new UpdateTarget.Column(position, cells));
    }

    private static RawTableSourceAction updateCell(int row, int column, Object value) {
        return new RawTableSourceAction.Update(new UpdateTarget.Cell(row, column, value));
    }

    private static RawTableSourceAction updateRange(int row, int column, List<List<RawCellInput>> cells) {
        return new RawTableSourceAction.Update(new UpdateTarget.Range(row, column, cells));
    }

    private static RawTableSourceAction merge(int row, int column, int rowspan, int colspan) {
        return new RawTableSourceAction.Merge(new MergeTarget.Cells(row, column, rowspan, colspan));
    }

    private static RawTableSourceAction unmerge(int row, int column) {
        return new RawTableSourceAction.Unmerge(new UnmergeTarget.Cells(row, column));
    }

    /**
     * Resolve the single-module project at {@code dir} and return its first table.
     */
    private static IOpenLTable load(Path dir) {
        try {
            var modules = ProjectResolver.getInstance().resolve(dir).getModules();
            var projectModel = new ProjectModel(mock(WebStudio.class), null);
            projectModel.setModuleInfo(modules.getFirst());
            for (TableSyntaxNode tsn : projectModel.getAllTableSyntaxNodes()) {
                IOpenLTable table = new TableSyntaxNodeAdapter(tsn);
                if (table.getGridTable(IXlsTableNames.VIEW_DEVELOPER) != null) {
                    return table;
                }
            }
            throw new IllegalStateException("No table resolved in " + dir);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve project at " + dir, e);
        }
    }

    /**
     * Write a single-sheet workbook holding one table that starts at cell B2. {@code null} cells are left blank.
     */
    private Path writeProject(String name, String[][] grid) throws IOException {
        var dir = tempDir.resolve(name);
        Files.createDirectories(dir);
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet(name);
            for (int r = 0; r < grid.length; r++) {
                var sheetRow = sheet.createRow(r + 1);
                for (int c = 0; c < grid[r].length; c++) {
                    if (grid[r][c] != null) {
                        sheetRow.createCell(c + 1).setCellValue(grid[r][c]);
                    }
                }
            }
            try (OutputStream out = Files.newOutputStream(dir.resolve(name + ".xlsx"))) {
                workbook.write(out);
            }
        }
        return dir;
    }

    /**
     * Write a single-table workbook whose first row is one cell merged across every column.
     */
    private Path writeProjectWithMergedHeader(String name, String[][] grid) throws IOException {
        var dir = tempDir.resolve(name);
        Files.createDirectories(dir);
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet(name);
            for (int r = 0; r < grid.length; r++) {
                var sheetRow = sheet.createRow(r + 1);
                for (int c = 0; c < grid[r].length; c++) {
                    if (grid[r][c] != null) {
                        sheetRow.createCell(c + 1).setCellValue(grid[r][c]);
                    }
                }
            }
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, grid[0].length));
            try (OutputStream out = Files.newOutputStream(dir.resolve(name + ".xlsx"))) {
                workbook.write(out);
            }
        }
        return dir;
    }

    private static final Path FIXTURE = Path.of("test-resources/org/openl/rules/table/TableLogicTest.xlsx");

    @Test
    void updateDropsMergesRemovedFromTheSource(@TempDir Path projectDir) throws Exception {
        Files.copy(FIXTURE, projectDir.resolve("TableLogicTest.xlsx"));

        // Flatten a merged table and write it back (which saves the workbook to disk).
        var table = firstTableWithMerges(projectDir);
        var region = table.getGridTable().getRegion();
        assertTrue(mergesWithin(table.getGridTable().getGrid(), region) > 0,
                "fixture table must start with merged regions");
        new RawTableWriter(table).write(flatten(new RawTableReader().read(table)));

        // Re-open the saved project: a subsequent read must not see the dropped merges.
        var reloaded = tableWithin(projectDir, region);
        assertEquals(0, mergesWithin(reloaded.getGridTable().getGrid(), region),
                "merges dropped from the source must not linger after update");
    }

    /**
     * Replaces every cell with a plain, span-free cell so the written source carries no merge information.
     */
    private static RawTableView flatten(RawTableView view) {
        List<List<RawTableCell>> flat = new ArrayList<>();
        for (var row : view.source) {
            List<RawTableCell> flatRow = new ArrayList<>();
            for (var cell : row) {
                Object value = cell.value() != null ? cell.value() : "";
                flatRow.add(RawTableCell.builder().value(value).build());
            }
            flat.add(flatRow);
        }
        return RawTableView.builder()
                .name(view.name)
                .kind(view.kind)
                .source(flat)
                .build();
    }

    private static int mergesWithin(IGrid grid, IGridRegion region) {
        int count = 0;
        for (int i = 0; i < grid.getNumberOfMergedRegions(); i++) {
            var merged = grid.getMergedRegion(i);
            if (IGridRegion.Tool.contains(region, merged.getLeft(), merged.getTop())) {
                count++;
            }
        }
        return count;
    }

    private static IOpenLTable firstTableWithMerges(Path projectDir) throws Exception {
        for (IOpenLTable table : loadTables(projectDir)) {
            if (mergesWithin(table.getGridTable().getGrid(), table.getGridTable().getRegion()) > 0) {
                return table;
            }
        }
        throw new IllegalStateException("No table with merged regions found in the fixture");
    }

    private static IOpenLTable tableWithin(Path projectDir, IGridRegion region) throws Exception {
        for (IOpenLTable table : loadTables(projectDir)) {
            var r = table.getGridTable().getRegion();
            if (r.getTop() == region.getTop() && r.getLeft() == region.getLeft()) {
                return table;
            }
        }
        throw new IllegalStateException("Table not found after reload");
    }

    private static List<IOpenLTable> loadTables(Path projectDir) throws Exception {
        var module = ProjectResolver.getInstance().resolve(projectDir).getModules().getFirst();
        var projectModel = new ProjectModel(mock(WebStudio.class), null);
        projectModel.setModuleInfo(module);
        List<IOpenLTable> tables = new ArrayList<>();
        for (TableSyntaxNode tsn : projectModel.getAllTableSyntaxNodes()) {
            tables.add(new TableSyntaxNodeAdapter(tsn));
        }
        return tables;
    }

    private static IOpenLTable tableByHeader(Path projectDir, String header) throws Exception {
        for (IOpenLTable table : loadTables(projectDir)) {
            if (header.equals(readSource(table).getFirst().getFirst().value())) {
                return table;
            }
        }
        throw new IllegalStateException("No table with header " + header);
    }
}
