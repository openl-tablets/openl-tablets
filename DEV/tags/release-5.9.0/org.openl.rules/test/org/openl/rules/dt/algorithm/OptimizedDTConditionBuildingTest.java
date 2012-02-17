package org.openl.rules.dt.algorithm;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.runtime.RuleEngineFactory;

public class OptimizedDTConditionBuildingTest {
    public interface DTConditionBuilding {
        String sayHello(int hour, boolean sayGood, Integer caseNumber);
    }

    private static String src = "test/rules/dt/algorithm/OptimizedDTConditionBuildingRules.xls";

    private DTConditionBuilding testInstance;

    @Before
    public void initEngine() {
        RuleEngineFactory<DTConditionBuilding> engineFactory = new RuleEngineFactory<DTConditionBuilding>(src,
            DTConditionBuilding.class);

        testInstance = engineFactory.makeInstance();
    }

    @Test
    public void testGoodCase() {
        String result = testInstance.sayHello(13, true, 2);
        assertEquals("Good Afternoon", result);
    }

}
