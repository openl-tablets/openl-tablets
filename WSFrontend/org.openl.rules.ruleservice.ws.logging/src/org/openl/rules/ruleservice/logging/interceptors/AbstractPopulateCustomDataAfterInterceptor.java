package org.openl.rules.ruleservice.logging.interceptors;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAfterAdvice;
import org.openl.rules.ruleservice.logging.LoggingCustomData;
import org.openl.rules.ruleservice.logging.ObjectSerializer;
import org.openl.rules.ruleservice.logging.RuleServiceLogging;
import org.openl.rules.ruleservice.logging.RuleServiceLoggingHolder;

public abstract class AbstractPopulateCustomDataAfterInterceptor<T> implements ServiceMethodAfterAdvice<T> {
    @SuppressWarnings("unchecked")
    @Override
    public final T afterReturning(Method method, Object result, Object... args) throws Exception {
        RuleServiceLogging ruleServiceLogging = RuleServiceLoggingHolder.get();
        LoggingCustomData loggingCustomData = ruleServiceLogging.getLoggingCustomData();
        LoggingCustomData customData;
        if (loggingCustomData == null) {
            customData = populateCustomData(new LoggingCustomData(), args, result, null);
        } else {
            customData = populateCustomData(loggingCustomData, args, result, null);
        }
        ruleServiceLogging.setLoggingCustomData(customData);
        return (T) result;
    }

    @Override
    public final T afterThrowing(Method method, Exception t, Object... args) throws Exception {
        RuleServiceLogging ruleServiceLogging = RuleServiceLoggingHolder.get();
        LoggingCustomData loggingCustomData = ruleServiceLogging.getLoggingCustomData();
        LoggingCustomData customData;
        if (loggingCustomData == null) {
            customData = populateCustomData(new LoggingCustomData(), args, null, t);
        } else {
            customData = populateCustomData(loggingCustomData, args, null, t);
        }
        ruleServiceLogging.setLoggingCustomData(customData);
        throw t;
    }

    protected final ObjectSerializer getObjectSerializer() {
        RuleServiceLogging ruleServiceLogging = RuleServiceLoggingHolder.get();
        return ruleServiceLogging.getObjectSerializer();
    }

    protected abstract LoggingCustomData populateCustomData(LoggingCustomData loggingCustomData,
            Object[] args,
            Object result,
            Exception ex);
}
