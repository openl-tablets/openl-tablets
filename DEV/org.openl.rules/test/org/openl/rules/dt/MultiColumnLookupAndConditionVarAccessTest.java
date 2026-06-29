package org.openl.rules.dt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.runtime.RulesEngineFactory;

class MultiColumnLookupAndConditionVarAccessTest {

    public interface IMultiColumnLookupTest {

        Double getBaseRate(int aoi, String deductible);

        int multiColumnTest1(int key1, int key2, int key3);
    }

    private static final String SRC = "test/rules/dt/MultiColumnLookupAndConditionVarAccessTest.xls";

    private IMultiColumnLookupTest instance;

    @BeforeEach
    void setUp() throws Exception {
        RulesEngineFactory<IMultiColumnLookupTest> engineFactory = new RulesEngineFactory<>(SRC,
                IMultiColumnLookupTest.class);

        instance = engineFactory.newEngineInstance();

    }

    @Test
    void testCombined() {
        Double res = instance.getBaseRate(10000, "$100,000 Deductible");

        assertEquals(15048.3021, res, 0.00005);

    }

    @Test
    void test1() {

        int res = instance.multiColumnTest1(1, 10, 100);

        assertEquals(123, res);

        res = instance.multiColumnTest1(6, 20, 200);

        assertEquals(1700 * 100 + 1800 * 10 + 1900, res);

    }

    public static void main(String[] args) {
        new MultiColumnLookupAndConditionVarAccessTest().test1();
    }

}
