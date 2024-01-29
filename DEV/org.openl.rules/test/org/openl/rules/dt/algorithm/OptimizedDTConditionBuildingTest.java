package org.openl.rules.dt.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.runtime.RulesEngineFactory;

public class OptimizedDTConditionBuildingTest {
    public interface DTConditionBuilding {
        String sayHello(int hour, boolean sayGood, Integer caseNumber);
    }

    private static final String SRC = "test/rules/dt/algorithm/OptimizedDTConditionBuildingRules.xls";

    private DTConditionBuilding instance;

    @BeforeEach
    public void initEngine() {
        RulesEngineFactory<DTConditionBuilding> engineFactory = new RulesEngineFactory<>(SRC,
            DTConditionBuilding.class);

        instance = engineFactory.newEngineInstance();
    }

    @Test
    public void testGoodCase() {
        String result = instance.sayHello(13, true, 2);
        assertEquals("Good Afternoon", result);
    }

}
