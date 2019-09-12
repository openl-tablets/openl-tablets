package org.openl.rules.ruleservice.logging.advice;

import org.openl.rules.ruleservice.logging.CustomData;
import org.openl.rules.ruleservice.logging.StoreLoggingData;
import org.openl.rules.ruleservice.logging.RuleServiceStoreLoggingData;
import org.openl.rules.ruleservice.logging.RuleServiceStoreLoggingDataolder;

public abstract class AbstractIgnoreStoreLoggingAdvice implements StoreLoggingAdvice {

    @Override
    public CustomData populateCustomData(CustomData customData,
            Object[] args,
            Object result,
            Exception ex) {
        RuleServiceStoreLoggingData ruleServiceStoreLoggingData = RuleServiceStoreLoggingDataolder.get();
        if (isIgnorable(args, result, null, new StoreLoggingData(ruleServiceStoreLoggingData))) {
            ruleServiceStoreLoggingData.ignore();
        }
        return customData;
    }

    protected abstract boolean isIgnorable(Object[] args,
            Object result,
            Exception rx,
            StoreLoggingData storeLoggingData);
}