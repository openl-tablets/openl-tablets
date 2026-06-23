package org.openl.studio.projects.service.tables.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.service.tables.read.RawTableReader;

class RawTableWriterTest {

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

}
