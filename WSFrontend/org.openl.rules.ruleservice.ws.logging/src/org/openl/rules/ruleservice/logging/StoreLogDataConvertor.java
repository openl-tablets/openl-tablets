package org.openl.rules.ruleservice.logging;

public interface StoreLogDataConvertor<T> {
    T convert(StoreLogData storeLogData);
}
