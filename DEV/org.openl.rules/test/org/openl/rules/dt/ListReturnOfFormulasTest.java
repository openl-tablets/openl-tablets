package org.openl.rules.dt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.TestUtils;
import org.openl.rules.runtime.RulesEngineFactory;

/**
 * Verifies that a rule returning a List gives a List when the values of a rule are formulas in several rows.
 *
 * @author Yury Molchan
 */
class ListReturnOfFormulasTest {

    @Test
    void returnsTheListTheRuleDeclares(@TempDir Path dir) throws Exception {
        var workbook = dir.resolve("Rules.xlsx");
        try (var book = new XSSFWorkbook()) {
            var sheet = book.createSheet("Rules");
            row(sheet, 0, "SmartRules List pick(Integer a)");
            row(sheet, 1, "a", "Result");
            row(sheet, 2, "100", "=a + 1");
            row(sheet, 3, null, "=a + 2");
            sheet.addMergedRegion(new CellRangeAddress(2, 3, 0, 0));
            row(sheet, 4, "200", "=a + 3");
            row(sheet, 5, null, "=a + 4");
            sheet.addMergedRegion(new CellRangeAddress(4, 5, 0, 0));
            try (OutputStream out = Files.newOutputStream(workbook)) {
                book.write(out);
            }
        }
        var factory = new RulesEngineFactory<>(workbook.toString());
        assertFalse(factory.getCompiledOpenClass().hasErrors(),
                () -> factory.getCompiledOpenClass().getAllMessages().toString());
        var rules = factory.newEngineInstance();

        assertEquals(List.of(101, 102), assertInstanceOf(List.class, TestUtils.invoke(rules, "pick", 100)));
        assertEquals(List.of(203, 204), assertInstanceOf(List.class, TestUtils.invoke(rules, "pick", 200)));
    }

    private static void row(Sheet sheet, int index, String... values) {
        var row = sheet.createRow(index);
        for (var i = 0; i < values.length; i++) {
            if (values[i] != null) {
                row.createCell(i).setCellValue(values[i]);
            }
        }
    }
}
