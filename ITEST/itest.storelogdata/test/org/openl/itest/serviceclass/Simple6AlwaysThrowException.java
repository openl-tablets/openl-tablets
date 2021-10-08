package org.openl.itest.serviceclass;

import java.util.Map;

import org.openl.rules.ruleservice.storelogdata.advice.StoreLogDataAdvice;

public class Simple6AlwaysThrowException implements StoreLogDataAdvice {

    @Override
    public void prepare(Map<String, Object> values, Object[] args, Object result, Exception ex) {
        throw new IllegalStateException("Wake up, Neo!");
    }
}
