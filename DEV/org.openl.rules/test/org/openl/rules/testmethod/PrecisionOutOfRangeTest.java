package org.openl.rules.testmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.runtime.RulesEngineFactory;

/**
 * A column precision outside its limits is reported as such.
 *
 * @author Yury Molchan
 */
class PrecisionOutOfRangeTest {

    private static final String FILE_NAME = "test/rules/testmethod/PrecisionOutOfRange.xlsx";

    @Test
    void precisionOutOfRange() {
        var errors = new RulesEngineFactory<>(FILE_NAME).getCompiledOpenClass()
                .getAllMessages()
                .stream()
                .filter(message -> message.getSeverity() == Severity.ERROR)
                .map(OpenLMessage::getSummary)
                .toList();
        assertEquals(List.of(outOfRange("(100)"),
                outOfRange("(-100)"),
                outOfRange("(100sf)"),
                outOfRange("(0sf)"),
                outOfRange("(-5sf)")), errors);
    }

    private static String outOfRange(String precision) {
        return ("Precision '%s' is out of range. Use a whole number of decimal places from -99 to 99, a number of"
                + " significant figures from 1sf to 99sf, or sf.").formatted(precision);
    }
}
