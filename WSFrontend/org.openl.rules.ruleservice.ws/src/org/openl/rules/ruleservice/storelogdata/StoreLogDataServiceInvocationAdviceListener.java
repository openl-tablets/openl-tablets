package org.openl.rules.ruleservice.storelogdata;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import org.openl.binding.MethodUtil;
import org.openl.rules.ruleservice.core.interceptors.ServiceInvocationAdviceListener;
import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAdvice;
import org.openl.rules.ruleservice.storelogdata.StoreLogData;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataHolder;
import org.openl.rules.ruleservice.storelogdata.advice.ObjectSerializerAware;
import org.openl.rules.ruleservice.storelogdata.advice.StoreLogDataAdvice;
import org.openl.rules.ruleservice.storelogdata.annotation.PrepareStoreLogData;
import org.openl.rules.ruleservice.storelogdata.annotation.PrepareStoreLogDatas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoreLogDataServiceInvocationAdviceListener implements ServiceInvocationAdviceListener {
    private final Logger log = LoggerFactory.getLogger(StoreLogDataServiceInvocationAdviceListener.class);

    private boolean storeLoggingEnabled = false;

    public boolean isStoreLoggingEnabled() {
        return storeLoggingEnabled;
    }

    public void setStoreLoggingEnabled(boolean storeLoggingEnabled) {
        this.storeLoggingEnabled = storeLoggingEnabled;
    }

    public void process(Method interfaceMethod,
            Object[] args,
            Object result,
            Exception lastOccuredException,
            Predicate<PrepareStoreLogData> predicate) {
        PrepareStoreLogDatas storeLoggings = interfaceMethod.getAnnotation(PrepareStoreLogDatas.class);
        if (storeLoggings != null) {
            StoreLogData storeLogData = null;
            for (PrepareStoreLogData storeLogging : storeLoggings.value()) {
                if (predicate.test(storeLogging)) {
                    StoreLogDataAdvice storeLogDataAdvice = null;
                    try {
                        storeLogDataAdvice = storeLogging.value().newInstance();
                        if (storeLogDataAdvice instanceof ObjectSerializerAware) {
                            ObjectSerializerAware objectSerializerAware = (ObjectSerializerAware) storeLogDataAdvice;
                            if (storeLogData == null) {
                                storeLogData = StoreLogDataHolder.get(); // Lazy local
                                                                                 // variable
                                // initialization
                            }
                            objectSerializerAware.setObjectSerializer(storeLogData.getObjectSerializer());
                        }
                    } catch (Exception e) {
                        String msg = String.format(
                            "Failed to instantiate store log data advice for '%s' method. Please, check that '%s' class is not abstact and has a default constructor.",
                            MethodUtil.printQualifiedMethodName(interfaceMethod),
                            storeLogging.value().getTypeName());
                        log.error(msg, e);
                    }
                    if (storeLogDataAdvice != null) {
                        if (storeLogData == null) {
                            storeLogData = StoreLogDataHolder.get(); // Lazy local variable
                                                                             // initialization
                        }

                        storeLogDataAdvice
                            .prepare(storeLogData.getCustomValues(), args, result, lastOccuredException);
                    }
                }
            }
        }
    }

    @Override
    public void beforeServiceMethodAdvice(ServiceMethodAdvice serviceMethodAdvice,
            Method interfaceMethod,
            Object[] args,
            Object result,
            Exception lastOccuredException) {
        if (isStoreLoggingEnabled()) {
            process(interfaceMethod,
                args,
                result,
                lastOccuredException,
                e -> e.before() && e.bindToServiceMethodAdvice().equals(serviceMethodAdvice.getClass()));
        }
    }

    @Override
    public void afterServiceMethodAdvice(ServiceMethodAdvice serviceMethodAdvice,
            Method interfaceMethod,
            Object[] args,
            Object result,
            Exception lastOccuredException) {
        if (isStoreLoggingEnabled()) {
            process(interfaceMethod,
                args,
                result,
                lastOccuredException,
                e -> !e.before() && e.bindToServiceMethodAdvice().equals(serviceMethodAdvice.getClass()));
        }
    }

    @Override
    public void beforeMethodInvocation(Method interfaceMethod,
            Object[] args,
            Object result,
            Exception lastOccuredException) {
        if (isStoreLoggingEnabled()) {
            process(interfaceMethod,
                args,
                result,
                lastOccuredException,
                e -> e.before() && e.bindToServiceMethodAdvice().equals(PrepareStoreLogData.Default.class));
        }
    }

    @Override
    public void afterMethodInvocation(Method interfaceMethod,
            Object[] args,
            Object result,
            Exception lastOccuredException) {
        if (isStoreLoggingEnabled()) {
            process(interfaceMethod,
                args,
                result,
                lastOccuredException,
                e -> !e.before() && e.bindToServiceMethodAdvice().equals(PrepareStoreLogData.Default.class));
        }
    }

}
