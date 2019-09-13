package org.openl.rules.ruleservice.logging;

public interface StoreLoggingDataConvertor<T> {
    T convert(StoreLoggingData storeLoggingData);
}
