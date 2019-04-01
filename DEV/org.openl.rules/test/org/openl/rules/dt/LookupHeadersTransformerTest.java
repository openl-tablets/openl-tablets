package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.openl.meta.DoubleValue;
import org.openl.rules.runtime.RulesEngineFactory;

public class LookupHeadersTransformerTest {
    interface ILookupTableTest {

        DoubleValue getFirstReturn(String country, String region, String brand, String model);

        DoubleValue getSecondReturn(String country, String region, String brand, String model);

        DoubleValue getNormalReturn(String country, String region, String brand, String model);

        DoubleValue getThreeCond(String country, String region, String brand, String model, int milesNumber);
    }

    private static final String SRC = "test/rules/dt/lookup/LookupHeadersTransformerTest.xls";

    private ILookupTableTest instance;

    @Before
    public void initEngine() {
        RulesEngineFactory<ILookupTableTest> engineFactory = new RulesEngineFactory<>(SRC, ILookupTableTest.class);

        instance = engineFactory.newEngineInstance();
    }

    // @Test
    public void testFirstReturn() {
        DoubleValue result = instance.getFirstReturn("Belarus", "Minsk", "BMW", "Z4 sDrive35i");
        assertEquals(39655, result.intValue());
    }

    // @Test
    public void testSecondReturn() {
        DoubleValue result = instance.getSecondReturn("Belarus", "Vitebsk", "BMW", "Z4 sDrive35i");
        assertEquals(39655, result.intValue());
    }

    // @Test
    public void testNormalReturn() {
        DoubleValue result = instance.getNormalReturn("Belarus", "Minsk", "BMW", "Z4 sDrive35i");
        assertEquals(39655, result.intValue());
    }

    // @Test
    public void testThreeCond() {
        DoubleValue result = instance.getThreeCond("Belarus", "Minsk", "BMW", "Z4 sDrive35i", 2000);
        assertEquals(39655, result.intValue());
    }

}
