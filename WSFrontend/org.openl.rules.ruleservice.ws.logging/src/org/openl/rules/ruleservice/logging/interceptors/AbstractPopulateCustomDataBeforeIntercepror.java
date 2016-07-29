package org.openl.rules.ruleservice.logging.interceptors;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodBeforeAdvice;
import org.openl.rules.ruleservice.logging.LoggingCustomData;
import org.openl.rules.ruleservice.logging.RuleServiceLoggingInfo;
import org.openl.rules.ruleservice.logging.RuleServiceLoggingInfoHolder;

public abstract class AbstractPopulateCustomDataBeforeIntercepror implements ServiceMethodBeforeAdvice {
    @Override
    public final void before(Method method, Object proxy, Object... args) throws Throwable {
        RuleServiceLoggingInfo loggingInfo = RuleServiceLoggingInfoHolder.get();
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
