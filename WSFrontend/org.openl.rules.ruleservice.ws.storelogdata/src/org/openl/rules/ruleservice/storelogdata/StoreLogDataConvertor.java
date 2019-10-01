package org.openl.rules.ruleservice.storelogdata;

public interface StoreLogDataConvertor<T> {
    T convert(StoreLogData storeLogData);
}
