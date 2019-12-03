package org.openl.rules.dt;

import java.util.HashMap;
import java.util.Map;

public class DecisionTableRuntimePool {

    private Map<String, Object> conditionExecutionPool;

    public void pushConditionExecutionResultToPool(String conditionName, Object result) {
        if (conditionExecutionPool == null) {
            conditionExecutionPool = new HashMap<>();
        }
        conditionExecutionPool.put(conditionName, result);
    }

    public Object getConditionExecutionResult(String conditionName) {
        if (conditionExecutionPool == null) {
            return null;
        }
        return conditionExecutionPool.get(conditionName);
    }

}
