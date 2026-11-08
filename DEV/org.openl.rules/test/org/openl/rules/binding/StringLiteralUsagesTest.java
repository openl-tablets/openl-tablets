package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.binding.impl.StringLiteralUsage;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.java.JavaOpenClass;

class StringLiteralUsagesTest {

    @TempDir
    Path folder;

    @Test
    void marksEveryTextInQuotesOfACellWithoutItsQuotes() throws IOException {
        var table = compile("= msg(\"greeting\", \"Ann\")", "= msg(true ? \"order.new\" : \"order.old\")", "= 1 + 2");

        assertEquals(List.of("greeting", "Ann"), literalsOf(table, 2));
        // Each text a choice may give is one, so a screen can lead from either to its message.
        assertEquals(List.of("order.new", "order.old"), literalsOf(table, 3));
        assertEquals(List.of(), literalsOf(table, 4));
    }

    @Test
    void aTextInQuotesAloneIsNoReferenceForTheEditorOfTheCell() {
        var literal = new StringLiteralUsage(1, 5, "Gold", mock(ISyntaxNode.class));
        var rule = new SimpleNodeUsage(0, 3, "Rule", null, NodeType.RULE);

        // The cell keeps the editor of its type: the text is told only so a screen can mark a key of a message.
        assertFalse(CellMetaInfo.isCellContainsNodeUsages(cellUsing(literal)));
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(cellUsing(literal, rule)));
    }

    private static CellMetaInfo cellUsing(NodeUsage... usages) {
        return new CellMetaInfo(JavaOpenClass.STRING, false, List.of(usages));
    }

    /** The texts in quotes of the formula of a step, each read from the cell over the piece its usage covers. */
    private static List<String> literalsOf(TableSyntaxNode table, int row) {
        var cell = table.getGridTable().getCell(1, row);
        var metaInfo = table.getMetaInfoReader().getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
        assertNotNull(metaInfo);
        var usages = metaInfo.getUsedNodes() == null ? List.<StringLiteralUsage>of() : metaInfo.getUsedNodes()
                .stream()
                .filter(StringLiteralUsage.class::isInstance)
                .map(StringLiteralUsage.class::cast)
                .toList();
        var text = cell.getStringValue();
        usages.forEach(usage -> assertEquals(usage.getValue(), text.substring(usage.getStart(), usage.getEnd())));
        return usages.stream().map(StringLiteralUsage::getValue).toList();
    }

    /** The one Spreadsheet of a module, whose steps hold the formulas. */
    private TableSyntaxNode compile(String... formulas) throws IOException {
        var file = folder.resolve("Literals.xlsx");
        try (var workbook = new XSSFWorkbook(); var out = Files.newOutputStream(file)) {
            var sheet = workbook.createSheet("Literals");
            sheet.createRow(0).createCell(0).setCellValue("Spreadsheet SpreadsheetResult Literals()");
            var header = sheet.createRow(1);
            header.createCell(0).setCellValue("Step");
            header.createCell(1).setCellValue("Value");
            for (var i = 0; i < formulas.length; i++) {
                var row = sheet.createRow(i + 2);
                row.createCell(0).setCellValue("Step" + i);
                row.createCell(1).setCellValue(formulas[i]);
            }
            workbook.write(out);
        }
        var compiled = new RulesEngineFactory<>(file.toUri().toURL()).getCompiledOpenClass();
        return ((XlsMetaInfo) compiled.getOpenClassWithErrors().getMetaInfo()).getXlsModuleNode()
                .getXlsTableSyntaxNodes()[0];
    }
}
