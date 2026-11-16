package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.rules.BaseOpenlBuilderHelper;

/**
 * A field an index operation is applied to keeps its hint and its link when the operation is an error, such as an
 * index of an Integer.
 *
 * @author Yury Molchan
 */
class ErrorIndexHintsTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/binding/EPBDS-7419_Hints_In_Error.xlsx";

    ErrorIndexHintsTest() {
        super(SRC);
    }

    @Test
    void anAggregateOperationOfAnIntegerKeepsTheHintOfTheField() {
        // = container[ (v) order increasing by v]
        assertFieldHint(2);
    }

    @Test
    void anIndexOfAnIntegerKeepsTheHintOfTheField() {
        // = container[0]
        assertFieldHint(3);
    }

    @Test
    void anIndexAfterAnIndexOfAnIntegerKeepsTheHintOfTheField() {
        // = container[0][0]
        assertFieldHint(4);
    }

    private void assertFieldHint(int row) {
        var foo = findTable("Datatype Foo");
        var node = findTable("Spreadsheet SpreadsheetResult hints(Foo foo)");
        var cell = node.getGridTable().getCell(1, row);
        var metaInfo = node.getMetaInfoReader().getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
        assertNotNull(metaInfo);
        var text = cell.getStringValue();
        var start = text.indexOf("container");
        NodeUsage usage = metaInfo.getUsedNodes()
                .stream()
                .filter(u -> u.getStart() == start)
                .findFirst()
                .orElseThrow();
        assertEquals("container", text.substring(usage.getStart(), usage.getEnd()));
        assertEquals(NodeType.FIELD, usage.getNodeType());
        assertEquals(foo.getUri(), usage.getUri());
    }
}
