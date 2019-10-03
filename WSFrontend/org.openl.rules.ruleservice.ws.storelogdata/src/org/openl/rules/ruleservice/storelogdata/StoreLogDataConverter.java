package org.openl.rules.ruleservice.storelogdata;

public interface StoreLogDataConverter<T> {
    T convert(StoreLogData storeLogData);
}
