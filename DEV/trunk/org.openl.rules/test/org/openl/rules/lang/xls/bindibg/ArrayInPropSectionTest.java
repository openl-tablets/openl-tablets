package org.openl.rules.lang.xls.bindibg;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public class ArrayInPropSectionTest extends BaseOpenlBuilderHelper{
    
    private static String __src = "test/rules/TestArrayInPropSection.xls";
    
    public ArrayInPropSectionTest() {
        super(__src);        
    }
    
    @Test
    public void testLoadingArrayInPropertyTableSection() {      
        String tableName = "Rules DoubleValue driverRiskScoreOverloadTest(String driverRisk)";
        TableSyntaxNode[] tsns = getTableSyntaxNodes();
        TableSyntaxNode resultTsn = findTable(tableName, tsns);
        
        if (resultTsn != null) {
            assertEquals("Check that number of properties defined in table is 4",
                    resultTsn.getTableProperties().getPropertiesDefinedInTable().size(), 4);
            assertEquals("tag1", resultTsn.getTableProperties().getTags()[0]);
            assertEquals("tag3", resultTsn.getTableProperties().getTags()[1]);
            assertEquals("tag4", resultTsn.getTableProperties().getTags()[2]);
       } else {
           fail();
       }
    }

}
