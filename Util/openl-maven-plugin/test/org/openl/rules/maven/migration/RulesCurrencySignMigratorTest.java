package org.openl.rules.maven.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.rules.maven.migration.RulesProjects.compile;
import static org.openl.rules.maven.migration.RulesProjects.context;
import static org.openl.rules.maven.migration.RulesProjects.engine;
import static org.openl.rules.maven.migration.RulesProjects.errors;
import static org.openl.rules.maven.migration.RulesProjects.read;
import static org.openl.rules.maven.migration.RulesProjects.row;
import static org.openl.rules.maven.migration.RulesProjects.writeProject;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;

class RulesCurrencySignMigratorTest {

    @Test
    void removesSignFromNumbers(@TempDir Path folder) throws Exception {
        var workbook = writeProject(folder, List.of(
                List.of(row("Method Integer twice()"), row("return $600 * 2;")),
                List.of(row("Method Double plus(Double x)"), row("return $2,500 + $1.5K + x;")),
                List.of(row("Spreadsheet SpreadsheetResult fee(Integer x)"),
                        row("Step", "Value"),
                        row("Fee", "= $100 + x")),
                List.of(row("Data IntRange ints"), row("this"), row("Range"), row(">= $2,500"), row("[$1K .. $10K)")),
                List.of(row("Data DoubleRange doubles"), row("this"), row("Range"), row("[$1.5; $3,000.25]")),
                List.of(row("Data String notes"), row("this"), row("Note"), row("$5 fee"))));

        var changed = new RulesCurrencySignMigrator().migrate(folder, context(folder));

        assertEquals(List.of(workbook.toRealPath()), changed);
        assertEquals(List.of(
                "Method Integer twice()",
                "return 600 * 2;",
                "",
                "Method Double plus(Double x)",
                "return 2,500 + 1.5K + x;",
                "",
                "Spreadsheet SpreadsheetResult fee(Integer x)",
                "Step|Value",
                "Fee|= 100 + x",
                "",
                "Data IntRange ints",
                "this",
                "Range",
                ">= 2500",
                "[1K .. 10K)",
                "",
                "Data DoubleRange doubles",
                "this",
                "Range",
                "[1.5; 3000.25]",
                "",
                "Data String notes",
                "this",
                "Note",
                "$5 fee"), read(workbook));
        assertEquals(List.of(), errors(compile(folder)));

        var factory = engine(folder);
        var rules = factory.newInstance();
        var type = factory.getInterfaceClass();
        assertEquals(1200, type.getMethod("twice").invoke(rules));
        assertEquals(4001.0, type.getMethod("plus", Double.class).invoke(rules, 1.0));
        var ints = (IntRange[]) type.getMethod("getInts").invoke(rules);
        assertTrue(ints[0].contains(2500));
        assertFalse(ints[0].contains(2499));
        assertTrue(ints[1].contains(9999));
        assertFalse(ints[1].contains(10000));
        var doubles = (DoubleRange[]) type.getMethod("getDoubles").invoke(rules);
        assertTrue(doubles[0].contains(3000.25));
        assertFalse(doubles[0].contains(1.4));
    }

    @Test
    void leavesNamesThatAreNotNumbers(@TempDir Path folder) throws Exception {
        var workbook = writeProject(folder, List.of(
                List.of(row("Method Integer premium()"), row("return $Premium * 2;"))));

        var changed = new RulesCurrencySignMigrator().migrate(folder, context(folder));

        assertEquals(List.of(), changed);
        assertEquals(List.of("Method Integer premium()", "return $Premium * 2;"), read(workbook));
    }
}
