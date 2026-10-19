package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.rules.BaseOpenlBuilderHelper;

/**
 * The names an array index operator declares, as {@code (m, i)}, have hints: where the value comes from, then the type
 * and the name, as a use of the name in the expression shows them.
 */
class ElementIndexHintsTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/binding/EPBDS-6885_ElementIndexHints.xlsx";

    ElementIndexHintsTest() {
        super(SRC);
    }

    @Test
    void describesTheElementAndTheIndex() {
        var member = findTable("Datatype Member");
        // = members[(m, i) select all having i > 0]
        var cell = cellOf(1, 2);

        var element = cell.usageOf("m", "(m");
        assertEquals("Element of Member[]\nMember m", element.getDescription());
        assertEquals(member.getUri(), element.getUri());
        assertEquals(NodeType.FIELD, element.getNodeType());

        var index = cell.usageOf("i", ", i");
        var indexInExpression = cell.usageOf("i", "having i");
        assertEquals("Index of the element in Member[]\nInteger i", index.getDescription());
        assertEquals("Integer i", indexInExpression.getDescription());
        assertEquals(indexInExpression.getUri(), index.getUri());
    }

    @Test
    void describesTheDeclaredTypes() {
        // = members[(Member m, Long i) transform to m.name + i]
        var cell = cellOf(1, 3);

        assertEquals("Element of Member[]\nMember m", cell.usageOf("m", " m,").getDescription());
        assertEquals("Index of the element in Member[]\nLong i", cell.usageOf("i", "Long i").getDescription());
    }

    @Test
    void describesTheElementNamedAlone() {
        // = members[(m) transform to m.name]
        var cell = cellOf(1, 4);

        assertEquals("Element of Member[]\nMember m", cell.usageOf("m", "(m").getDescription());
    }

    private Cell cellOf(int column, int row) {
        var node = findTable("Spreadsheet SpreadsheetResult hints()");
        var cell = node.getGridTable().getCell(column, row);
        var metaInfo = node.getMetaInfoReader().getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
        assertNotNull(metaInfo);
        return new Cell(cell.getStringValue(), metaInfo.getUsedNodes());
    }

    /**
     * The text of a cell and its hints.
     */
    private record Cell(String text, List<? extends NodeUsage> usages) {

        /**
         * Finds the hint of the name where it last occurs in the given part of the text.
         */
        NodeUsage usageOf(String name, String context) {
            var start = text.indexOf(context) + context.lastIndexOf(name);
            var usage = usages.stream().filter(u -> u.getStart() == start).findFirst().orElseThrow();
            assertEquals(name, text.substring(usage.getStart(), usage.getEnd()));
            return usage;
        }
    }
}
