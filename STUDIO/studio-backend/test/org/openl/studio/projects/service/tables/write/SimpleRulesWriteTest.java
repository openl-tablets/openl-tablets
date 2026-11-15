package org.openl.studio.projects.service.tables.write;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.openl.studio.projects.service.tables.TableTestProjects.merge;
import static org.openl.studio.projects.service.tables.TableTestProjects.row;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.studio.projects.model.tables.SimpleRulesView;
import org.openl.studio.projects.service.tables.TableTestProjects;
import org.openl.studio.projects.service.tables.read.SimpleRulesTableReader;

/**
 * A SimpleRules table written back with rules that no longer share a merged value.
 *
 * @author Yury Molchan
 */
class SimpleRulesWriteTest {

    @TempDir
    Path tempDir;

    @Test
    void splitsAMergedValueTheRulesNoLongerShare() throws Exception {
        var project = project("split");

        write(project, rule(1, 10, "aaa"), rule(2, 20, "bbb"), rule(3, 30, "ccc"));

        assertEquals(List.of(
                cells("SimpleRules String myRule(int a, int b)", null, null),
                cells("a", "b", "res"),
                cells("1", "10", "aaa"),
                cells("2", "20", "bbb"),
                cells("3", "30", "ccc")), TableTestProjects.rawSource(TableTestProjects.onlyTable(project)));
        assertEquals("[{a=1, b=10, res=aaa}, {a=2, b=20, res=bbb}, {a=3, b=30, res=ccc}]", read(project));
    }

    @Test
    void keepsAMergedValueTheRulesStillShare() throws Exception {
        var project = project("kept");

        write(project, rule(1, 10, "xxx"), rule(1, 20, "yyy"), rule(2, 30, "zzz"));

        assertEquals("[{a=1, b=10, res=xxx}, {a=1, b=20, res=yyy}, {a=2, b=30, res=zzz}]", read(project));
        assertEquals(1, TableTestProjects.onlyTable(project).getGridTable().getGrid().getNumberOfMergedRegions());
    }

    /** {@code myRule} with the value {@code a = 1} merged over its first two rules. */
    private Path project(String name) throws Exception {
        var dir = Files.createDirectories(tempDir.resolve(name));
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Rules");
            row(sheet, 0, 0, "SimpleRules String myRule(int a, int b)");
            row(sheet, 1, 0, "a", "b", "res");
            row(sheet, 2, 0, "1", "10", "aaa");
            row(sheet, 3, 0, "", "20", "bbb");
            row(sheet, 4, 0, "2", "30", "ccc");
            merge(sheet, 2, 3, 0, 0);
            try (OutputStream out = Files.newOutputStream(dir.resolve(name + ".xlsx"))) {
                workbook.write(out);
            }
        }
        return dir;
    }

    @SafeVarargs
    private static void write(Path project, LinkedHashMap<String, Object>... rules) {
        var table = TableTestProjects.onlyTable(project);
        var view = new SimpleRulesTableReader().read(table);
        new SimpleRulesWriter(table).write(SimpleRulesView.builder()
                .name(view.name)
                .returnType(view.returnType)
                .args(view.args)
                .headers(view.headers)
                .rules(Arrays.asList(rules))
                .build());
    }

    private static String read(Path project) {
        return new SimpleRulesTableReader().read(TableTestProjects.onlyTable(project)).rules.toString();
    }

    private static LinkedHashMap<String, Object> rule(int a, int b, String res) {
        var rule = new LinkedHashMap<String, Object>();
        rule.put("a", a);
        rule.put("b", b);
        rule.put("res", res);
        return rule;
    }

    private static List<String> cells(String... cells) {
        return Arrays.asList(cells);
    }
}
