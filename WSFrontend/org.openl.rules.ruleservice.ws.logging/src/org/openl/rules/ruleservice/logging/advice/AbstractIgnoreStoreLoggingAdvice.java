package org.openl.rules.ruleservice.logging.advice;

import org.openl.rules.ruleservice.logging.CustomData;
import org.openl.rules.ruleservice.logging.StoreLoggingData;
import org.openl.rules.ruleservice.logging.StoreLoggingDataImpl;
import org.openl.rules.ruleservice.logging.RuleServiceStoreLoggingData;
import org.openl.rules.ruleservice.logging.RuleServiceStoreLoggingDataHolder;

public abstract class AbstractIgnoreStoreLoggingAdvice implements StoreLoggingAdvice {

    @Override
    public CustomData populateCustomData(CustomData customData, Object[] args, Object result, Exception ex) {
        RuleServiceStoreLoggingData ruleServiceStoreLoggingData = RuleServiceStoreLoggingDataHolder.get();
        if (isIgnorable(args, result, null, new StoreLoggingDataImpl(ruleServiceStoreLoggingData))) {
            ruleServiceStoreLoggingData.ignore();
        }
        return customData;
    }

    protected abstract boolean isIgnorable(Object[] args,
            Object result,
            Exception rx,
            StoreLoggingData storeLoggingData);
}