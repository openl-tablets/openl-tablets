package org.openl.rules.ruleservice.logging;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.rules.ruleservice.logging.annotation.CustomDateValue1;
import org.openl.rules.ruleservice.logging.annotation.CustomDateValue2;
import org.openl.rules.ruleservice.logging.annotation.CustomDateValue3;
import org.openl.rules.ruleservice.logging.annotation.CustomNumberValue1;
import org.openl.rules.ruleservice.logging.annotation.CustomNumberValue2;
import org.openl.rules.ruleservice.logging.annotation.CustomNumberValue3;
import org.openl.rules.ruleservice.logging.annotation.CustomNumberValue4;
import org.openl.rules.ruleservice.logging.annotation.CustomNumberValue5;
import org.openl.rules.ruleservice.logging.annotation.CustomStringValue1;
import org.openl.rules.ruleservice.logging.annotation.CustomStringValue2;
import org.openl.rules.ruleservice.logging.annotation.CustomStringValue3;
import org.openl.rules.ruleservice.logging.annotation.CustomStringValue4;
import org.openl.rules.ruleservice.logging.annotation.CustomStringValue5;
import org.openl.rules.ruleservice.logging.annotation.DefaultTypeConvertor;
import org.openl.rules.ruleservice.logging.annotation.IncomingTime;
import org.openl.rules.ruleservice.logging.annotation.InputName;
import org.openl.rules.ruleservice.logging.annotation.OutcomingTime;
import org.openl.rules.ruleservice.logging.annotation.Publisher;
import org.openl.rules.ruleservice.logging.annotation.Request;
import org.openl.rules.ruleservice.logging.annotation.Response;
import org.openl.rules.ruleservice.logging.annotation.ServiceName;
import org.openl.rules.ruleservice.logging.annotation.Url;
import org.openl.rules.ruleservice.logging.annotation.UseLoggingInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingInfoMapper {

    private final Logger log = LoggerFactory.getLogger(LoggingInfoMapper.class);

    private final static Set<Class<?>> CUSTOM_ANNOTATIONS;
    private final static Set<Class<?>> MAPPING_ANNOTATIONS;

    static {
        Set<Class<?>> customAnnotations = new HashSet<Class<?>>();
        customAnnotations.add(CustomStringValue1.class);
        customAnnotations.add(CustomStringValue2.class);
        customAnnotations.add(CustomStringValue3.class);
        customAnnotations.add(CustomStringValue4.class);
        customAnnotations.add(CustomStringValue5.class);
        customAnnotations.add(CustomNumberValue1.class);
        customAnnotations.add(CustomNumberValue2.class);
        customAnnotations.add(CustomNumberValue3.class);
        customAnnotations.add(CustomNumberValue4.class);
        customAnnotations.add(CustomNumberValue5.class);
        customAnnotations.add(CustomDateValue1.class);
        customAnnotations.add(CustomDateValue2.class);
        customAnnotations.add(CustomDateValue3.class);
        CUSTOM_ANNOTATIONS = Collections.unmodifiableSet(customAnnotations);

        Set<Class<?>> mappingAnnotations = new HashSet<Class<?>>();
        mappingAnnotations.add(Publisher.class);
        mappingAnnotations.add(IncomingTime.class);
        mappingAnnotations.add(OutcomingTime.class);
        mappingAnnotations.add(InputName.class);
        mappingAnnotations.add(Url.class);
        mappingAnnotations.add(Request.class);
        mappingAnnotations.add(Response.class);
        mappingAnnotations.add(ServiceName.class);
        mappingAnnotations.add(UseLoggingInfo.class);

        MAPPING_ANNOTATIONS = Collections.unmodifiableSet(mappingAnnotations);
    }

    public void map(LoggingInfo loggingInfo, Object target) {
        LoggingCustomData loggingCustomData = loggingInfo.getLoggingCustomData();
        Class<?> targetClass = target.getClass();
        Class<?> klass = targetClass;
        Map<Annotation, Method> customAnnotationMethodMap = new HashMap<Annotation, Method>();
        Map<Annotation, Method> annotationMethodMap = new HashMap<Annotation, Method>();
        while (klass != Object.class) {
            for (final Method method : klass.getDeclaredMethods()) {
                for (final Annotation annotation : method.getAnnotations()) {
                    if (loggingCustomData != null && CUSTOM_ANNOTATIONS.contains(annotation.annotationType())) {
                        if (method.getParameterTypes().length != 1) {
                            if (log.isWarnEnabled()) {
                                log.warn("Please, check that annotated method '" + method
                                    .toGenericString() + "' is setter method!");
                            }
                        }
                        customAnnotationMethodMap.put(annotation, method);
                    }
                    if (MAPPING_ANNOTATIONS.contains(annotation.annotationType())) {
                        if (method.getParameterTypes().length != 1) {
                            if (log.isWarnEnabled()) {
                                log.warn("Please, check that annotated method '" + method
                                    .toGenericString() + "' is setter method!");
                            }
                        }
                        annotationMethodMap.put(annotation, method);
                    }

                }
            }
            klass = klass.getSuperclass();
        }

        for (Entry<Annotation, Method> entry : annotationMethodMap.entrySet()) {
            Annotation annotation = entry.getKey();
            Method method = entry.getValue();
            if (IncomingTime.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingInfo.getIncomingMessageTime());
            }
            if (OutcomingTime.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingInfo.getOutcomingMessageTime());
            }
            if (InputName.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingInfo.getInputName());
            }
            if (ServiceName.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingInfo.getServiceName());
            }
            if (Publisher.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingInfo.getPublisherType().toString());
            }
            if (loggingInfo.getRequestMessage() != null){
                if (Url. class.equals(annotation.annotationType()) && loggingInfo.getRequestMessage().getAddress() != null) {
                    insertValue(loggingInfo,
                        target,
                        annotation,
                        method,
                        loggingInfo.getRequestMessage().getAddress().toString());
                }
                if (Request.class.equals(annotation.annotationType()) && loggingInfo.getResponseMessage().getPayload() != null) {
                    insertValue(loggingInfo,
                        target,
                        annotation,
                        method,
                        loggingInfo.getRequestMessage().getPayload().toString());
                }
            }else{
                log.error("Request message is not present!");
            }
            if (loggingInfo.getResponseMessage() != null){
                if (Response.class.equals(annotation.annotationType()) && loggingInfo.getResponseMessage().getPayload() != null) {
                    insertValue(loggingInfo,
                        target,
                        annotation,
                        method,
                        loggingInfo.getResponseMessage().getPayload().toString());
                }
            }else{
                log.error("Response message is not present!");
            }
            if (UseLoggingInfo.class.equals(annotation.annotationType())) {
                useLoggingInfoInsertValue(loggingInfo, target, annotation, method);
            }
        }

        for (Entry<Annotation, Method> entry : customAnnotationMethodMap.entrySet()) {
            Annotation annotation = entry.getKey();
            Method method = entry.getValue();
            if (CustomStringValue1.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getStringValue1());
            }
            if (CustomStringValue2.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getStringValue2());
            }
            if (CustomStringValue3.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getStringValue3());
            }
            if (CustomStringValue4.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getStringValue4());
            }
            if (CustomStringValue5.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getStringValue5());
            }

            if (CustomNumberValue1.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getNumberValue1());
            }
            if (CustomNumberValue2.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getNumberValue2());
            }
            if (CustomNumberValue3.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getNumberValue3());
            }
            if (CustomNumberValue4.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getNumberValue4());
            }
            if (CustomNumberValue5.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getNumberValue5());
            }

            if (CustomDateValue1.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getDateValue1());
            }
            if (CustomDateValue2.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getDateValue2());
            }
            if (CustomDateValue3.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getDateValue3());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void insertValue(LoggingInfo loggingInfo,
            Object target,
            Annotation annotation,
            Method method,
            Object value) {
        boolean f = false;
        try {
            Method publisherTypeMethod = annotation.annotationType().getMethod("publisherTypes", new Class<?>[] {});
            PublisherType[] publisherTypes = (PublisherType[]) publisherTypeMethod.invoke(annotation);
            for (PublisherType publisherType : publisherTypes) {
                if (publisherType.equals(loggingInfo.getPublisherType())) {
                    f = true;
                    break;
                }
            }
        } catch (Exception e) {
            f = false;
            if (log.isErrorEnabled()) {
                log.error("Invalid annotation!", e);
            }
        }
        if (f) {
            Class<? extends TypeConvertor<?, ?>> typeConvertorClass = null;
            try {
                Method convertorMethod = annotation.annotationType().getMethod("convertor", new Class<?>[] {});
                typeConvertorClass = (Class<? extends TypeConvertor<?, ?>>) convertorMethod.invoke(annotation);
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("Invalid annotation!", e);
                }
                return;
            }
            if (!DefaultTypeConvertor.class.equals(typeConvertorClass)) {
                TypeConvertor<Object, Object> convertor = null;
                try {
                    convertor = (TypeConvertor<Object, Object>) typeConvertorClass.newInstance();
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Type convertor instantiation was failed! Null value used as result value!", e);
                    }
                    value = null;
                }
                if (convertor != null) {
                    try {
                        value = convertor.convert(value);
                    } catch (Exception e) {
                        if (log.isErrorEnabled()) {
                            log.error("Type convertation was failed! Null value used as result value!", e);
                        }
                        value = null;
                    }
                }
            }
            try {
                method.invoke(target, value);
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("Method invoke was failed! Please, check that method annotated correctly!", e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void useLoggingInfoInsertValue(LoggingInfo loggingInfo,
            Object target,
            Annotation annotation,
            Method method) {
        if (annotation instanceof UseLoggingInfo) {
            UseLoggingInfo useLoggingInfo = (UseLoggingInfo) annotation;
            boolean f = false;
            for (PublisherType publisherType : useLoggingInfo.publisherTypes()) {
                if (publisherType.equals(loggingInfo.getPublisherType())) {
                    f = true;
                    break;
                }
            }
            if (f) {
                Class<? extends LoggingInfoConvertor<?>> convertorClass = useLoggingInfo.convertor();
                LoggingInfoConvertor<Object> convertor = null;
                try {
                    convertor = (LoggingInfoConvertor<Object>) convertorClass.newInstance();
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("LoggingInfo convertor instantiation was failed!", e);
                    }
                    return;
                }
                if (convertor != null) {
                    Object convertedValue = null;
                    try {
                        convertedValue = convertor.convert(loggingInfo);
                    } catch (Exception e) {
                        if (log.isErrorEnabled()) {
                            log.error("LoggingInfo convertation was failed! Null value used as convertation result!",
                                e);
                        }
                        convertedValue = null;
                    }
                    try {
                        method.invoke(target, convertedValue);
                    } catch (Exception e) {
                        if (log.isErrorEnabled()) {
                            log.error("Method invoke was failed! Please, check that method '" + method
                                .toGenericString() + "' annotated correctly!", e);
                        }
                    }
                }
            }
        }
    }
}
