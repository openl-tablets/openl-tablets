package org.openl.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.runtime.RulesEngineFactory;

/**
 * A message names the table it is about in the text a log or a console shows. Its summary, which OpenL Studio shows
 * next to the table, stays as it is.
 *
 * @author Yury Molchan
 */
class MessageTablesTest {

    private static final String WORKBOOK = "test-resources/functionality/EPBDS-8658_Message_Table_Names.xlsx";

    @Test
    void aMessageNamesItsTable() {
        var messages = compile(false);

        assertEquals(List.of("products: Found duplicate of field 'featureValues'"),
                messages.stream().map(OpenLMessage::getSummaryInTable).toList());
        assertEquals(List.of("Found duplicate of field 'featureValues'"),
                messages.stream().map(OpenLMessage::getSummary).toList());
    }

    // The execution mode drops the tables after the compilation, and the warnings of the author
    @Test
    void anErrorNamesItsTableInTheExecutionMode() {
        var messages = compile(true);

        assertEquals(1, messages.size());
        assertEquals("products: Found duplicate of field 'featureValues'", messages.getFirst().getSummaryInTable());
    }

    private static List<OpenLMessage> compile(boolean executionMode) {
        var factory = new RulesEngineFactory<>(WORKBOOK);
        factory.setExecutionMode(executionMode);
        return List.copyOf(factory.getCompiledOpenClass().getAllMessages());
    }
}
