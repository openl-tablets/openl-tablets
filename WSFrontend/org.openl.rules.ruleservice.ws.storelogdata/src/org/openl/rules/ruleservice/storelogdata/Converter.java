package org.openl.rules.ruleservice.storelogdata;

public interface Converter<S, T> {
    T apply(S value);
}
