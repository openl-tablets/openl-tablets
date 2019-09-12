package org.openl.rules.ruleservice.logging;

public interface StoreLoggingConvertor<T> {
    T convert(StoreLoggingData storeLoggingData);
}
