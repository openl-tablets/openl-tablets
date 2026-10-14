package org.openl.rules.lang.xls.types.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.openl.CompiledOpenClass;
import org.openl.binding.impl.NodeType;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.TestUtils;
import org.openl.rules.datatype.binding.DatatypeHelper;
import org.openl.rules.datatype.binding.DatatypeTableBoundNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IOpenClass;

class DatatypeTableMetaInfoReaderTest {

    private static final String SRC = "test/rules/datatype/ContextBoundFields.xlsx";

    private static CompiledOpenClass compiledOpenClass;
    private static IOpenClass moduleOpenClass;

    @BeforeAll
    static void compile() {
        compiledOpenClass = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        moduleOpenClass = compiledOpenClass.getOpenClassWithErrors();
    }

    // The workbook fails only on the rows it rejects on purpose, so every other row declares its field.
    @Test
    void theWorkbookFailsOnlyOnTheRowsItRejects() {
        var errors = OpenLMessagesUtils.filterMessagesBySeverity(compiledOpenClass.getAllMessages(), Severity.ERROR)
                .stream()
                .map(OpenLMessage::getSummary)
                .collect(Collectors.toSet());
        assertEquals(Set.of("Bad context property name: '1bad'.", "Field 'homeState' is already declared."), errors);
    }

    @Test
    void returnsNoMetaInfoForRowsOutsideBoundLogicalTable() {
        var boundNode = mock(DatatypeTableBoundNode.class);
        var logicalTable = mock(ILogicalTable.class);
        var firstCell = mock(ICell.class);
        when(boundNode.getTable()).thenReturn(logicalTable);
        when(boundNode.getColumnTitlesOrder()).thenReturn(Map.of(DatatypeHelper.DEFAULT_COLUMN_TITLE, 2));
        when(logicalTable.getCell(0, 0)).thenReturn(firstCell);
        when(logicalTable.isNormalOrientation()).thenReturn(true);
        when(logicalTable.getHeight()).thenReturn(5);
        when(logicalTable.getRow(5)).thenThrow(ArrayIndexOutOfBoundsException.class);

        assertNull(new DatatypeTableMetaInfoReader(boundNode).getBodyMetaInfo(5, 2));
        verify(logicalTable, never()).getRow(anyInt());
    }

    // A field bound to a runtime context property is read as any other field: its type links to the datatype, and
    // its default value is a value of that type.
    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            situsState : context.usState  | State
            lob : context                 | Lob
            businessNature:context.nature | Nature
            currency* : context.currency  | Currency
            mailState                     | State
            """)
    void aFieldBoundToAContextPropertyIsReadAsAnyOtherField(String nameCell, String typeName) {
        var link = metaInfoBeside("Datatype Policy", nameCell, -1).getUsedNodes().getFirst();
        assertEquals(NodeType.DATATYPE, link.getNodeType());
        assertEquals(moduleOpenClass.findType(typeName).getMetaInfo().getSourceUrl(), link.getUri());

        assertEquals(typeName, metaInfoBeside("Datatype Policy", nameCell, 1).getDataType().getName());
    }

    // The default value of an array field holds values of its element type, several of them in one cell.
    @Test
    void theDefaultValueOfAnArrayFieldHoldsValuesOfItsElementType() {
        var metaInfo = metaInfoBeside("Datatype Policy", "states", 1);

        assertEquals("State", metaInfo.getDataType().getName());
        assertTrue(metaInfo.isMultiValue());
    }

    // A row that declares no field has no meta info, even when its name cell names a field declared elsewhere: a
    // commented row, a row rejected for a bad context property, a row declaring a name the second time, and the row
    // of column titles.
    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            Datatype Policy               | lob:context
            Datatype Child extends Policy | situsState : context.1bad
            Datatype Twice                | homeState~
            Datatype Titled               | Name
            """)
    void aRowThatDeclaresNoFieldHasNoMetaInfo(String tableName, String nameCell) {
        assertNull(metaInfoBeside(tableName, nameCell, -1));
        assertNull(metaInfoBeside(tableName, nameCell, 1));
    }

    private static CellMetaInfo metaInfoBeside(String tableName, String nameCell, int columnOffset) {
        return TestUtils.metaInfoBeside(TestUtils.findTable(moduleOpenClass, tableName), nameCell, columnOffset);
    }
}
