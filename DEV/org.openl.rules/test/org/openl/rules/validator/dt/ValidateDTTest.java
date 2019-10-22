package org.openl.rules.validator.dt;

import org.junit.Assert;
import org.junit.Test;
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
        Assert.assertNotNull(resultTsn);

        Object vv = resultTsn.getValidationResult();

        Assert.assertEquals(8, ((DecisionTableValidationResult) vv).getOverlappings().length);
        Assert.assertEquals(0, ((DecisionTableValidationResult) vv).getUncovered().length);
        Assert.assertEquals(0, ((DecisionTableValidationResult) vv).getOverlappingBlocks().size());
        Assert.assertEquals(8, ((DecisionTableValidationResult) vv).getOverlappingPartialOverlaps().size());

    }

    @Test
    public void testHello2() {
        String tableName = "SimpleRules String check2(String dd1, String dd2)";
        TableSyntaxNode resultTsn = findTable(tableName);
        Assert.assertNotNull(resultTsn);

        Object vv = resultTsn.getValidationResult();

        Assert.assertEquals(1, ((DecisionTableValidationResult) vv).getOverlappingBlocks().size());
        Assert.assertEquals(2, ((DecisionTableValidationResult) vv).getOverlappingOverrides().size());
        Assert.assertEquals(1, ((DecisionTableValidationResult) vv).getUncovered().length);

    }

    @Test
    public void testHello3() {
        String tableName = "Rules String check3(int dd1, String dd2)";
        TableSyntaxNode resultTsn = findTable(tableName);
        Assert.assertNotNull(resultTsn);

        Object vv = resultTsn.getValidationResult();

        Assert.assertEquals(1, ((DecisionTableValidationResult) vv).getOverlappings().length);
        Assert.assertEquals(1, ((DecisionTableValidationResult) vv).getUncovered().length);

    }

    @Test
    public void testHello4() {
        String tableName = "Rules String check4(int dd1, String dd2)";
        TableSyntaxNode resultTsn = findTable(tableName);
        Assert.assertNotNull(resultTsn);

        Object vv = resultTsn.getValidationResult();

        Assert.assertNull(vv);
    }

}
