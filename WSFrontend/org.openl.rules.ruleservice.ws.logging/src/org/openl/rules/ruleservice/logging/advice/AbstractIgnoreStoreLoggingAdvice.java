package org.openl.rules.ruleservice.logging.advice;

import org.openl.rules.ruleservice.logging.LoggingCustomData;
import org.openl.rules.ruleservice.logging.LoggingInfo;
import org.openl.rules.ruleservice.logging.RuleServiceLogging;
import org.openl.rules.ruleservice.logging.RuleServiceLoggingHolder;

public abstract class AbstractIgnoreStoreLoggingAdvice implements StoreLoggingAdvice {

    @Override
    public LoggingCustomData populateCustomData(LoggingCustomData loggingCustomData,
            Object[] args,
            Object result,
            Exception ex) {
        RuleServiceLogging ruleServiceLogging = RuleServiceLoggingHolder.get();
        if (isIgnorable(args, result, null, new LoggingInfo(ruleServiceLogging))) {
            ruleServiceLogging.ignore();
        }
        return loggingCustomData;
    }

    protected abstract boolean isIgnorable(Object[] args, Object result, Exception rx, LoggingInfo loggingInfo);
}