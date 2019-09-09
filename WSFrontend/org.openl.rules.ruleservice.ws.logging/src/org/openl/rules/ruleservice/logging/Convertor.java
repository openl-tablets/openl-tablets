package org.openl.rules.ruleservice.logging;

public interface Convertor<S, T> {
    T convert(S value);
}
