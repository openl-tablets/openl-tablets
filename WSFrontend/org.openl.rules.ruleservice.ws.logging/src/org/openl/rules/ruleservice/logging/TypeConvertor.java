package org.openl.rules.ruleservice.logging;

public interface TypeConvertor<T, S> {
    T convert(S value);
}
