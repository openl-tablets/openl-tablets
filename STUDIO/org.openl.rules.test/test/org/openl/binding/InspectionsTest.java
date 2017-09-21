package org.openl.binding;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.openl.OpenL;
import org.openl.engine.OpenLManager;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessages;
import org.openl.message.Severity;
import org.openl.source.SourceType;
import org.openl.source.impl.StringSourceCodeModule;

public class InspectionsTest {
    private static final String ALWAYS_TRUE = "Condition is always true";
    private static final String ALWAYS_FALSE = "Condition is always false";

    @Test
    public void testConditionTypes() {
        checkWarning("Integer num = 7; num == num ? 1 : 2", ALWAYS_TRUE);
        checkWarning("Integer num = 7; num ==== num ? 1 : 2", ALWAYS_TRUE);
        checkWarning("Integer num = 7; num <= num ? 1 : 2", ALWAYS_TRUE);
        checkWarning("Integer num = 7; num <=== num ? 1 : 2", ALWAYS_TRUE);
        checkWarning("Integer num = 7; num >= num ? 1 : 2", ALWAYS_TRUE);
        checkWarning("Integer num = 7; num >=== num ? 1 : 2", ALWAYS_TRUE);

        checkWarning("Integer num = 7; num != num ? 1 : 2", ALWAYS_FALSE);
        checkWarning("Integer num = 7; num !=== num ? 1 : 2", ALWAYS_FALSE);
        checkWarning("Integer num = 7; num < num ? 1 : 2", ALWAYS_FALSE);
        checkWarning("Integer num = 7; num <== num ? 1 : 2", ALWAYS_FALSE);
        checkWarning("Integer num = 7; num > num ? 1 : 2", ALWAYS_FALSE);
        checkWarning("Integer num = 7; num >== num ? 1 : 2", ALWAYS_FALSE);
    }

    @Test
    public void testDifferentExpressionTypes() {
        checkWarning("Integer num = 7; num == num ? 1 : 2", ALWAYS_TRUE);
        // Same field of same object
        checkWarning("String[] arr = {\"bb\"}; arr.length == arr.length ? 1 : 2", ALWAYS_TRUE);
        // Literal
        checkWarning("1 == 1 ? 5 : 6", ALWAYS_TRUE);

        // Deep field access
        Object result;
        result = checkWarning("java.io.File f = new java.io.File(\"/some/fictional/path/\"); f.parentFile.parentFile.name == f.parentFile.parentFile.name ? 1 : 2", ALWAYS_TRUE);
        assertEquals(1, result);
        result = checkNoMessage("java.io.File f = new java.io.File(\"/some/fictional/path/\"); f.parentFile.name == f.parentFile.parentFile.name ? 1 : 2");
        assertEquals(2, result);

        checkWarning("Integer[] arr = {1, 0, 2, 3}; arr[(a) select first having a == a]", ALWAYS_TRUE);
        checkWarning("Integer i = 0; while(i < i) i++; i", ALWAYS_FALSE);
        checkWarning("Integer sum = 0; for (int i = 0; i < i; i++) sum++; sum", ALWAYS_FALSE);
    }

    @Test
    public void testNoWarning() {
        Object result;

        result = checkNoMessage("Integer[] arr = {1, 0, 2, 3}; arr[(a) @ a == 0].length == arr.length ? \"All zero\" : \"Has non zero values\"");
        assertEquals("Has non zero values", result);

        result = checkNoMessage("Integer[] arr = {0, 0, 0}; arr[(a) @ a == 0].length == arr.length ? \"All zero\" : \"Has non zero values\"");
        assertEquals("All zero", result);

        checkNoMessage("String[] arr1 = {\"bb\"}; String[] arr2 = {\"bb\"}; arr1.length == arr2.length ? 1 : 2");
    }

    private <T> T checkWarning(String expression, String expectedMessage) {
        OpenLMessages.removeCurrentInstance();

        @SuppressWarnings("unchecked")
        T result = (T) OpenLManager.run(OpenL.getInstance(OpenL.OPENL_J_NAME),
                new StringSourceCodeModule(expression, null),
                SourceType.METHOD_BODY);

        List<OpenLMessage> messages = OpenLMessages.getCurrentInstance().getMessages();
        assertEquals(1, messages.size());
        assertEquals(Severity.WARN, messages.get(0).getSeverity());
        assertEquals(expectedMessage, messages.get(0).getSummary());

        OpenLMessages.removeCurrentInstance();
        return result;
    }

    private <T> T checkNoMessage(String expression) {
        OpenLMessages.removeCurrentInstance();

        @SuppressWarnings("unchecked")
        T result = (T) OpenLManager.run(OpenL.getInstance(OpenL.OPENL_J_NAME),
                new StringSourceCodeModule(expression, null),
                SourceType.METHOD_BODY);

        List<OpenLMessage> messages = OpenLMessages.getCurrentInstance().getMessages();
        assertEquals(0, messages.size());

        OpenLMessages.removeCurrentInstance();
        return result;
    }
}
