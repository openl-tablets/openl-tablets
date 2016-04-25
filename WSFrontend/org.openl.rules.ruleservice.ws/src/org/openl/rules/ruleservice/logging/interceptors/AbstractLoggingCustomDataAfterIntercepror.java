package org.openl.rules.ruleservice.logging.interceptors;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAfterAdvice;
import org.openl.rules.ruleservice.logging.LoggingCustomData;
import org.openl.rules.ruleservice.logging.LoggingInfo;
import org.openl.rules.ruleservice.logging.LoggingInfoHolder;

public abstract class AbstractLoggingCustomDataAfterIntercepror implements ServiceMethodAfterAdvice<Object> {

    @Override
    public final Object afterReturning(Method method, Object result, Object... args) throws Exception {
        LoggingInfo loggingInfo = LoggingInfoHolder.get();
        LoggingCustomData loggingCustomData = loggingInfo.getLoggingCustomData();
        LoggingCustomData customData;
        if (loggingCustomData == null) {
            customData = populateCustomData(new LoggingCustomData(), args, result, null);
        } else {
            customData = populateCustomData(loggingCustomData, args, result, null);
        }
        loggingInfo.setLoggingCustomData(customData);
        return result;
    }

    @Override
    public final Object afterThrowing(Method method, Exception t, Object... args) throws Exception {
        LoggingInfo loggingInfo = LoggingInfoHolder.get();
        LoggingCustomData loggingCustomData = loggingInfo.getLoggingCustomData();
        LoggingCustomData customData;
        if (loggingCustomData == null) {
            customData = populateCustomData(new LoggingCustomData(), args, null, t);
        } else {
            customData = populateCustomData(loggingCustomData, args, null, t);
        }
        loggingInfo.setLoggingCustomData(customData);
        throw t;
    }

    protected abstract LoggingCustomData populateCustomData(LoggingCustomData loggingCustomData,
            Object[] args,
            Object result,
            Exception rx);
}
