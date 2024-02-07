package org.openl.rules.validator.dt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.dt.validator.DecisionTableValidationResult;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public class ValidateDTTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/validation/TestValidateDT.xls";

    public ValidateDTTest() {
        super(SRC);
    }

    @Test
    public void testHello1() {
        String tableName = "Rules int tableAScore(String maritalStatus, String gender, int tyde, int dsr)";
        TableSyntaxNode resultTsn = findTable(tableName);
        assertNotNull(resultTsn);

        Object vv = resultTsn.getValidationResult();

        assertEquals(8, ((DecisionTableValidationResult) vv).getOverlappings().length);
        assertEquals(0, ((DecisionTableValidationResult) vv).getUncovered().length);
        assertEquals(0, ((DecisionTableValidationResult) vv).getOverlappingBlocks().size());
        assertEquals(8, ((DecisionTableValidationResult) vv).getOverlappingPartialOverlaps().size());

    }

    @Test
    public void testHello2() {
        String tableName = "SimpleRules String check2(String dd1, String dd2)";
        TableSyntaxNode resultTsn = findTable(tableName);
        assertNotNull(resultTsn);

        Object vv = resultTsn.getValidationResult();

        assertEquals(1, ((DecisionTableValidationResult) vv).getOverlappingBlocks().size());
        assertEquals(2, ((DecisionTableValidationResult) vv).getOverlappingOverrides().size());
        assertEquals(1, ((DecisionTableValidationResult) vv).getUncovered().length);

    }

    @Test
    public void testHello3() {
        String tableName = "Rules String check3(int dd1, String dd2)";
        TableSyntaxNode resultTsn = findTable(tableName);
        assertNotNull(resultTsn);

        Object vv = resultTsn.getValidationResult();

        assertEquals(1, ((DecisionTableValidationResult) vv).getOverlappings().length);
        assertEquals(1, ((DecisionTableValidationResult) vv).getUncovered().length);

    }

    @Test
    public void testHello4() {
        String tableName = "Rules String check4(int dd1, String dd2)";
        TableSyntaxNode resultTsn = findTable(tableName);
        assertNotNull(resultTsn);

        Object vv = resultTsn.getValidationResult();

        assertNull(vv);
    }

}
