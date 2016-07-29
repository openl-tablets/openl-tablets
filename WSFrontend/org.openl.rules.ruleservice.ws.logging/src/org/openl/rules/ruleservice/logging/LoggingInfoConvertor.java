package org.openl.rules.ruleservice.logging;

public interface LoggingInfoConvertor<T> {
    T convert(LoggingInfo loggingInfo);
}
