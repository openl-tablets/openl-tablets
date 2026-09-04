package org.openl.rules.datatype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.binding.impl.NodeType;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.types.IOpenClass;

/**
 * A datatype that titles its columns declares the same thing as one written in the legacy positional layout, so
 * everything reading it must agree: the fields start below the title row, and a column is found by its title.
 */
class TitledDatatypeTest {

    private static final String SRC = "test/rules/datatype/TitledDatatype.xlsx";

    private static IOpenClass moduleOpenClass;
    private static TableSyntaxNode[] tableSyntaxNodes;

    @BeforeAll
    static void compile() {
        var compiledOpenClass = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        moduleOpenClass = compiledOpenClass.getOpenClassWithErrors();
        tableSyntaxNodes = ((XlsMetaInfo) moduleOpenClass.getMetaInfo()).getXlsModuleNode().getXlsTableSyntaxNodes();
    }

    // The index field is the first field of the datatype, which in a titled table is the row below the titles.
    @Test
    void aTitledDatatypeIndexesItselfByItsFirstField() {
        assertIndexField("Person", "name");
    }

    @Test
    void aLegacyDatatypeIndexesItselfByItsFirstField() {
        assertIndexField("LegacyPerson", "name");
    }

    // A commented row declares no field, so the index field is the first field the table really declares.
    @Test
    void aCommentedRowDoesNotTakeTheIndexField() {
        assertIndexField("CommentedFirstField", "name");
    }

    // The editor links a Type cell to the datatype it names, and finds the field by the name cell of the same row.
    @Test
    void aTypeCellOfATitledDatatypeLinksToTheDatatypeItNames() {
        assertTypeCellIsLinked("Datatype Person");
    }

    @Test
    void aTypeCellOfALegacyDatatypeLinksToTheDatatypeItNames() {
        assertTypeCellIsLinked("Datatype LegacyPerson");
    }

    private static void assertIndexField(String datatype, String expectedFieldName) {
        var type = moduleOpenClass.findType(datatype);
        assertNotNull(type, "There is '%s' datatype".formatted(datatype));
        var indexField = type.getIndexField();
        assertNotNull(indexField, "Datatype '%s' has an index field".formatted(datatype));
        assertEquals(expectedFieldName, indexField.getName());
    }

    private static void assertTypeCellIsLinked(String tableName) {
        var metaInfo = metaInfoOfCellNamed(tableName, "Address");
        assertNotNull(metaInfo, "The 'Address' cell of '%s' has meta info".formatted(tableName));
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(metaInfo),
                "The 'Address' cell of '%s' links somewhere".formatted(tableName));
        var link = metaInfo.getUsedNodes().getFirst();
        assertEquals("Datatype Address", link.getDescription(),
                "The 'Address' cell of '%s' links to the datatype it names".formatted(tableName));
        assertEquals(NodeType.DATATYPE, link.getNodeType());
        assertEquals(moduleOpenClass.findType("Address").getMetaInfo().getSourceUrl(), link.getUri());
    }

    private static CellMetaInfo metaInfoOfCellNamed(String tableName, String cellText) {
        var tableSyntaxNode = findTable(tableName);
        var grid = tableSyntaxNode.getGridTable();
        for (var row = 0; row < grid.getHeight(); row++) {
            for (var column = 0; column < grid.getWidth(); column++) {
                var cell = grid.getCell(column, row);
                if (cellText.equals(cell.getStringValue())) {
                    return tableSyntaxNode.getMetaInfoReader()
                            .getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
                }
            }
        }
        throw new IllegalStateException("There is no '%s' cell in '%s'".formatted(cellText, tableName));
    }

    private static TableSyntaxNode findTable(String tableName) {
        for (TableSyntaxNode tableSyntaxNode : tableSyntaxNodes) {
            if (tableName.equals(tableSyntaxNode.getDisplayName())) {
                return tableSyntaxNode;
            }
        }
        throw new IllegalStateException("There is no '%s' table".formatted(tableName));
    }
}
