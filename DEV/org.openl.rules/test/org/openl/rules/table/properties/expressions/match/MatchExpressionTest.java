package org.openl.rules.table.properties.expressions.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import org.openl.exception.OpenLRuntimeException;

class MatchExpressionTest {

    @Test
    void testMatchExpressionLE() {
        var operationTest = "<=";

        var operationNameTest = LEMatchingExpression.OPERATION_NAME;

        testMatchExpression(operationNameTest, operationTest);
    }

    @Test
    void testMatchExpressionGT() {
        var operationTest = ">=";

        var operationNameTest = GTMatchingExpression.OPERATION_NAME;

        testMatchExpression(operationNameTest, operationTest);
    }

    @Test
    void testMatchExpressionEQ() {
        var operationTest = "==";

        var operationNameTest = EQMatchingExpression.OPERATION_NAME;

        testMatchExpression(operationNameTest, operationTest);
    }

    @Test
    void testMatchExpressionContains() {
        var operationTest = "==";

        var operationNameTest = ContainsMatchingExpression.OPERATION_NAME;

        testMatchExpression(operationNameTest, operationTest);
    }

    @Test
    void testUnknownMatchExpression() {
        var operationTest = "unknown";

        var operationNameTest = "unknownOperation";

        try {
            testMatchExpression(operationNameTest, operationTest);
            fail();
        } catch (OpenLRuntimeException e) {
            assertEquals("Unknown match expression operation 'UNKNOWNOPERATION'.", e.getMessage());
        }
    }

    private void testMatchExpression(String operationNameTest, String operationTest) {
        var contextAttributeTest = "contextAttributeTest";
        var matchExpressionStr = "%s(%s)".formatted(operationNameTest, contextAttributeTest);
        var matchExpression = new MatchingExpression(matchExpressionStr);

        var param = "paramTest";

        assertEquals(operationNameTest, matchExpression.getMatchExpression().getOperationName());
        assertEquals(contextAttributeTest, matchExpression.getMatchExpression().getContextAttribute());

        var codeExpressionTest = "%s %s %s".formatted(param, operationTest, contextAttributeTest);

        assertEquals(codeExpressionTest, matchExpression.getMatchExpression().getCodeExpression(param));
    }

}
