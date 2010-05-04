package org.openl.rules.dt.element;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.runtime.RuleEngineFactory;
import org.openl.runtime.EngineFactory;

public class CommaSeparatedParamsDTTest {

    private static String src = "test/rules/Comma_Separated_Params_DT_Test.xls";
    
    private CommaSeparatedTest test;
    
    @Before
    public void initEngine() {
        EngineFactory<CommaSeparatedTest> engineFactory = new EngineFactory<CommaSeparatedTest>(
                RuleEngineFactory.RULE_OPENL_NAME, src, CommaSeparatedTest.class);
        
        test = engineFactory.newInstance();
    }
    
    @Test
    public void testcommaSeparated() {
        test("firstValue", "comma2", "Good Morning");                
        
        test("value1", "singleValue", "Good Afternoon");                
    }
    
    private void test(String income1, String income2, String expectedResult) {
        String result = test.hello2(income1, income2);        
        assertEquals(expectedResult, result);
    }

}
