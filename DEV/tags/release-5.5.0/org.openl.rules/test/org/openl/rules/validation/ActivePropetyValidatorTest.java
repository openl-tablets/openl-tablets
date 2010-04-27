package org.openl.rules.validation;

import java.util.List;

import org.junit.Test;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessages;
import org.openl.message.Severity;
import org.openl.rules.BaseOpenlBuilderHelper;
import static org.junit.Assert.*;

public class ActivePropetyValidatorTest extends BaseOpenlBuilderHelper {

    private static String __src = "test/rules/validation/TestData.xls";

    public ActivePropetyValidatorTest() {
        super(__src);
    }

    @Test
    public void testOddActiveTable() {
        assertTrue(isMessageOccured(ActivePropertyValidator.ODD_ACTIVE_TABLE_MESSAGE, Severity.ERROR));
    }

    @Test
    public void testNoActiveTable() {
        assertTrue(isMessageOccured(ActivePropertyValidator.NO_ACTIVE_TABLE_MESSAGE, Severity.WARN));
    }

    private boolean isMessageOccured(String message, Severity severity) {
        List<OpenLMessage> messages = OpenLMessages.getCurrentInstance().getMessages();
        for (OpenLMessage openLMessage : messages) {
            if (openLMessage.getSummary().equals(message) && openLMessage.getSeverity() == severity) {
                return true;
            }
        }
        return false;
    }
}
