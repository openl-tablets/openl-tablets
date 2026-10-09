package org.openl.binding.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.table.xls.XlsUrlParser;

/**
 * A method declaring one parameter twice is reported once, at the header that declares it, rather than by every
 * cell of the table that is bound with the parameters of the method.
 */
class DuplicateParameterTest {

    private static final String WORKBOOK = "test-resources/functionality/EPBDS-15361_DuplicateParameter.xlsx";

    @Test
    void reportsTheParameterDeclaredTwiceOnceAtTheHeader() {
        var errors = new RulesEngineFactory<>(WORKBOOK).getCompiledOpenClass()
                .getAllMessages()
                .stream()
                .filter(message -> message.getSeverity() == Severity.ERROR)
                .toList();

        assertEquals(List.of("Parameter 'a' is already defined.", "Parameter 'hour' is already defined."),
                errors.stream().map(OpenLMessage::getSummary).sorted().toList());
        // Each error leads to the header of its table, where the parameter is declared twice.
        assertEquals(List.of("B11", "B3"), errors.stream().map(DuplicateParameterTest::cellOf).sorted().toList());
    }

    private static String cellOf(OpenLMessage message) {
        return new XlsUrlParser(message.getSourceLocation()).getCell();
    }
}
