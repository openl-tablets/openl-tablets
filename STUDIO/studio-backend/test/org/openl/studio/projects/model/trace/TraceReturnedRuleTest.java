package org.openl.studio.projects.model.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.studio.projects.service.trace.CallNode;
import org.openl.studio.projects.service.trace.DebugListener;
import org.openl.studio.projects.service.trace.TraceDebugger;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * Verifies that the trace names the rule a decision table returned, not every rule whose conditions hold.
 *
 * @author Yury Molchan
 */
class TraceReturnedRuleTest {

    @Test
    void namesOnlyTheRuleThatReturnedWhenSeveralMatch(@TempDir Path dir) throws Exception {
        var workbook = dir.resolve("Rules.xlsx");
        try (var book = new XSSFWorkbook()) {
            var sheet = book.createSheet("Rules");
            row(sheet, 0, "Rules Integer pick(Integer a)");
            row(sheet, 1, "C1", "RET1");
            row(sheet, 2, "a > min", "result");
            row(sheet, 3, "Integer min", "Integer result");
            row(sheet, 4, "Min", "Result");
            row(sheet, 5, "0", "7");
            row(sheet, 6, "1", "8");
            try (OutputStream out = Files.newOutputStream(workbook)) {
                book.write(out);
            }
        }
        var compiled = new RulesEngineFactory<>(workbook.toString()).getCompiledOpenClass();
        var module = compiled.getOpenClass();
        var pick = module.getMethod("pick", new IOpenClass[]{JavaOpenClass.getOpenClass(Integer.class)});

        var debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.start("returned-rule-test", compiled.getClassLoader(), false, true, false, () -> {
            var env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(debugger.tracer());
            assertEquals(7, pick.invoke(module.newInstance(env), new Object[]{2}, env));
        });
        try {
            assertEquals(DebugStatus.COMPLETED, debugger.awaitInitialHalt(20_000));
            var table = find(debugger.completedTree());
            assertNotNull(table, "the run calls the pick decision table");
            var returned = table.steps().stream().filter(s -> s.decision() == DecisionRow.RETURNED).toList();
            assertEquals(1, returned.size());
            assertEquals("Returned rule: [R1]", returned.getFirst().label());
        } finally {
            debugger.terminate(20_000);
        }
    }

    private static @Nullable CallNode find(@Nullable CallNode node) {
        if (node == null || node.kind() == FrameKind.DECISION_TABLE) {
            return node;
        }
        for (var step : node.steps()) {
            for (var child : step.children()) {
                var found = find(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void row(Sheet sheet, int index, String... values) {
        var row = sheet.createRow(index);
        for (var i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }
}
