package org.openl.studio.projects.service.tables.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.service.tables.TableTestProjects;
import org.openl.types.IOpenClass;

/**
 * Covers a table bound alone against its compiled module: it is laid out in the regions the compile of the module lays
 * it out in, and the module is left as compiled.
 */
class SingleTableBindTest {

    private static final Path REFERENCE_GUIDE = Path.of(
            "../../DEV/org.openl.rules.demo/src/org.openl.rules.demo.examples/Example-4-Reference-Guide");

    @TempDir
    Path dir;

    static Stream<Arguments> modules() throws IOException {
        try (var files = Files.walk(REFERENCE_GUIDE.resolve("rules"))) {
            return files.filter(file -> file.toString().endsWith(".xlsx"))
                    .map(file -> Arguments.of(REFERENCE_GUIDE.relativize(file).toString()))
                    .toList()
                    .stream();
        }
    }

    @ParameterizedTest
    @MethodSource("modules")
    void laysEveryTableBoundAloneOutAsTheCompileOfItsModuleDoes(String workbook) throws Exception {
        var model = compiled(workbook);
        var module = (XlsModuleOpenClass) model.getOpenedModuleCompiledOpenClass().getOpenClassWithErrors();
        var before = Snapshot.of(module);
        var differences = new ArrayList<String>();
        var bound = 0;
        for (var node : module.getXlsMetaInfo().getXlsModuleNode().getXlsTableSyntaxNodes()) {
            var table = new TableSyntaxNodeAdapter(node);
            if (!TableRegionLayout.formats(table)) {
                continue;
            }
            var grid = GridTableUtils.getOriginalTable(table.getGridTable());
            grid.edit();
            try {
                var compiledRegions = TableRegionLayout.of(table, grid);
                var boundRegions = TableRegionLayout.boundRegionsOf(grid, module, null);
                if (boundRegions != null) {
                    bound++;
                    if (!compiledRegions.equals(boundRegions)) {
                        differences.add(describe(node, compiledRegions, boundRegions));
                    }
                }
            } finally {
                grid.stopEditing();
            }
        }
        System.out.printf("%s: %d tables bound alone%n", workbook, bound);
        assertEquals(List.of(), differences);
        before.assertUnchangedIn(Snapshot.of(module));
    }

    private ProjectModel compiled(String workbook) throws Exception {
        var copy = dir.resolve("project");
        try (var files = Files.walk(REFERENCE_GUIDE)) {
            for (var file : files.toList()) {
                var target = copy.resolve(REFERENCE_GUIDE.relativize(file).toString());
                if (Files.isDirectory(file)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(file, target);
                }
            }
        }
        var project = ProjectResolver.getInstance().resolve(copy);
        var module = project.getModules()
                .stream()
                .filter(each -> each.getRulesPath().endsWith(Path.of(workbook).getFileName()))
                .findFirst()
                .orElseThrow();
        module.getWebstudioConfiguration().setCompileThisModuleOnly(true);
        var model = new ProjectModel(mock(WebStudio.class), null);
        model.setModuleInfo(module);
        assertNotNull(model.getOpenedModuleCompiledOpenClass());
        return model;
    }

    private static String describe(TableSyntaxNode node, TableRegions compiled, TableRegions bound) {
        return "%s%n  compiled: %s%n  bound:    %s".formatted(node.getDisplayName(), compiled, bound);
    }

    @Test
    void leavesTheModuleAsCompiledWhenATableWritingIntoItIsEdited() throws IOException {
        var model = TableTestProjects.projectModel(dir, "Rules", sheet -> {
            TableTestProjects.row(sheet, 0, 0, "Conditions");
            TableTestProjects.merge(sheet, 0, 0, 0, 3);
            TableTestProjects.row(sheet, 1, 0, "Inputs", "Expression", "Parameter", "Title");
            TableTestProjects.row(sheet, 2, 0, "Integer age", "age > min", "Integer min", "Min Age");
            TableTestProjects.row(sheet, 5, 0, "Constants");
            TableTestProjects.merge(sheet, 5, 5, 0, 2);
            TableTestProjects.row(sheet, 6, 0, "Double", "rate", "1.5");
            TableTestProjects.row(sheet, 9, 0, "Spreadsheet String Greet()");
            TableTestProjects.merge(sheet, 9, 9, 0, 1);
            TableTestProjects.row(sheet, 10, 0, "Step", "Value");
            TableTestProjects.row(sheet, 11, 0, "Text", "= msg(\"greeting\")");
            var top = 14;
            for (var name : List.of("SprA", "SprB", "SprC")) {
                TableTestProjects.row(sheet, top, 0, "Spreadsheet SpreadsheetResult " + name + "()");
                TableTestProjects.merge(sheet, top, top, 0, 1);
                TableTestProjects.row(sheet, top + 1, 0, "Step", "Value");
                TableTestProjects.row(sheet, top + 2, 0, "Step" + name, "= 1");
                top += 5;
            }
            TableTestProjects.row(sheet, top, 0, "Spreadsheet Object Pick(Boolean f)");
            TableTestProjects.merge(sheet, top, top, 0, 1);
            TableTestProjects.row(sheet, top + 1, 0, "Step", "Value");
            TableTestProjects.row(sheet, top + 2, 0, "Picked", "= f ? SprA() : SprB()");
        });
        var module = (XlsModuleOpenClass) model.getOpenedModuleCompiledOpenClass().getOpenClassWithErrors();
        var before = Snapshot.of(module);

        // A condition of another expression, and a constant of another name, as an edit writes them.
        bindEdited(model, module, "Conditions", 2, 1, "age >= min");
        bindEdited(model, module, "Constants", 1, 1, "fee");
        bindEdited(model, module, "Spreadsheet String Greet()", 2, 0, "Hello");
        // Results of two other types combined, which the module has not combined.
        bindEdited(model, module, "Spreadsheet Object Pick(Boolean f)", 2, 1, "= f ? SprA() : SprC()");

        before.assertUnchangedIn(Snapshot.of(module));
    }

    /** Writes a cell of a table, in table coordinates, and binds the table alone as a save of the edit does. */
    private static void bindEdited(ProjectModel model, XlsModuleOpenClass module, String header, int row, int column,
                                   String text) {
        var table = TableTestProjects.tableHeaded(model, header);
        var grid = GridTableUtils.getOriginalTable(table.getGridTable());
        grid.edit();
        try {
            ((IWritableGrid) grid.getGrid()).setCellValue(grid.getRegion().getLeft() + column,
                    grid.getRegion().getTop() + row, text);
            TableRegionLayout.boundRegionsOf(grid, module, null);
        } finally {
            grid.stopEditing();
        }
    }

    /**
     * What a bind could change in a module: its methods, types and fields, the fields of each of its types, its data
     * tables with their data, the definitions of its Conditions, Actions and Returns tables, the calls of
     * {@code msg} it keeps, and the {@code SpreadsheetResult} types it combined.
     */
    private record Snapshot(List<?> methods,
                            List<?> types,
                            List<?> fields,
                            Map<IOpenClass, Integer> typeFields,
                            Map<ITable, Object> tables,
                            List<?> definitions,
                            List<?> messageCalls,
                            List<?> combinedTypes) {

        static Snapshot of(XlsModuleOpenClass module) {
            var tables = new IdentityHashMap<ITable, Object>();
            module.getDataBase().getTables().forEach(table -> tables.put(table, table.getDataArray()));
            var typeFields = new IdentityHashMap<IOpenClass, Integer>();
            module.getTypes().forEach(type -> typeFields.put(type, type.getFields().size()));
            return new Snapshot(List.copyOf(module.getMethods()),
                    List.copyOf(module.getTypes()),
                    List.copyOf(module.getFields()),
                    typeFields,
                    tables,
                    List.copyOf(module.getXlsDefinitions().getDtColumnsDefinitions()),
                    List.copyOf(module.getMessageCalls()),
                    List.copyOf(module.getCombinedSpreadsheetResultOpenClasses()));
        }

        void assertUnchangedIn(Snapshot after) {
            assertSameElements(methods, after.methods, "The methods of the module");
            assertSameElements(types, after.types, "The types of the module");
            assertSameElements(fields, after.fields, "The fields of the module");
            assertEquals(typeFields, after.typeFields, "The fields of the types of the module");
            assertEquals(tables.keySet(), after.tables.keySet(), "The data tables of the module");
            tables.forEach((table, data) -> assertSame(data, after.tables.get(table),
                    "The data of " + table.getName()));
            assertSameElements(definitions, after.definitions, "The definitions of the module");
            assertSameElements(messageCalls, after.messageCalls, "The calls of msg the module keeps");
            assertEquals(combinedTypes.size(), after.combinedTypes.size(), "The types the module combined");
        }

        /** The same objects, in the same order: an object replaced by an equal one is a change too. */
        private static void assertSameElements(List<?> expected, List<?> actual, String what) {
            assertEquals(expected.size(), actual.size(), what);
            for (var i = 0; i < expected.size(); i++) {
                assertSame(expected.get(i), actual.get(i), what);
            }
        }
    }
}
