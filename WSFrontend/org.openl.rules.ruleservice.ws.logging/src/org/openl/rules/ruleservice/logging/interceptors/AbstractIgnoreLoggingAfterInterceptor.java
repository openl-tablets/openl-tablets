package org.openl.rules.ruleservice.logging.interceptors;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAfterAdvice;
import org.openl.rules.ruleservice.logging.LoggingInfo;
import org.openl.rules.ruleservice.logging.RuleServiceLoggingInfo;
import org.openl.rules.ruleservice.logging.RuleServiceLoggingInfoHolder;

public abstract class AbstractIgnoreLoggingAfterInterceptor implements ServiceMethodAfterAdvice<Object> {
    @Override
    public final Object afterReturning(Method method, Object result, Object... args) throws Exception {
        RuleServiceLoggingInfo ruleServiceLoggingInfo = RuleServiceLoggingInfoHolder.get();
        if (isIgnorable(args, result, null, new LoggingInfo(ruleServiceLoggingInfo))) {
            ruleServiceLoggingInfo.ignore();
        }
        return result;
    }

    @Override
    public final Object afterThrowing(Method method, Exception t, Object... args) throws Exception {
        RuleServiceLoggingInfo ruleServiceLoggingInfo = RuleServiceLoggingInfoHolder.get();
        if (isIgnorable(args, null, t, new LoggingInfo(ruleServiceLoggingInfo))) {
            ruleServiceLoggingInfo.ignore();
        }
        throw t;
    }

    protected abstract boolean isIgnorable(Object[] args, Object result, Exception rx, LoggingInfo loggingInfo);
}