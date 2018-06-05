package org.openl.rules.ruleservice.logging.interceptors;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodBeforeAdvice;
import org.openl.rules.ruleservice.logging.LoggingCustomData;
import org.openl.rules.ruleservice.logging.ObjectSerializer;
import org.openl.rules.ruleservice.logging.RuleServiceLogging;
import org.openl.rules.ruleservice.logging.RuleServiceLoggingHolder;

public abstract class AbstractPopulateCustomDataBeforeInterceptor implements ServiceMethodBeforeAdvice {
    @Override
    public final void before(Method method, Object proxy, Object... args) throws Throwable {
        RuleServiceLogging ruleServiceLogging = RuleServiceLoggingHolder.get();
        LoggingCustomData loggingCustomData = ruleServiceLogging.getLoggingCustomData();
        LoggingCustomData customData;
        if (loggingCustomData == null) {
            customData = populateCustomData(new LoggingCustomData(), args);
        } else {
            customData = populateCustomData(loggingCustomData, args);
        }
        ruleServiceLogging.setLoggingCustomData(customData);
    }
    
    protected final ObjectSerializer getObjectSerializer() {
        RuleServiceLogging ruleServiceLogging = RuleServiceLoggingHolder.get();
        return ruleServiceLogging.getObjectSerializer();
    }

    protected abstract LoggingCustomData populateCustomData(LoggingCustomData loggingCustomData, Object[] args);
}
