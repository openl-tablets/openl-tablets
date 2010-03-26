package org.openl.rules.validator.dt;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;


public class ValidatorTest extends TestHelperValidator{
    
    private static String __src = "test/rules/Test.xls";
    
    public ValidatorTest() {
        super(__src);        
    }
    
    @Test
    public void testOk()
    {
        String tableName = "Rules String validationOK(TestValidationEnum1 value1, TestValidationEnum2 value2)";
        __testTable(tableName);
        tableName = "Rules String validationGap(TestValidationEnum1 value1, TestValidationEnum2 value2)";
        __testTable(tableName);
    }
    
    
    private void __testTable(String tableName) {
        TableSyntaxNode[] tsns = getTableSyntaxNodes();
        TableSyntaxNode resultTsn = findTable(tableName, tsns);
        if (resultTsn != null) {
            ITableProperties tableProperties  = resultTsn.getTableProperties();
            assertNotNull(tableProperties);
            assertTrue(getJavaWrapper().getCompiledClass().getBindingErrors().length == 0);
            assertTrue(getJavaWrapper().getCompiledClass().getParsingErrors().length == 0);
            
            DecisionTable dt = (DecisionTable) resultTsn.getMember();
            try {
                System.out.println("Validating " + tableName);
                DTValidationResult dtr = DTValidator.validateDT(dt, null, getJavaWrapper().getOpenClass());
                
                if (dtr.hasProblems()) {
                    resultTsn.setValidationResult(dtr);
                    System.out.println("we have problems!!!!");
                } else {
                    System.out.println("NO PROBLEMS!!!!");
                }
            } catch (Exception t) {
                System.out.println("Exception " + t.getMessage());
            }
        } else {
            fail();
        }
    }

}
