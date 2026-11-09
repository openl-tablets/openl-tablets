package org.openl.studio.projects.service.tables.write;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.sun.management.ThreadMXBean;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.rules.lang.xls.SingleTableBinder;
import org.openl.rules.lang.xls.syntax.TableIds;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.ReadOnceSheetGrid;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.model.tables.RawTableSourceAction;
import org.openl.studio.projects.model.tables.UpdateTarget;
import org.openl.studio.projects.service.tables.format.TableRegionLayout;

/**
 * Measures the cost of binding an edited table alone on large Data and decision tables, against the save it is part
 * of. Runs only with {@code -DbigTables=true} and a large heap.
 */
@EnabledIfSystemProperty(named = "bigTables", matches = "true")
class BigTablesBindPerfTest {

    private static final String TYPES = "Types";
    private static final String BODY = "Body";
    private static final int RUNS = 5;
    private static final int SAVES = 3;

    /**
     * A table to measure: its sheets, a value edit, and an edit of a title, which has the save bind the table.
     */
    record Case(String table, Consumer<Sheet> types, Consumer<Sheet> body, int editRow, int editColumn,
                String editValue, int titleRow, int titleColumn) {
    }

    static Stream<Arguments> cases() {
        var only = System.getProperty("bigTables.only");
        return Stream.of(
                Arguments.of(Named.of("Data 10 x 100,000", dataCase(10, 100_000))),
                Arguments.of(Named.of("Data 100 x 10,000", dataCase(100, 10_000))),
                Arguments.of(Named.of("SmartRules 4 conditions x 100,000", smartCase(100_000))),
                Arguments.of(Named.of("Rules 3 conditions x 100,000", rulesCase(100_000))),
                Arguments.of(Named.of("SmartRules 20 conditions x 10,000", wideSmartCase(20, 10_000))))
                .filter(each -> only == null
                        || ((Named<?>) each.get()[0]).getName().replace(" ", "_").startsWith(only));
    }

    @ParameterizedTest
    @MethodSource("cases")
    void measures(Case scenario, @TempDir Path dir) throws IOException {
        write(dir, scenario);
        var started = System.nanoTime();
        var model = compile(dir);
        var compileMs = ms(started);
        var table = table(model, scenario.table());
        var module = model.compiledModuleOf(table.getSyntaxNode());

        // The layout of the compile is laid out once and kept for the compiled table.
        var t = System.nanoTime();
        var compiledRegions = TableRegionLayout.regionsOf(table);
        var compiledLayoutMs = ms(t);
        var probe = new ArrayList<Double>();
        var bind = new ArrayList<Double>();
        var bindAndLayout = new ArrayList<Double>();
        var allocated = new ArrayList<Double>();
        var grid = GridTableUtils.getOriginalTable(table.getGridTable());
        grid.edit();
        try {
            for (var run = 0; run <= RUNS; run++) {
                var bytes = allocatedBytes();
                var t0 = System.nanoTime();
                var read = new GridTable(grid.getRegion(),
                        new ReadOnceSheetGrid(((XlsSheetGridModel) grid.getGrid()).getSheetSource()));
                SingleTableBinder.bind(TableIds.nodeOf(read), module);
                var t1 = System.nanoTime();
                var mb = (allocatedBytes() - bytes) / 1e6;
                TableRegionLayout.boundRegionsOf(grid, module, null);
                var t2 = System.nanoTime();
                TableRegionLayout.boundRegionsOf(grid, module, compiledRegions);
                var t3 = System.nanoTime();
                if (run > 0) {
                    bind.add((t1 - t0) / 1e6);
                    bindAndLayout.add((t2 - t1) / 1e6);
                    probe.add((t3 - t2) / 1e6);
                    allocated.add(mb);
                }
            }
        } finally {
            grid.stopEditing();
        }

        // The first save formats the table whole; the ones measured format what an edit touches.
        save(compile(dir), scenario, false, scenario.editRow(), scenario.editColumn(), "warm");
        var off = new ArrayList<Double>();
        var on = new ArrayList<Double>();
        var titleOff = new ArrayList<Double>();
        var titleOn = new ArrayList<Double>();
        for (var run = 0; run < SAVES; run++) {
            off.add(save(compile(dir), scenario, false, scenario.editRow(), scenario.editColumn(),
                    scenario.editValue() + run));
            on.add(save(compile(dir), scenario, true, scenario.editRow(), scenario.editColumn(),
                    scenario.editValue() + run + "b"));
            titleOff.add(save(compile(dir), scenario, false, scenario.titleRow(), scenario.titleColumn(),
                    "Title " + run));
            titleOn.add(save(compile(dir), scenario, true, scenario.titleRow(), scenario.titleColumn(),
                    "Title " + run + "b"));
        }
        System.out.printf(
                """
                %n### %s: compile %.0f ms | layout of the compile %.0f ms | bind alone %.0f ms (%.0f MB allocated) |\
                 bind and layout %.0f ms | probe %.0f ms | value save: shifted %.0f ms, bound %.0f ms |\
                 title save: shifted %.0f ms, bound %.0f ms%n""",
                scenario.table(), compileMs, compiledLayoutMs, median(bind), median(allocated),
                median(bindAndLayout), median(probe), median(off), median(on), median(titleOff), median(titleOn));
    }

    private static double save(ProjectModel model, Case scenario, boolean bind, int row, int column, String value) {
        var table = table(model, scenario.table());
        var writer = new RawTableWriter(table);
        writer.formatOnSave(true);
        writer.setCompiledModule(bind ? model.compiledModuleOf(table.getSyntaxNode()) : null);
        var started = System.nanoTime();
        writer.apply(List.of(new RawTableSourceAction.Update(
                new UpdateTarget.Cell(row, column, value))));
        return ms(started);
    }

    private static long allocatedBytes() {
        var threads = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        return threads.getCurrentThreadAllocatedBytes();
    }

    private static double ms(long started) {
        return (System.nanoTime() - started) / 1e6;
    }

    private static double median(List<Double> values) {
        var sorted = values.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        return sorted[sorted.length / 2];
    }

    private static IOpenLTable table(ProjectModel model, String name) {
        return model.getAllTableSyntaxNodes()
                .stream()
                .map(TableSyntaxNodeAdapter::new)
                .filter(each -> name.equals(each.getName()))
                .findFirst()
                .orElseThrow();
    }

    private static ProjectModel compile(Path dir) {
        try {
            var module = ProjectResolver.getInstance().resolve(dir).getModules().getFirst();
            module.getWebstudioConfiguration().setCompileThisModuleOnly(true);
            var model = new ProjectModel(mock(WebStudio.class), null);
            model.setModuleInfo(module);
            return model;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void write(Path dir, Case scenario) throws IOException {
        Files.createDirectories(dir);
        try (var workbook = new SXSSFWorkbook(1000)) {
            scenario.types().accept(workbook.createSheet(TYPES));
            scenario.body().accept(workbook.createSheet(BODY));
            try (var out = Files.newOutputStream(dir.resolve("Rules.xlsx"))) {
                workbook.write(out);
            }
        }
    }

    private static void row(Sheet sheet, int index, Object... values) {
        var row = sheet.createRow(index);
        for (var column = 0; column < values.length; column++) {
            var cell = row.createCell(column);
            if (values[column] instanceof Number number) {
                cell.setCellValue(number.doubleValue());
            } else if (values[column] != null) {
                cell.setCellValue(values[column].toString());
            }
        }
    }

    /** A Data table of a datatype of the given number of fields: texts, integers and numbers in turn. */
    private static Case dataCase(int fields, int rows) {
        Consumer<Sheet> types = sheet -> {
            row(sheet, 0, "Datatype Row" + fields);
            for (var field = 0; field < fields; field++) {
                row(sheet, field + 1, fieldType(field), "f" + field);
            }
        };
        Consumer<Sheet> body = sheet -> {
            row(sheet, 0, "Data Row" + fields + " rows" + fields);
            row(sheet, 1, Stream.iterate(0, i -> i + 1).limit(fields).map(i -> "f" + i).toArray());
            row(sheet, 2, Stream.iterate(0, i -> i + 1).limit(fields).map(i -> "F" + i).toArray());
            for (var r = 0; r < rows; r++) {
                var values = new Object[fields];
                for (var field = 0; field < fields; field++) {
                    values[field] = switch (field % 3) {
                        case 0 -> "v" + r;
                        case 1 -> r % 1000;
                        default -> r * 0.5;
                    };
                }
                row(sheet, r + 3, values);
            }
        };
        return new Case("rows" + fields, types, body, 2 + rows / 2, 0, "edited", 2, 0);
    }

    private static String fieldType(int field) {
        return switch (field % 3) {
            case 0 -> "String";
            case 1 -> "Integer";
            default -> "Double";
        };
    }

    /** A SmartRules table of four conditions: a text, a range of ages, a number and a text. */
    private static Case smartCase(int rules) {
        Consumer<Sheet> body = sheet -> {
            row(sheet, 0, "SmartRules Double Rate(String region, Integer age, Double amount, String product)");
            row(sheet, 1, "Region", "Age", "Amount", "Product", "Rate");
            for (var r = 0; r < rules; r++) {
                row(sheet, r + 2, "R" + (r % 50), (r % 90) + "-" + (r % 90 + 9), "> " + (r % 1000), "P" + (r % 7),
                        r * 0.01);
            }
        };
        return new Case("Rate", sheet -> row(sheet, 0, "Notes"), body, 1 + rules / 2, 4, "1.5", 1, 4);
    }

    /** A SmartRules table of the given number of text conditions. */
    private static Case wideSmartCase(int conditions, int rules) {
        var parameters = new ArrayList<String>();
        var titles = new ArrayList<Object>();
        for (var c = 0; c < conditions; c++) {
            parameters.add("String p" + c);
            titles.add("P" + c);
        }
        titles.add("Result");
        Consumer<Sheet> body = sheet -> {
            row(sheet, 0, "SmartRules Double Wide(" + String.join(", ", parameters) + ")");
            row(sheet, 1, titles.toArray());
            for (var r = 0; r < rules; r++) {
                var values = new Object[conditions + 1];
                for (var c = 0; c < conditions; c++) {
                    values[c] = "v" + ((r + c) % 30);
                }
                values[conditions] = r * 0.01;
                row(sheet, r + 2, values);
            }
        };
        return new Case("Wide", sheet -> row(sheet, 0, "Notes"), body, 1 + rules / 2, conditions, "1.5", 1,
                conditions);
    }

    /** A Rules table of three conditions and a return, written with its declarations. */
    private static Case rulesCase(int rules) {
        Consumer<Sheet> body = sheet -> {
            row(sheet, 0, "Rules Double Factor(String code, Integer level, Double amount)");
            row(sheet, 1, "C1", "C2", "C3", "RET1");
            row(sheet, 2, "code == c", "level >= l", "amount < a", "f");
            row(sheet, 3, "String c", "Integer l", "Double a", "Double f");
            row(sheet, 4, "Code", "Level", "Amount", "Factor");
            for (var r = 0; r < rules; r++) {
                row(sheet, r + 5, "C" + (r % 100), r % 50, (double) (r % 1000), r * 0.01);
            }
        };
        return new Case("Factor", sheet -> row(sheet, 0, "Notes"), body, 4 + rules / 2, 3, "1.5", 4, 3);
    }
}
