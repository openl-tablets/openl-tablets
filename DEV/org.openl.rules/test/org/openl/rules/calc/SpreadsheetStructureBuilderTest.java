package org.openl.rules.calc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.SpreadsheetMetaInfoReader;
import org.openl.rules.runtime.RulesEngineFactory;

/**
 * The steps, the columns and the result of a Spreadsheet as the compiler read them, which the editor and the table
 * themes of OpenL Studio take its parts from.
 */
class SpreadsheetStructureBuilderTest {

    // Spreadsheet intAlias test( ): the column "v" and the steps "A : Byte", "B : intAlias" and "RETURN".
    private static final String SRC = "test/rules/calc1/AliasInSpreadsheet.xlsx";

    private static SpreadsheetStructureBuilder structure;

    @BeforeAll
    static void compile() {
        var compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        var nodes = ((XlsMetaInfo) compiled.getOpenClassWithErrors().getMetaInfo()).getXlsModuleNode()
                .getXlsTableSyntaxNodes();
        var reader = Arrays.stream(nodes)
                .map(TableSyntaxNode::getMetaInfoReader)
                .filter(SpreadsheetMetaInfoReader.class::isInstance)
                .map(SpreadsheetMetaInfoReader.class::cast)
                .findFirst()
                .orElseThrow();
        structure = reader.getBoundNode().getStructureBuilder();
    }

    @Test
    void readsTheStepsByTheRowsOfTheBodyUnderTheTitles() {
        assertEquals(Map.of(0, "A", 1, "B", 2, "RETURN"), namesOf(structure.getRowHeaders()));
    }

    @Test
    void readsTheColumnsByTheColumnsOfTheBodyAfterTheSteps() {
        assertEquals(Map.of(0, "v"), namesOf(structure.getColumnHeaders()));
    }

    @Test
    void returnsTheStepNamedReturn() {
        var returned = structure.getReturnHeaderDefinition();
        assertTrue(returned.isRow());
        assertEquals("RETURN", returned.getDefinitionName());
    }

    private static Map<Integer, String> namesOf(Map<Integer, SpreadsheetHeaderDefinition> headers) {
        return headers.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, header -> header.getValue().getDefinitionName()));
    }
}
