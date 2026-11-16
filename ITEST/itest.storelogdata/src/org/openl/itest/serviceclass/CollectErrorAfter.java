package org.openl.itest.serviceclass;

import java.util.Map;

import org.openl.rules.ruleservice.storelogdata.advice.StoreLogDataAdvice;

/**
 * Collects the message of the failure after the rule runs.
 */
public class CollectErrorAfter implements StoreLogDataAdvice {
    @Override
    public void prepare(Map<String, Object> values, Object[] args, Object result, Exception ex) {
        values.put("errorMessage", ex == null ? null : ex.getMessage());
    }
}
