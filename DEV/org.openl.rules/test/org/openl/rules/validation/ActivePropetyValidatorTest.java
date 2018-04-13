package org.openl.rules.validation;

import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;
import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.BaseOpenlBuilderHelper;

public class ActivePropetyValidatorTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/validation/TestData.xls";

    public ActivePropetyValidatorTest() {
        super(SRC);
    }

    @Test
    public void testOddActiveTable() {
        assertTrue(isMessageOccured(getCompiledOpenClass().getMessages(),
            ActivePropertyValidator.ODD_ACTIVE_TABLE_MESSAGE,
            Severity.ERROR));
    }

    @Test
    public void testNoActiveTable() {
        assertTrue(isMessageOccured(getCompiledOpenClass().getMessages(),
            ActivePropertyValidator.NO_ACTIVE_TABLE_MESSAGE,
            Severity.WARN));
    }

    private boolean isMessageOccured(Collection<OpenLMessage> messages, String message, Severity severity) {
        for (OpenLMessage m : messages) {
            if (m.getSummary().equals(message) && m.getSeverity() == severity) {
                return true;
            }
        }
        return false;
    }
}
