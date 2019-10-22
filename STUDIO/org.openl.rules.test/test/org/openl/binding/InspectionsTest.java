package org.openl.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openl.OpenL;
import org.openl.engine.OpenLManager;
import org.openl.engine.OpenLSourceManager;
import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.source.SourceType;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.code.ProcessedCode;

public class InspectionsTest {
    private static final String ALWAYS_TRUE = "Condition is always true.";
    private static final String ALWAYS_FALSE = "Condition is always false.";

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
        checkWarning("String[] arr = {\"bb\"}; arr == arr ? 1 : 2", ALWAYS_TRUE);
        // Literal
        checkWarning("1 == 1 ? 5 : 6", ALWAYS_TRUE);

        // Deep field access
        Object result;
        result = checkWarning(
            "java.io.File f = new java.io.File(\"/some/fictional/path/\"); f.parentFile.parentFile.name == f.parentFile.parentFile.name ? 1 : 2",
            ALWAYS_TRUE);
        assertEquals(1, result);
        result = checkNoMessage(
            "java.io.File f = new java.io.File(\"/some/fictional/path/\"); f.parentFile.name == f.parentFile.parentFile.name ? 1 : 2");
        assertEquals(2, result);

        checkWarning("Integer[] arr = {1, 0, 2, 3}; arr[(a) select first having a == a]", ALWAYS_TRUE);
        checkWarning("Integer[] arr = {1, 0, 2, 3}; arr[(a) select first \n    having a == a]", ALWAYS_TRUE);
        checkWarning("Integer[] arr = {1, 0, 2, 3}; arr[(a) select first \n    where a == a]", ALWAYS_TRUE);
        checkWarning("Integer i = 0; while(i < i) i++; i", ALWAYS_FALSE);
        checkWarning("Integer sum = 0; for (int i = 0; i < i; i++) sum++; sum", ALWAYS_FALSE);
    }

    @Test
    public void testNoWarning() {
        Object result;

        result = checkNoMessage(
            "Integer[] arr = {1, 0, 2, 3}; arr[(a) @ a == 0] == arr ? \"All zero\" : \"Has non zero values\"");
        assertEquals("Has non zero values", result);

        result = checkNoMessage(
            "Integer[] arr = {0, 0, 0}; arr[(a) @ a == 0] == arr ? \"All zero\" : \"Has non zero values\"");
        assertEquals("All zero", result);

        result = checkNoMessage("String[] arr1 = {\"bb\"}; String[] arr2 = {\"bb\"}; arr1 == arr2 ? \"1\" : \"2\"");
        assertEquals("1", result);

        result = checkNoMessage("Integer[] arr = {0, 0, 1}; arr[(a) @ a == 0] != arr ? \"1\" : \"2\"");
        assertEquals("1", result);
        result = checkNoMessage("Integer[] arr = {0, 0, 0}; arr[(a) @ a == 0] != arr ? \"1\" : \"2\"");
        assertEquals("2", result);
    }

    @SuppressWarnings("unchecked")
    private <T> T checkWarning(String expression, String... expectedMessage) {
        ProcessedCode processedCode = new OpenLSourceManager(OpenL.getInstance(OpenL.OPENL_J_NAME))
            .processSource(new StringSourceCodeModule(expression, null), SourceType.METHOD_BODY);
        assertEquals(expectedMessage.length, processedCode.getMessages().size());
        int i = 0;
        for (OpenLMessage message : processedCode.getMessages()) {
            assertEquals(Severity.WARN, message.getSeverity());
            assertEquals(expectedMessage[i], message.getSummary());
            i++;
        }

        return (T) OpenLManager.run(OpenL.getInstance(OpenL.OPENL_J_NAME),
            new StringSourceCodeModule(expression, null),
            SourceType.METHOD_BODY);
    }

    @SuppressWarnings("unchecked")
    private <T> T checkNoMessage(String expression) {
        ProcessedCode processedCode = new OpenLSourceManager(OpenL.getInstance(OpenL.OPENL_J_NAME))
            .processSource(new StringSourceCodeModule(expression, null), SourceType.METHOD_BODY);

        assertTrue(processedCode.getMessages().isEmpty());

        return (T) OpenLManager.run(OpenL.getInstance(OpenL.OPENL_J_NAME),
            new StringSourceCodeModule(expression, null),
            SourceType.METHOD_BODY);
    }
}
