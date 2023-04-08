package org.openl.itest.serviceclass;

import java.util.Map;

import org.openl.rules.ruleservice.storelogdata.advice.StoreLogDataAdvice;

public class BeforeMethod implements StoreLogDataAdvice {
    @Override
    public void prepare(Map<String, Object> values, Object[] args, Object result, Exception ex) {
        values.put("intValueToString", args[1]);
        values.put("intValue3", args[1]);
    }
}
