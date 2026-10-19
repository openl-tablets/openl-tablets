package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.binding.impl.NodeUsage;
import org.openl.rules.BaseOpenlBuilderHelper;

class CurrentSpreadsheetHintsTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/binding/EPBDS-6960_CurrentSpreadsheetHints.xlsx";

    CurrentSpreadsheetHintsTest() {
        super(SRC);
    }

    @Test
    void describesBothQualifiersAsTheContainingSpreadsheet() {
        var table = findTable("Spreadsheet SpreadsheetResult hintSelection(Integer[] values, Integer desired)");

        for (var row = 4; row <= 5; row++) {
            var name = row == 4 ? "this" : "current";
            var usage = cellOf(row).usageOf(name);
            assertEquals("Spreadsheet hintSelection", usage.getDescription());
            assertEquals(table.getUri(), usage.getUri());
        }
    }

    private Cell cellOf(int row) {
        var table = findTable("Spreadsheet SpreadsheetResult hintSelection(Integer[] values, Integer desired)");
        var cell = table.getGridTable().getCell(1, row);
        var metaInfo = table.getMetaInfoReader().getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
        assertNotNull(metaInfo);
        return new Cell(cell.getStringValue(), metaInfo.getUsedNodes());
    }

    private record Cell(String text, List<? extends NodeUsage> usages) {

        NodeUsage usageOf(String name) {
            var start = text.indexOf(name + ".$Key");
            var usage = usages.stream().filter(u -> u.getStart() == start).findFirst().orElseThrow();
            assertEquals(name, text.substring(usage.getStart(), usage.getEnd()));
            return usage;
        }
    }
}
