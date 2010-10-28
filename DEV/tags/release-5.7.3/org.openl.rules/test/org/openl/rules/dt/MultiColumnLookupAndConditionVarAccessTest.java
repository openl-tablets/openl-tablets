package org.openl.rules.dt;

import junit.framework.TestCase;

import org.junit.Assert;
import org.openl.meta.DoubleValue;
import org.openl.rules.runtime.RuleEngineFactory;
import org.openl.runtime.EngineFactory;

public class MultiColumnLookupAndConditionVarAccessTest extends TestCase {

    public interface IMultiColumnLookupTest {

        DoubleValue getBaseRate(int aoi, String deductible);

        int multiColumnTest1(int key1, int key2, int key3);
    }

    private static String src = "test/rules/dt/MultiColumnLookupAndConditionVarAccessTest.xls";

    private IMultiColumnLookupTest test;

    @Override
    protected void setUp() throws Exception {

        EngineFactory<IMultiColumnLookupTest> engineFactory = new EngineFactory<IMultiColumnLookupTest>(RuleEngineFactory.RULE_OPENL_NAME,
            src,
            IMultiColumnLookupTest.class);

        test = engineFactory.makeInstance();

    }

    public void testCombined() {
        DoubleValue res = test.getBaseRate(10000, "$100,000 Deductible");

        Assert.assertEquals(15048.3021, res.doubleValue(), 0.00005);

    }

    public void test1() {

        int res = test.multiColumnTest1(1, 10, 100);

        Assert.assertEquals(123, res);
        
        res = test.multiColumnTest1(6, 20, 200);

        Assert.assertEquals(1700*100 +    1800*10 + 1900, res);
        
    }

    public static void main(String[] args) {
        new MultiColumnLookupAndConditionVarAccessTest().test1();
    }

}
