package org.openl.rules.dt.algorithm;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.runtime.RulesEngineFactory;

public class OptimizedDTConditionBuildingTest {
    public interface DTConditionBuilding {
        String sayHello(int hour, boolean sayGood, Integer caseNumber);
    }

    private static final String SRC = "test/rules/dt/algorithm/OptimizedDTConditionBuildingRules.xls";

    private DTConditionBuilding instance;

    @Before
    public void initEngine() {
        RulesEngineFactory<DTConditionBuilding> engineFactory = new RulesEngineFactory<DTConditionBuilding>(SRC,
            DTConditionBuilding.class);

        instance = engineFactory.newEngineInstance();
    }

    @Test
    public void testGoodCase() {
        String result = instance.sayHello(13, true, 2);
        assertEquals("Good Afternoon", result);
    }

}
