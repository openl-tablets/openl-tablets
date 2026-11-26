package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.rules.TestUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.table.ICell;
import org.openl.types.IOpenClass;

class DeclaredTypeHintsTest {

    private static final String COMBINED_TYPE = "SRsprA & SRsprB";

    @TempDir
    Path folder;

    private IOpenClass module;

    @BeforeEach
    void compile() throws IOException {
        var file = folder.resolve("Declarations.xlsx");
        try (var workbook = new XSSFWorkbook(); var out = Files.newOutputStream(file)) {
            var sheet = workbook.createSheet("Declarations");
            String[][] rows = {
                    {"Spreadsheet SpreadsheetResult sprA()"}, {"Step", "Value"}, {"Step1", "= 1"}, {},
                    {"Spreadsheet SpreadsheetResult sprB()"}, {"Step", "Value"}, {"Step2", "= 2"}, {},
                    {"Method Integer primitive()"}, {"int count = 1; return count;"}, {},
                    {"Method Object combined()"},
                    {COMBINED_TYPE + " x = (" + COMBINED_TYPE + ") sprA(); return x;"}};
            for (var r = 0; r < rows.length; r++) {
                var row = sheet.createRow(r);
                for (var c = 0; c < rows[r].length; c++) {
                    row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            workbook.write(out);
        }
        var compiled = new RulesEngineFactory<>(file.toUri().toURL()).getCompiledOpenClass();
        assertTrue(compiled.getAllMessages().isEmpty(), () -> compiled.getAllMessages().toString());
        module = compiled.getOpenClassWithErrors();
    }

    @Test
    void describesAPrimitiveTypeOfADeclaredVariableWithoutALink() {
        var table = TestUtils.findTable(module, "Method Integer primitive()");

        var type = usagesOf(table).stream()
                .filter(usage -> usage.getStart() == 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No usage starts at 0 in " + formulaOf(table)));

        assertEquals("primitive int", type.getDescription());
        assertEquals(3, type.getEnd());
        assertEquals(NodeType.OTHER, type.getNodeType());
        assertNull(type.getUri());
    }

    @Test
    void leavesACombinedTypeOfADeclaredVariableWithoutALink() {
        var table = TestUtils.findTable(module, "Method Object combined()");
        var text = formulaOf(table);
        var declaredType = text.indexOf(COMBINED_TYPE);
        var castType = text.indexOf(COMBINED_TYPE, declaredType + 1);

        var usages = usagesOf(table);

        assertTrue(usages.stream()
                .noneMatch(usage -> overlapsCombinedType(usage, declaredType)
                        || overlapsCombinedType(usage, castType)));
        var variable = usages.stream()
                .filter(usage -> "x".equals(text.substring(usage.getStart(), usage.getEnd())))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No usage of x in " + text));
        assertEquals(COMBINED_TYPE + " x", variable.getDescription());
    }

    private static boolean overlapsCombinedType(NodeUsage usage, int typeStart) {
        return usage.getStart() < typeStart + COMBINED_TYPE.length() && typeStart < usage.getEnd();
    }

    private static ICell formulaCell(TableSyntaxNode table) {
        return table.getGridTable().getCell(0, 1);
    }

    private static String formulaOf(TableSyntaxNode table) {
        return formulaCell(table).getStringValue();
    }

    private static List<? extends NodeUsage> usagesOf(TableSyntaxNode table) {
        var cell = formulaCell(table);
        var metaInfo = table.getMetaInfoReader().getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
        assertNotNull(metaInfo);
        return metaInfo.getUsedNodes();
    }
}
