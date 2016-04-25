package org.openl.rules.ruleservice.logging.interceptors;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodBeforeAdvice;
import org.openl.rules.ruleservice.logging.LoggingCustomData;
import org.openl.rules.ruleservice.logging.LoggingInfo;
import org.openl.rules.ruleservice.logging.LoggingInfoHolder;

public abstract class AbstractLoggingCustomDataBeforeIntercepror implements ServiceMethodBeforeAdvice {
    @Override
    public final void before(Method method, Object proxy, Object... args) throws Throwable {
        LoggingInfo loggingInfo = LoggingInfoHolder.get();
        LoggingCustomData loggingCustomData = loggingInfo.getLoggingCustomData();
        LoggingCustomData customData;
        if (loggingCustomData == null) {
            customData = populateCustomData(new LoggingCustomData(), args);
        } else {
            customData = populateCustomData(loggingCustomData, args);
        }
        loggingInfo.setLoggingCustomData(customData);
    }

    protected abstract LoggingCustomData populateCustomData(LoggingCustomData loggingCustomData, Object[] args);
}
