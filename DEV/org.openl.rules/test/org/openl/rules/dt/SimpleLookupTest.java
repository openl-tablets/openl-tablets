package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.testmethod.TestUnitsResults;

public class SimpleLookupTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "test/rules/dt/lookup/SimpleLookup.xls";

    public SimpleLookupTest() {
        super(SRC);
    }
    
    @Test
    public void testMergedConditions() {
        final String tableName = "SimpleLookup DoubleValue CarPrice (String country, String carBrand, String carModel)";
        TableSyntaxNode resultTsn = findTable(tableName);
        assertFalse(resultTsn.hasErrors());

        TestUnitsResults result = (TestUnitsResults) invokeMethod("CarPriceTest");
        assertEquals(0, result.getNumberOfFailures());
    }

}
