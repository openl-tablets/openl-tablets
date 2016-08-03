package org.openl.rules.ruleservice.logging;

public interface TypeConvertor<S, T> {
    T convert(S value);
}
