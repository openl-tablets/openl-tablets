package org.openl.rules.dt;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.runtime.RulesEngineFactory;

public class MultiColumnLookupAndConditionVarAccessTest {

    public interface IMultiColumnLookupTest {

        DoubleValue getBaseRate(int aoi, String deductible);

        int multiColumnTest1(int key1, int key2, int key3);
    }

    private static final String SRC = "test/rules/dt/MultiColumnLookupAndConditionVarAccessTest.xls";

    private IMultiColumnLookupTest instance;

    @Before
    public void setUp() throws Exception {
        RulesEngineFactory<IMultiColumnLookupTest> engineFactory = new RulesEngineFactory<IMultiColumnLookupTest>(SRC,
            IMultiColumnLookupTest.class);

        instance = engineFactory.newEngineInstance();

    }

    @Test
    public void testCombined() {
        DoubleValue res = instance.getBaseRate(10000, "$100,000 Deductible");

        Assert.assertEquals(15048.3021, res.doubleValue(), 0.00005);

    }

    @Test
    public void test1() {

        int res = instance.multiColumnTest1(1, 10, 100);

        Assert.assertEquals(123, res);

        res = instance.multiColumnTest1(6, 20, 200);

        Assert.assertEquals(1700 * 100 + 1800 * 10 + 1900, res);

    }

    public static void main(String[] args) {
        new MultiColumnLookupAndConditionVarAccessTest().test1();
    }

}
