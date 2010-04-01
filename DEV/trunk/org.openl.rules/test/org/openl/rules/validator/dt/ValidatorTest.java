package org.openl.rules.validator.dt;

import static org.junit.Assert.*;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openl.domain.IntRangeDomain;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;


public class ValidatorTest extends BaseOpenlBuilderHelper{
    
    private static String __src = "test/rules/Test_Validator_DT.xls";
    
    public ValidatorTest() {
        super(__src);        
    }
    
    @Test
    public void testOk() {
        String tableName = "Rules String validationOK(TestValidationEnum1 value1, TestValidationEnum2 value2)";
        Map<String, IDomainAdaptor> domains = new HashMap<String, IDomainAdaptor>();
        
//        EnumDomain<TestValidationEnum1> enumDomain1 = new EnumDomain<TestValidationEnum1>(new TestValidationEnum1[]{TestValidationEnum1.V1, TestValidationEnum1.V2});        
//        EnumDomainAdaptor enumDomainAdaptor1 = new EnumDomainAdaptor(enumDomain1);
//        domains.put("value1", enumDomainAdaptor1);
//        
//        EnumDomain<TestValidationEnum2> enumDomain2 = new EnumDomain<TestValidationEnum2>(new TestValidationEnum2[]{TestValidationEnum2.V1, TestValidationEnum2.V2});        
//        EnumDomainAdaptor enumDomainAdaptor2 = new EnumDomainAdaptor(enumDomain2);
//        domains.put("value2", enumDomainAdaptor2);
        
        DTValidationResult dtValidResult = testTable(tableName, domains);
        assertFalse(dtValidResult.hasProblems());
    }    
    
    @Test
    public void testGap() {
        String tableName = "Rules String validationGap(TestValidationEnum1 value1, TestValidationEnum2 value2)";        
        DTValidationResult dtValidResult = testTable(tableName, null);
        assertEquals(1, dtValidResult.getUncovered().length);
    }
    
    @Test
    public void testOverlap() {
        String tableName = "Rules String validationOverlap(TestValidationEnum1 value1, TestValidationEnum2 value2)";        
        DTValidationResult dtValidResult = testTable(tableName, null);
        assertEquals(1, dtValidResult.getOverlappings().length);
    }
    
    @Test
    public void testIntRule() {
        String tableName = "Rules void hello1(int hour)";
        IntRangeDomain intRangeDomain = new IntRangeDomain(0,24);
        Map<String, IDomainAdaptor> domains = new HashMap<String, IDomainAdaptor>();
        IntRangeDomainAdaptor intRangeDomainAdaptor = new IntRangeDomainAdaptor(intRangeDomain);
        domains.put("hour", intRangeDomainAdaptor);
        
        DTValidationResult dtValidResult = testTable(tableName, domains);
        assertEquals(1, dtValidResult.getUncovered().length);
        assertEquals("Param value missing", "hour=24", dtValidResult.getUncovered()[0].getValues().toString());
    }
        
    private DTValidationResult testTable(String tableName, Map<String, IDomainAdaptor> domains) {
        DTValidationResult result = null;
        TableSyntaxNode[] tsns = getTableSyntaxNodes();
        TableSyntaxNode resultTsn = findTable(tableName, tsns);
        if (resultTsn != null) {
            ITableProperties tableProperties  = resultTsn.getTableProperties();
            assertNotNull(tableProperties);
            assertTrue(getJavaWrapper().getCompiledClass().getBindingErrors().length == 0);
            assertTrue(getJavaWrapper().getCompiledClass().getParsingErrors().length == 0);
            
            DecisionTable dt = (DecisionTable) resultTsn.getMember();
            try {
                //System.out.println("Validating <" + tableName+ ">");
                result = DTValidator.validateDT(dt, domains, getJavaWrapper().getOpenClass());
                
                if (result.hasProblems()) {
                    resultTsn.setValidationResult(result);
                    //System.out.println("There are problems in table!!\n");
                } else {
                    //System.out.println("NO PROBLEMS IN TABLE!!!!\n");
                }
            } catch (Exception t) {
                //System.out.println("Exception " + t.getMessage());
            }
        } else {
            fail();
        }
        return result;
    }
    
    @Test
    public void testOk2() {
        String tableName = "Rules void hello2(int currentValue)";        
        IntRangeDomain intRangeDomain = new IntRangeDomain(0,50);
        Map<String, IDomainAdaptor> domains = new HashMap<String, IDomainAdaptor>();
        IntRangeDomainAdaptor intRangeDomainAdaptor = new IntRangeDomainAdaptor(intRangeDomain);
        domains.put("currentValue", intRangeDomainAdaptor);
        
        DTValidationResult dtValidResult = testTable(tableName, domains);
        assertFalse(dtValidResult.hasProblems());
    } 
            


}
