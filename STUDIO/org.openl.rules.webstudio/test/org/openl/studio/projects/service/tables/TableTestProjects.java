package org.openl.studio.projects.service.tables;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.service.tables.read.RawTableReader;

/**
 * Single-module projects on disk for the table reader and writer tests.
 *
 * <p>A writer saves the workbook, which leaves the in-memory grid it was built from stale — so a test writes a
 * project here, acts on it, and reads it back through a fresh {@link #projectModel}.
 *
 * <p>Public because the writer tests live in a subpackage.
 */
public final class TableTestProjects {

    private TableTestProjects() {
    }

    /** Write a single-sheet workbook holding one table that starts at cell B2. {@code null} cells stay blank. */
    public static Path writeProject(Path dir, String name, String sheetName, String[][] grid) throws IOException {
        return writeProject(dir, name, sheetName, grid, false);
    }

    /** The same, with the first row written as one cell merged across every column. */
    public static Path writeProjectWithMergedHeader(Path dir, String name, String sheetName, String[][] grid)
            throws IOException {
        return writeProject(dir, name, sheetName, grid, true);
    }

    private static Path writeProject(Path dir, String name, String sheetName, String[][] grid, boolean mergeHeaderRow)
            throws IOException {
        Files.createDirectories(dir);
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet(sheetName);
            for (var r = 0; r < grid.length; r++) {
                var sheetRow = sheet.createRow(r + 1);
                for (var c = 0; c < grid[r].length; c++) {
                    if (grid[r][c] != null) {
                        sheetRow.createCell(c + 1).setCellValue(grid[r][c]);
                    }
                }
            }
            if (mergeHeaderRow) {
                sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, grid[0].length));
            }
            try (OutputStream out = Files.newOutputStream(dir.resolve(name + ".xlsx"))) {
                workbook.write(out);
            }
        }
        return dir;
    }

    /** Resolve and compile the single-module project at {@code dir}. */
    public static ProjectModel projectModel(Path dir) {
        try {
            var modules = ProjectResolver.getInstance().resolve(dir).getModules();
            var projectModel = new ProjectModel(mock(WebStudio.class), null);
            projectModel.setModuleInfo(modules.getFirst());
            return projectModel;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve project at " + dir, e);
        }
    }

    /**
     * A writable grid over one sheet of the project, the way the create endpoint builds it.
     *
     * <p>The resolved project only reads its workbook, so a test that writes a new table needs this grid rather
     * than the one the existing tables were read from.
     */
    public static XlsSheetGridModel sheetGrid(Path project, String sheetName) {
        return new TableCreatorService(null, null, null, null, null)
                .sheetGridModel(projectModel(project), sheetName);
    }

    /** The table's cells as plain text, row by row, as they sit on the sheet. */
    public static List<List<String>> rawSource(IOpenLTable table) {
        var rows = new ArrayList<List<String>>();
        for (var row : new RawTableReader().read(table).source) {
            rows.add(row.stream().map(cell -> cell.value() == null ? null : String.valueOf(cell.value())).toList());
        }
        return rows;
    }

}
