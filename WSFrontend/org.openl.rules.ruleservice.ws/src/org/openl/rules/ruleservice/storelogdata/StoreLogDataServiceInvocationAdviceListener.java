package org.openl.rules.ruleservice.storelogdata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.openl.binding.MethodUtil;
import org.openl.rules.ruleservice.core.interceptors.ServiceInvocationAdviceListener;
import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAdvice;
import org.openl.rules.ruleservice.storelogdata.advice.ObjectSerializerAware;
import org.openl.rules.ruleservice.storelogdata.advice.StoreLogDataAdvice;
import org.openl.rules.ruleservice.storelogdata.annotation.InjectObjectSerializer;
import org.openl.rules.ruleservice.storelogdata.annotation.PrepareStoreLogData;

@Component
public class StoreLogDataServiceInvocationAdviceListener implements ServiceInvocationAdviceListener {
    private final Logger log = LoggerFactory.getLogger(StoreLogDataServiceInvocationAdviceListener.class);

    @Autowired
    private StoreLogDataManager storeLogDataManager;

    public void process(Method interfaceMethod,
                        Object[] args,
                        Object result,
                        Exception lastOccurredException,
                        Instantiator postProcessAdvice,
                        Predicate<PrepareStoreLogData> predicate) {

        PrepareStoreLogData[] annotations = interfaceMethod.getAnnotationsByType(PrepareStoreLogData.class);
        Collection<Runnable> destroyFunctions = new ArrayList<>();
        try {
            StoreLogData storeLogData = StoreLogDataHolder.get();
            IdentityHashMap<Inject<?>, Object> cache = new IdentityHashMap<>();
            for (PrepareStoreLogData storeLogging : annotations) {
                if (predicate.test(storeLogging)) {
                    StoreLogDataAdvice storeLogDataAdvice = null;
                    Class<? extends StoreLogDataAdvice> clazz = storeLogging.value();
                    try {
                        storeLogDataAdvice = postProcessAdvice.instantiate(clazz);
                        injectObjectSerializer(storeLogData.getObjectSerializer(), storeLogDataAdvice);
                        processAwareInterfaces(interfaceMethod, storeLogDataAdvice, cache, destroyFunctions);
                    } catch (Exception e) {
                        String msg = String.format(
                                "Failed to instantiate store log data advice for method '%s'. Please, check that class '%s' is not abstract and has a default constructor.",
                                MethodUtil.printQualifiedMethodName(interfaceMethod),
                                clazz.getTypeName());
                        log.error(msg, e);
                    }
                    if (storeLogDataAdvice != null) {
                        storeLogDataAdvice.prepare(storeLogData.getCustomValues(), args, result, lastOccurredException);
                    }
                }
            }
        } finally {
            destroyFunctions.forEach(Runnable::run);
        }
    }

    private void processAwareInterfaces(Method interfaceMethod,
                                        StoreLogDataAdvice storeLogDataAdvice,
                                        IdentityHashMap<Inject<?>, Object> cache,
                                        Collection<Runnable> destroyFunctions) {
        for (var storeLogDataService : storeLogDataManager.getServices()) {
            for (var inject : storeLogDataService.additionalInjects()) {
                var annotationClass = inject.getAnnotationClass();
                try {
                    var resource = cache.get(inject);
                    if (resource == null) {
                        var resource1 = inject(storeLogDataAdvice, annotationClass,
                                e -> inject.getResource(interfaceMethod, e));
                        cache.put(inject, resource1);
                        if (resource1 != null) {
                            destroyFunctions.add(() -> inject.destroy(resource1));
                        }
                    } else {
                        inject(storeLogDataAdvice, annotationClass, e -> resource);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log.error("Failed to inject a resource through annotation '{}'",
                            annotationClass.getTypeName(),
                            e);
                }
            }
        }
    }

    private void injectObjectSerializer(ObjectSerializer objectSerializer, StoreLogDataAdvice storeLogDataAdvice) {
        if (storeLogDataAdvice instanceof ObjectSerializerAware) {
            ((ObjectSerializerAware) storeLogDataAdvice).setObjectSerializer(objectSerializer);
        }
        try {
            inject(storeLogDataAdvice, InjectObjectSerializer.class, e -> objectSerializer);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to inject a resource through @InjectObjectSerializer annotation.", e);
        }
    }

    private Object inject(Object target,
                          Class<? extends Annotation> annotationClass,
                          Function<Annotation, Object> supplier) throws IllegalAccessException, InvocationTargetException {
        if (annotationClass != null) {
            Class<?> cls = target.getClass();
            Object resource = null;
            boolean initialized = false;
            while (cls != Object.class) {
                for (Field field : cls.getDeclaredFields()) {
                    Annotation annotation = field.getAnnotation(annotationClass);
                    if (annotation != null) {
                        if (!initialized) {
                            resource = supplier.apply(annotation);
                            if (resource == null) {
                                return null;
                            }
                            initialized = true;
                        }
                        field.setAccessible(true);
                        field.set(target, resource);
                    }
                }
                cls = cls.getSuperclass();
            }
            for (Method method : target.getClass().getMethods()) {
                if (method.getParameterCount() == 1) {
                    Annotation annotation = method.getAnnotation(annotationClass);
                    if (annotation != null) {
                        if (!initialized) {
                            resource = supplier.apply(annotation);
                            if (resource == null) {
                                return null;
                            }
                            initialized = true;
                        }
                        method.invoke(target, resource);
                    }
                }
            }
            return resource;
        }
        return null;
    }

    @Override
    public void beforeServiceMethodAdvice(ServiceMethodAdvice serviceMethodAdvice,
                                          Method interfaceMethod,
                                          Object[] args,
                                          Object result,
                                          Exception lastOccurredException,
                                          Instantiator postProcessAdvice) {
        if (storeLogDataManager.isEnabled()) {
            process(interfaceMethod,
                    args,
                    result,
                    lastOccurredException,
                    postProcessAdvice,
                    e -> e.before() && e.bindToServiceMethodAdvice().equals(serviceMethodAdvice.getClass()));
        }
    }

    @Override
    public void afterServiceMethodAdvice(ServiceMethodAdvice serviceMethodAdvice,
                                         Method interfaceMethod,
                                         Object[] args,
                                         Object result,
                                         Exception lastOccurredException,
                                         Instantiator postProcessAdvice) {
        if (storeLogDataManager.isEnabled()) {
            process(interfaceMethod,
                    args,
                    result,
                    lastOccurredException,
                    postProcessAdvice,
                    e -> !e.before() && e.bindToServiceMethodAdvice().equals(serviceMethodAdvice.getClass()));
        }
    }

    @Override
    public void beforeMethodInvocation(Method interfaceMethod,
                                       Object[] args,
                                       Object result,
                                       Exception ex,
                                       Instantiator postProcessAdvice) {
        if (storeLogDataManager.isEnabled()) {
            process(interfaceMethod,
                    args,
                    result,
                    ex,
                    postProcessAdvice,
                    e -> e.before() && e.bindToServiceMethodAdvice().equals(PrepareStoreLogData.Default.class));
        }
    }

    @Override
    public void afterMethodInvocation(Method interfaceMethod,
                                      Object[] args,
                                      Object result,
                                      Exception lastOccurredException,
                                      Instantiator postProcessAdvice) {
        if (storeLogDataManager.isEnabled()) {
            process(interfaceMethod,
                    args,
                    result,
                    lastOccurredException,
                    postProcessAdvice,
                    e -> !e.before() && e.bindToServiceMethodAdvice().equals(PrepareStoreLogData.Default.class));
        }
    }

}
