package org.openl.studio.projects.service.tables.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.RawTableSourceAction;
import org.openl.studio.projects.model.tables.RawTextMark;
import org.openl.studio.projects.model.tables.RawTextMarkColor;
import org.openl.studio.projects.model.tables.UpdateTarget;
import org.openl.studio.projects.service.tables.StyledRead.StyledRun;
import org.openl.studio.projects.service.tables.TableTestProjects;
import org.openl.studio.projects.service.tables.format.ExcelTableFormatter;

/**
 * The marks a cell update writes on the pieces of a text: bold in the colour of the mark, kept as rich text of the
 * cell and nowhere else in the workbook.
 *
 * @author Yury Molchan
 */
class TextMarksWriteTest {

    @TempDir
    Path tempDir;

    private Path project;

    @BeforeEach
    void setUp() throws IOException {
        project = TableTestProjects.writeProject(tempDir.resolve("main"), "main", "main", new String[][]{
                {"Datatype Greeting", null, null},
                {"String", "code", "alpha beta"},
                {"String", "text", "gamma"}
        });
    }

    @Test
    void writesAMarkedPieceBoldInTheColourOfItsMark() {
        apply(update(1, 2, "alpha beta", mark(6, 10, RawTextMarkColor.ERROR)));

        var runs = runsOf(1, 2);
        assertEquals(2, runs.size());
        assertEquals("alpha ", runs.get(0).text());
        assertNull(runs.get(0).style());
        assertEquals("beta", runs.get(1).text());
        assertEquals("#ff0000", runs.get(1).style().color());
        assertEquals(Boolean.TRUE, runs.get(1).style().bold());
    }

    @Test
    void writesEachColourInItsStandardColourOfExcel() {
        apply(update(1, 2, "alpha beta", mark(0, 2, RawTextMarkColor.WARNING), mark(2, 4,
                RawTextMarkColor.SUCCESS), mark(6, 10, RawTextMarkColor.INFO)));

        var runs = runsOf(1, 2);
        assertEquals("#ffc000", runs.get(0).style().color());
        assertEquals("#00b050", runs.get(1).style().color());
        assertNull(runs.get(2).style());
        assertEquals("#00b0f0", runs.get(3).style().color());
    }

    @Test
    void addsNoCellStyleNorFontToTheWorkbook() throws IOException {
        var before = stylesOf();

        apply(update(1, 2, "alpha beta", mark(0, 5, RawTextMarkColor.ERROR)));

        assertEquals(before, stylesOf());
    }

    @Test
    void marksANewValue() {
        apply(update(2, 2, "delta", mark(0, 5, RawTextMarkColor.SUCCESS)));

        var runs = runsOf(2, 2);
        assertEquals(1, runs.size());
        assertEquals("delta", runs.getFirst().text());
        assertEquals("#00b050", runs.getFirst().style().color());
    }

    @Test
    void keepsTheMarksWhenAnotherCellIsSaved() {
        apply(update(1, 2, "alpha beta", mark(0, 5, RawTextMarkColor.ERROR)));

        apply(new RawTableSourceAction.Update(new UpdateTarget.Cell(2, 2, "delta")));

        assertEquals("#ff0000", runsOf(1, 2).getFirst().style().color());
    }

    @Test
    void keepsTheMarksOfAValueWrittenAgainWithoutMarks() {
        apply(update(1, 2, "alpha beta", mark(0, 5, RawTextMarkColor.ERROR)));

        apply(new RawTableSourceAction.Update(new UpdateTarget.Cell(1, 2, "alpha beta")));

        assertEquals("#ff0000", runsOf(1, 2).getFirst().style().color());
    }

    @Test
    void writesANewValueWithoutMarksAsPlainText() {
        apply(update(1, 2, "alpha beta", mark(0, 5, RawTextMarkColor.ERROR)));

        apply(new RawTableSourceAction.Update(new UpdateTarget.Cell(1, 2, "alpha gamma")));

        assertNull(TableTestProjects.styledSource(load()).get(1).get(2).runs());
    }

    @Test
    void removesTheMarksWithAnEmptyList() {
        apply(update(1, 2, "alpha beta", mark(0, 5, RawTextMarkColor.ERROR)));

        apply(update(1, 2, "alpha beta"));

        assertNull(TableTestProjects.styledSource(load()).get(1).get(2).runs());
    }

    @Test
    void joinsAdjacentMarksOfOneColour() {
        apply(update(1, 2, "alpha beta", mark(0, 2, RawTextMarkColor.INFO), mark(2, 5, RawTextMarkColor.INFO)));

        var runs = runsOf(1, 2);
        assertEquals("alpha", runs.getFirst().text());
        assertEquals("#00b0f0", runs.getFirst().style().color());
    }

    @Test
    void keepsTheMarksWhenTheTableIsFormattedOnSave() {
        apply(update(1, 2, "alpha beta", mark(6, 10, RawTextMarkColor.ERROR)));

        // The first save with formatting on formats the whole table, the marked cell included.
        var writer = new RawTableWriter(load());
        writer.formatOnSave(true);
        writer.apply(new RawTableSourceAction.Update(new UpdateTarget.Cell(2, 2, "delta")));

        var marked = runsOf(1, 2).get(1);
        assertEquals("beta", marked.text());
        assertEquals("#ff0000", marked.style().color());
        assertEquals(Boolean.TRUE, marked.style().bold());
    }

    @Test
    void writesAMarkTheTableIsFormattedOnSaveWith() {
        var writer = new RawTableWriter(load());
        writer.formatOnSave(true);
        writer.apply(update(1, 2, "alpha beta", mark(0, 5, RawTextMarkColor.SUCCESS)));

        assertEquals("#00b050", runsOf(1, 2).getFirst().style().color());
    }

    @Test
    void keepsTheMarksWhenTheProjectIsReformatted() throws IOException {
        apply(update(1, 2, "alpha beta", mark(6, 10, RawTextMarkColor.INFO)));

        reformat();

        var marked = runsOf(1, 2).get(1);
        assertEquals("beta", marked.text());
        assertEquals("#00b0f0", marked.style().color());
        assertEquals(Boolean.TRUE, marked.style().bold());
        // Reformatted twice, the workbook stays as the first reformat left it: a mark adds nothing to its styles.
        var once = stylesOf();
        reformat();
        assertEquals(once, stylesOf());
        assertEquals(1, runsOf(1, 2).stream().filter(run -> run.style() != null).count(), "The mark is the one run");
    }

    /** Formats every table of the project and saves each workbook once, as Reformat Excel files does. */
    private void reformat() {
        var formatter = new ExcelTableFormatter();
        for (var node : TableTestProjects.projectModel(project).getAllTableSyntaxNodes()) {
            var grid = GridTableUtils.getOriginalTable(new TableSyntaxNodeAdapter(node).getGridTable());
            grid.edit();
            try {
                if (formatter.format(new TableSyntaxNodeAdapter(node), grid)) {
                    var sheet = (XlsSheetGridModel) grid.getGrid();
                    ExcelTableFormatter.removeUnusedStyles((XSSFWorkbook) sheet.getWorkbookToWrite());
                    TableWriter.saveWorkbook(sheet);
                }
            } finally {
                grid.stopEditing();
            }
        }
    }

    @Test
    void writesNoMarksOnAValueWrittenAsAnExcelFormula() {
        apply(update(2, 2, "=1+2", mark(0, 2, RawTextMarkColor.ERROR)));

        assertNull(runsOf(2, 2));
    }

    @Test
    void refusesMarksInAnXlsWorkbook() throws IOException {
        var xls = tempDir.resolve("xls");
        Files.createDirectories(xls);
        try (var workbook = new HSSFWorkbook(); var out = Files.newOutputStream(xls.resolve("main.xls"))) {
            var sheet = workbook.createSheet("main");
            sheet.createRow(1).createCell(1).setCellValue("Datatype Greeting");
            var field = sheet.createRow(2);
            field.createCell(1).setCellValue("String");
            field.createCell(2).setCellValue("code");
            field.createCell(3).setCellValue("alpha beta");
            workbook.write(out);
        }
        var writer = new RawTableWriter(load(xls));

        var refused = assertThrows(BadRequestException.class,
                () -> writer.apply(update(1, 2, "alpha beta", mark(0, 5, RawTextMarkColor.ERROR))));

        assertEquals("openl.error.400.table.action.marks.xls.message", refused.getErrorCode());
    }

    @Test
    void refusesMarksOnTheHeader() {
        assertRefused(update(0, 0, "Datatype Greeting", mark(0, 8, RawTextMarkColor.ERROR)),
                "openl.error.400.table.action.marks.header.message");
    }

    @Test
    void refusesMarksOnAValueThatIsNotAText() {
        assertRefused(update(1, 2, 42, mark(0, 1, RawTextMarkColor.ERROR)),
                "openl.error.400.table.action.marks.not-text.message");
    }

    @Test
    void refusesMarksOutOfTheText() {
        assertRefused(update(1, 2, "alpha", mark(3, 9, RawTextMarkColor.ERROR)),
                "openl.error.400.table.action.marks.invalid.message");
    }

    @Test
    void refusesMarksThatOverlap() {
        assertRefused(update(1, 2, "alpha beta", mark(0, 4, RawTextMarkColor.ERROR),
                mark(2, 6, RawTextMarkColor.INFO)), "openl.error.400.table.action.marks.invalid.message");
    }

    private void assertRefused(RawTableSourceAction action, String code) {
        var refused = assertThrows(BadRequestException.class, () -> apply(action));
        assertEquals(code, refused.getErrorCode());
    }

    private void apply(RawTableSourceAction action) {
        new RawTableWriter(load()).apply(action);
    }

    private List<StyledRun> runsOf(int row, int column) {
        return TableTestProjects.styledSource(load()).get(row).get(column).runs();
    }

    /** The number of cell formats, named styles and fonts of the workbook. */
    private List<Integer> stylesOf() throws IOException {
        try (var in = Files.newInputStream(project.resolve("main.xlsx")); var workbook = new XSSFWorkbook(in)) {
            var styles = workbook.getStylesSource();
            var named = styles.getCTStylesheet().getCellStyles();
            return List.of(styles.getNumCellStyles(), named == null ? 0 : named.sizeOfCellStyleArray(),
                    styles.getFonts().size());
        }
    }

    private IOpenLTable load() {
        return load(project);
    }

    private static IOpenLTable load(Path project) {
        return TableTestProjects.projectModel(project).getAllTableSyntaxNodes().stream()
                .map(TableSyntaxNodeAdapter::new)
                .filter(table -> table.getGridTable(IXlsTableNames.VIEW_DEVELOPER) != null)
                .findFirst()
                .orElseThrow();
    }

    private static RawTableSourceAction update(int row, int column, Object value, RawTextMark... marks) {
        return new RawTableSourceAction.Update(new UpdateTarget.Cell(row, column, value, List.of(marks)));
    }

    private static RawTextMark mark(int from, int to, RawTextMarkColor color) {
        return new RawTextMark(from, to, color);
    }
}
