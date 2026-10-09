package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * The name of a type written where a value stands — a vocabulary passed to {@code getValues} — leads to the table
 * declaring the type, as it does where it is written as the type of a parameter.
 */
class TypeNamedAsValueTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/binding/TypeNamedAsValueTest.xlsx";

    TypeNamedAsValueTest() {
        super(SRC);
    }

    @Test
    void linksAVocabularyPassedAsAValueToItsTable() {
        var color = findTable("Datatype Color <String>");

        // =getValues(Color)
        var usages = usagesOf(findTable("Spreadsheet SpreadsheetResult colors(Color favourite)"), 1, 2);

        var link = usages.stream().filter(usage -> usage.getNodeType() == NodeType.DATATYPE).findFirst().orElseThrow();
        assertEquals(color.getUri(), link.getUri());
        assertEquals("Datatype Color <String>", link.getDescription());
        assertEquals(11, link.getStart());
        assertEquals(16, link.getEnd());
    }

    @Test
    void leavesAParameterOfTheSameTypeAField() {
        // =favourite names the parameter, a value of the type rather than the type itself.
        var usages = usagesOf(findTable("Spreadsheet SpreadsheetResult colors(Color favourite)"), 1, 3);

        // The `=` is described by the type of the cell, and the parameter is a field, linked to no table.
        assertEquals(List.of(NodeType.OTHER, NodeType.FIELD), usages.stream().map(NodeUsage::getNodeType).toList());
    }

    private static List<? extends NodeUsage> usagesOf(TableSyntaxNode node, int column, int row) {
        var cell = node.getGridTable().getCell(column, row);
        var metaInfo = node.getMetaInfoReader().getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
        assertNotNull(metaInfo);
        return metaInfo.getUsedNodes() == null ? List.of() : metaInfo.getUsedNodes();
    }
}
