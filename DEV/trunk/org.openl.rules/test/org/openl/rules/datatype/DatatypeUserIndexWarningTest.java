package org.openl.rules.datatype;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.BaseOpenlBuilderHelper;

public class DatatypeUserIndexWarningTest extends BaseOpenlBuilderHelper {
    
    private static String src = "test/rules/datatype/DatatypeUserIndexWarning.xls";
    
    public DatatypeUserIndexWarningTest() {
        super(src);
    }
    
    @Test
    public void testDatatypeWarnings() {
        CompiledOpenClass compiledOpenClass = getJavaWrapper().getCompiledClass();

        List<OpenLMessage> messages = compiledOpenClass.getMessages();
        List<OpenLMessage> warningMessages = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.WARN);
        assertEquals(5, warningMessages.size());
        for (OpenLMessage message : warningMessages) {
            assertTrue(message.getSummary().contains("Can`t set index field for datatype, as it`s first value"));
        }
    }
   
}
