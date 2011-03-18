package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.runtime.RuleEngineFactory;
import org.openl.runtime.EngineFactory;

public class LookupHeadersTransformerTest {
    public interface ILookupTableTest {
        
        DoubleValue getFirstReturn(String country, String region, String brand, String model);
        
        DoubleValue getSecondReturn(String country, String region, String brand, String model);
        
        DoubleValue getNormalReturn(String country, String region, String brand, String model);
        
        DoubleValue getThreeCond(String country, String region, String brand, String model, int milesNumber);
    }
    
    private static String src = "test/rules/dt/lookup/LookupHeadersTransformerTest.xls";
    
    private ILookupTableTest test;
    
    @Before
    public void initEngine() {
        EngineFactory<ILookupTableTest> engineFactory = new EngineFactory<ILookupTableTest>(
                RuleEngineFactory.RULE_OPENL_NAME, src, ILookupTableTest.class);
        
        test = engineFactory.makeInstance();
    }
    
    @Test
    public void testFirstReturn() {
        DoubleValue result = test.getFirstReturn("Belarus", "Minsk", "BMW", "Z4 sDrive35i");
        assertEquals(39655, result.intValue());
    }
    
    @Test
    public void testSecondReturn() {
        DoubleValue result = test.getSecondReturn("Belarus", "Minsk", "BMW", "Z4 sDrive35i");
        assertEquals(39655, result.intValue());
    }
    
    @Test
    public void testNormalReturn() {
        DoubleValue result = test.getNormalReturn("Belarus", "Minsk", "BMW", "Z4 sDrive35i");
        assertEquals(39655, result.intValue());
    }
    
    @Test
    public void testThreeCond() {
        DoubleValue result = test.getThreeCond("Belarus", "Minsk", "BMW", "Z4 sDrive35i", 2000);
        assertEquals(39655, result.intValue());
    }

}
