package org.openl.rules.ruleservice.logging.interceptors;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAfterAdvice;
import org.openl.rules.ruleservice.logging.LoggingInfo;
import org.openl.rules.ruleservice.logging.RuleServiceLogging;
import org.openl.rules.ruleservice.logging.RuleServiceLoggingHolder;

public abstract class AbstractIgnoreLoggingAfterInterceptor<T> implements ServiceMethodAfterAdvice<T> {
    @SuppressWarnings("unchecked")
    @Override
    public final T afterReturning(Method method, Object result, Object... args) throws Exception {
        RuleServiceLogging ruleServiceLogging = RuleServiceLoggingHolder.get();
        if (isIgnorable(args, result, null, new LoggingInfo(ruleServiceLogging))) {
            ruleServiceLogging.ignore();
        }
        return (T) result;
    }

    @Override
    public final T afterThrowing(Method method, Exception t, Object... args) throws Exception {
        RuleServiceLogging ruleServiceLogging = RuleServiceLoggingHolder.get();
        if (isIgnorable(args, null, t, new LoggingInfo(ruleServiceLogging))) {
            ruleServiceLogging.ignore();
        }
        throw t;
    }

    protected abstract boolean isIgnorable(Object[] args, Object result, Exception rx, LoggingInfo loggingInfo);
}