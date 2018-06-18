package org.openl.rules.testmethod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.openl.message.OpenLMessage;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;

public class TestTableErrorsTest extends BaseOpenlBuilderHelper {
    public TestTableErrorsTest() {
        super("test/rules/testmethod/TestAndRunTablesWithError.xlsx");
    }

    @Test
    public void testHaveErrorAboveRunTable() {
        TableSyntaxNode table = findTable("Run DriverPremium5 DriverPremium5Run");
        assertNotNull("Table DriverPremium5Run not found", table);
        assertContainsError("Property 'version' can't be defined in Run Table", table);
        assertEquals("Run Table node must contain only one error", 1, table.getErrors().length);
    }

    @Test
    public void testHaveErrorAboveTestTable() {
        TableSyntaxNode table = findTable("Test DriverPremium5 DriverPremium5Test");
        assertNotNull("Table DriverPremium5Test not found", table);
        assertContainsError("Property 'active' can't be defined in Test Table", table);
        assertEquals("Test Table node must contain only one error", 1, table.getErrors().length);
    }

    @Test
    public void testHaveErrorAboveEmptyTestTable() {
        TableSyntaxNode table = findTable("Test DriverPremium5 DriverPremiumEmptyTest");
        assertNotNull("Table DriverPremiumEmptyTest not found", table);
        assertContainsError("There is no body in Data table.", table);
        assertContainsError("Property 'id' can't be defined in Test Table", table);
        assertEquals("Test Table node must contain only 2 errors", 2, table.getErrors().length);
    }

    @Test
    public void testAllErrorsHaveSyntaxNodes() {
        TableSyntaxNode[] syntaxNodes = getTableSyntaxNodes();

        for (OpenLMessage message : getCompiledOpenClass().getMessages()) {
            // Search syntax node with same error message
            boolean found = false;
            for (TableSyntaxNode node : syntaxNodes) {
                if (containsError(message.getSummary(), node)) {
                    found = true;
                    break;
                }
            }

            assertTrue("Message \"" + message.getSummary() + "\" doesn't have corresponding TableSyntaxNode", found);
        }
    }

    private void assertContainsError(String error, TableSyntaxNode node) {
        if (!containsError(error, node)) {
            fail("TableSyntaxNode doesn't contain error \"" + error + "\"");
        }
    }

    private boolean containsError(String error, TableSyntaxNode node) {
        if (node.getErrors() != null) {
            for (SyntaxNodeException exception : node.getErrors()) {
                if (error.equals(exception.getMessage())) {
                    return true;
                }
            }
        }
        return false;
    }
}
