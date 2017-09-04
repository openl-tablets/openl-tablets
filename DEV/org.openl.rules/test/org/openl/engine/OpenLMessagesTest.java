package org.openl.engine;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessages;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.runtime.RulesEngineFactory;

public class OpenLMessagesTest {

    public static final String SRC1 = "test/rules/messages/project1.xls";
    public static final String SRC2 = "test/rules/messages/project2.xls";
    public static final String SRC3 = "test/rules/messages/merged-region.xlsx";

    @Test
    public void testInSeriesCompileMessages1() {
        // test using wrapper generation
        //
        BaseOpenlBuilderHelper helper1 = new BaseOpenlBuilderHelper() {
        };
        helper1.build(SRC1);
        OpenLMessages messages = OpenLMessages.getCurrentInstance();
        assertEquals("Should be one message from current module", 1, messages.getMessages().size());

        BaseOpenlBuilderHelper helper2 = new BaseOpenlBuilderHelper() {
        };
        helper2.build(SRC2);
        OpenLMessages messages1 = OpenLMessages.getCurrentInstance();
        assertEquals("Messages should be 5, just from current module", 2, messages1.getMessages().size());
    }

    public interface Project1Int {
        String hello(int hour);
    }

    public interface Project2Int {
        int test(int a);
    }

    @Test
    public void testInSeriesCompileMessages2() {
        // test using engine factory
        //

        RulesEngineFactory<Project1Int> engineFactory1 = new RulesEngineFactory<Project1Int>(SRC1, Project1Int.class);
        engineFactory1.setExecutionMode(false);
        try {
            engineFactory1.newEngineInstance();
        } catch (OpenlNotCheckedException ignored) {
        }
        OpenLMessages messages = OpenLMessages.getCurrentInstance();
        assertEquals("Should be one message from current module", 1, messages.getMessages().size());

        RulesEngineFactory<Project2Int> engineFactory2 = new RulesEngineFactory<Project2Int>(SRC2,
                Project2Int.class);
        engineFactory2.setExecutionMode(false);
        try {
            engineFactory2.newEngineInstance();
        } catch (OpenlNotCheckedException ignored) {
        }
        OpenLMessages messages1 = OpenLMessages.getCurrentInstance();
        assertEquals("Messages should be 5, just from current module", 2, messages1.getMessages().size());
    }

    @Test
    public void testErrorsInMergedRegions() {
        BaseOpenlBuilderHelper helper1 = new BaseOpenlBuilderHelper() { };
        helper1.build(SRC3);
        OpenLMessages messages = OpenLMessages.getCurrentInstance();
        assertEquals("Must be only one message from current module", 1, messages.getMessages().size());
    }

    @Test
    public void testNoDuplicatedErrors() {
        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>("test/rules/messages/tables-with-errors.xls");
        engineFactory.setExecutionMode(false);
        List<OpenLMessage> messages = engineFactory.getCompiledOpenClass().getMessages();

        assertOnlyOneMessage("Operator not defined: negative(int[])", messages);
        assertOnlyOneMessage("Access non-static field from a static object", messages);
        assertOnlyOneMessage("Access of a non-static method from a static object", messages);
        assertOnlyOneMessage("Condition must have boolean type", messages);
        assertOnlyOneMessage("Operator not defined for: add(int[], int)", messages);
        assertOnlyOneMessage("Operator not defined for: and(int, int)", messages);
        assertOnlyOneMessage("Operator not defined for: or(int, int)", messages);
        assertOnlyOneMessage("Type 'int1' is not found", messages);
        assertOnlyOneMessage("Constructor is not found: java.util.Calendar()", messages);
        assertOnlyOneMessage("Order By expression must be Comparable", messages);
        assertOnlyOneMessage("Operator not defined: inc(int[])", messages);
        assertOnlyOneMessage("Operator not defined: dec(int[])", messages);
        assertOnlyOneMessage("The method must return a value", messages);

        assertEquals(13, messages.size());
    }

    private void assertOnlyOneMessage(String message, List<OpenLMessage> messages) {
        int count = 0;
        for (OpenLMessage openLMessage : messages) {
            if (openLMessage.getSummary().equals(message)) {
                count++;
            }
        }

        assertEquals("Expected only one message \"" + message + "\"", 1, count);
    }
}
