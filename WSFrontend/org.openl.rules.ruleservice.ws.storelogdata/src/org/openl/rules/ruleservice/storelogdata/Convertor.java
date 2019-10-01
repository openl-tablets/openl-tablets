package org.openl.rules.ruleservice.storelogdata;

public interface Convertor<S, T> {
    T convert(S value);
}
