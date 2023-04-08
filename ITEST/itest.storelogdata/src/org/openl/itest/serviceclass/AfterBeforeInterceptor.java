package org.openl.itest.serviceclass;

import java.util.Map;

import org.openl.rules.ruleservice.storelogdata.advice.StoreLogDataAdvice;

public class AfterBeforeInterceptor implements StoreLogDataAdvice {
    @Override
    public void prepare(Map<String, Object> values, Object[] args, Object result, Exception ex) {
        values.put("boolValue1", result == null);
        values.put("intValue2", args[1]);
    }
}
