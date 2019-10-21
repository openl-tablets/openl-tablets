package org.openl.rules.table.properties.expressions.match;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.openl.exception.OpenLRuntimeException;

public class MatchExpressionTest {

    @Test
    public void testMatchExpressionLE() {
        String operationTest = "<=";

        String operationNameTest = LEMatchingExpression.OPERATION_NAME;

        testMatchExpression(operationNameTest, operationTest);
    }

    @Test
    public void testMatchExpressionGT() {
        String operationTest = ">=";

        String operationNameTest = GTMatchingExpression.OPERATION_NAME;

        testMatchExpression(operationNameTest, operationTest);
    }

    @Test
    public void testMatchExpressionEQ() {
        String operationTest = "==";

        String operationNameTest = EQMatchingExpression.OPERATION_NAME;

        testMatchExpression(operationNameTest, operationTest);
    }

    @Test
    public void testMatchExpressionContains() {
        String operationTest = "==";

        String operationNameTest = ContainsMatchingExpression.OPERATION_NAME;

        testMatchExpression(operationNameTest, operationTest);
    }

    @Test
    public void testUnknownMatchExpression() {
        String operationTest = "unknown";

        String operationNameTest = "unknownOperation";

        try {
            testMatchExpression(operationNameTest, operationTest);
            fail();
        } catch (OpenLRuntimeException e) {
            assertEquals("Unknown match expression operation 'UNKNOWNOPERATION'", e.getMessage());
        }
    }

    private void testMatchExpression(String operationNameTest, String operationTest) {
        String contextAttributeTest = "contextAttributeTest";
        String matchExpressionStr = String.format("%s(%s)", operationNameTest, contextAttributeTest);
        MatchingExpression matchExpression = new MatchingExpression(matchExpressionStr);

        String param = "paramTest";

        assertEquals(operationNameTest, matchExpression.getMatchExpression().getOperationName());
        assertEquals(contextAttributeTest, matchExpression.getMatchExpression().getContextAttribute());

        String codeExpressionTest = String.format("%s %s %s", param, operationTest, contextAttributeTest);

        assertEquals(codeExpressionTest, matchExpression.getMatchExpression().getCodeExpression(param));
    }

}
