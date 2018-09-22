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
import org.openl.rules.ruleservice.logging.annotation.DefaultDateConvertor;
import org.openl.rules.ruleservice.logging.annotation.DefaultNumberConvertor;
import org.openl.rules.ruleservice.logging.annotation.DefaultStringConvertor;
import org.openl.rules.ruleservice.logging.annotation.DefaultTypeConvertor;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomDateValue1;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomDateValue2;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomDateValue3;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomNumberValue1;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomNumberValue2;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomNumberValue3;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomNumberValue4;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomNumberValue5;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomStringValue1;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomStringValue2;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomStringValue3;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomStringValue4;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomStringValue5;
import org.openl.rules.ruleservice.logging.annotation.SetterIncomingTime;
import org.openl.rules.ruleservice.logging.annotation.SetterInputName;
import org.openl.rules.ruleservice.logging.annotation.SetterOutcomingTime;
import org.openl.rules.ruleservice.logging.annotation.SetterPublisher;
import org.openl.rules.ruleservice.logging.annotation.SetterRequest;
import org.openl.rules.ruleservice.logging.annotation.SetterResponse;
import org.openl.rules.ruleservice.logging.annotation.SetterServiceName;
import org.openl.rules.ruleservice.logging.annotation.SetterUrl;
import org.openl.rules.ruleservice.logging.annotation.SetterValue;
import org.openl.rules.ruleservice.logging.annotation.UseLoggingInfoConvertor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingInfoMapper {

    private final Logger log = LoggerFactory.getLogger(LoggingInfoMapper.class);

    private final static Set<Class<?>> CUSTOM_ANNOTATIONS;
    private final static Set<Class<?>> MAPPING_ANNOTATIONS;

    static {
        Set<Class<?>> customAnnotations = new HashSet<Class<?>>();
        customAnnotations.add(SetterCustomStringValue1.class);
        customAnnotations.add(SetterCustomStringValue2.class);
        customAnnotations.add(SetterCustomStringValue3.class);
        customAnnotations.add(SetterCustomStringValue4.class);
        customAnnotations.add(SetterCustomStringValue5.class);
        customAnnotations.add(SetterCustomNumberValue1.class);
        customAnnotations.add(SetterCustomNumberValue2.class);
        customAnnotations.add(SetterCustomNumberValue3.class);
        customAnnotations.add(SetterCustomNumberValue4.class);
        customAnnotations.add(SetterCustomNumberValue5.class);
        customAnnotations.add(SetterCustomDateValue1.class);
        customAnnotations.add(SetterCustomDateValue2.class);
        customAnnotations.add(SetterCustomDateValue3.class);
        customAnnotations.add(SetterValue.class);
        CUSTOM_ANNOTATIONS = Collections.unmodifiableSet(customAnnotations);

        Set<Class<?>> mappingAnnotations = new HashSet<Class<?>>();
        mappingAnnotations.add(SetterPublisher.class);
        mappingAnnotations.add(SetterIncomingTime.class);
        mappingAnnotations.add(SetterOutcomingTime.class);
        mappingAnnotations.add(SetterInputName.class);
        mappingAnnotations.add(SetterUrl.class);
        mappingAnnotations.add(SetterRequest.class);
        mappingAnnotations.add(SetterResponse.class);
        mappingAnnotations.add(SetterServiceName.class);
        mappingAnnotations.add(UseLoggingInfoConvertor.class);

        MAPPING_ANNOTATIONS = Collections.unmodifiableSet(mappingAnnotations);
    }

    private String methodToString(Method method) {
        String s = method.toGenericString();
        return s.substring(s.indexOf(method.getReturnType().getSimpleName()));
    }

    public void map(LoggingInfo loggingInfo, Object target) {
        LoggingCustomData loggingCustomData = loggingInfo.getLoggingCustomData();
        Class<?> targetClass = target.getClass();
        Class<?> clazz = targetClass;
        Map<Annotation, Method> customAnnotationMethodMap = new HashMap<Annotation, Method>();
        Map<Annotation, Method> annotationMethodMap = new HashMap<Annotation, Method>();
        while (clazz != Object.class) {
            for (final Method method : clazz.getDeclaredMethods()) {
                for (final Annotation annotation : method.getAnnotations()) {
                    if (loggingCustomData != null && CUSTOM_ANNOTATIONS.contains(annotation.annotationType())) {
                        if (method.getParameterTypes().length != 1) {
                            if (log.isWarnEnabled()) {
                                log.warn("Please, check that annotated method '" + methodToString(
                                    method) + "' is setter method!");
                            }
                        }
                        customAnnotationMethodMap.put(annotation, method);
                    }
                    if (MAPPING_ANNOTATIONS.contains(annotation.annotationType())) {
                        if (method.getParameterTypes().length != 1) {
                            if (log.isWarnEnabled()) {
                                log.warn("Please, check that annotated method '" + methodToString(
                                    method) + "' is setter method!");
                            }
                        }
                        annotationMethodMap.put(annotation, method);
                    }

                }
            }
            clazz = clazz.getSuperclass();
        }

        for (Entry<Annotation, Method> entry : annotationMethodMap.entrySet()) {
            Annotation annotation = entry.getKey();
            Method method = entry.getValue();
            if (SetterIncomingTime.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingInfo.getIncomingMessageTime());
            }
            if (SetterOutcomingTime.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingInfo.getOutcomingMessageTime());
            }
            if (SetterInputName.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingInfo.getInputName());
            }
            if (SetterServiceName.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingInfo.getServiceName());
            }
            if (SetterPublisher.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingInfo.getPublisherType().toString());
            }
            if (loggingInfo.getRequestMessage() != null) {
                if (SetterUrl.class
                    .equals(annotation.annotationType()) && loggingInfo.getRequestMessage().getAddress() != null) {
                    insertValue(loggingInfo,
                        target,
                        annotation,
                        method,
                        loggingInfo.getRequestMessage().getAddress().toString());
                }
                if (SetterRequest.class
                    .equals(annotation.annotationType()) && loggingInfo.getResponseMessage().getPayload() != null) {
                    insertValue(loggingInfo,
                        target,
                        annotation,
                        method,
                        loggingInfo.getRequestMessage().getPayload().toString());
                }
            } else {
                log.error("Not found request message in logging info!");
            }
            if (loggingInfo.getResponseMessage() != null) {
                if (SetterResponse.class
                    .equals(annotation.annotationType()) && loggingInfo.getResponseMessage().getPayload() != null) {
                    insertValue(loggingInfo,
                        target,
                        annotation,
                        method,
                        loggingInfo.getResponseMessage().getPayload().toString());
                }
            } else {
                log.error("Not found response message in logging info!");
            }
            if (UseLoggingInfoConvertor.class.equals(annotation.annotationType())) {
                useLoggingInfoInsertValue(loggingInfo, target, annotation, method);
            }
        }

        for (Entry<Annotation, Method> entry : customAnnotationMethodMap.entrySet()) {
            Annotation annotation = entry.getKey();
            Method method = entry.getValue();
            if (SetterValue.class.equals(annotation.annotationType())) {
                SetterValue setterValue = (SetterValue) annotation;
                String key = setterValue.value();
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getValue(key));
            }

            if (SetterCustomStringValue1.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getStringValue1());
            }
            if (SetterCustomStringValue2.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getStringValue2());
            }
            if (SetterCustomStringValue3.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getStringValue3());
            }
            if (SetterCustomStringValue4.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getStringValue4());
            }
            if (SetterCustomStringValue5.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getStringValue5());
            }
            if (SetterCustomNumberValue1.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getNumberValue1());
            }
            if (SetterCustomNumberValue2.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getNumberValue2());
            }
            if (SetterCustomNumberValue3.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getNumberValue3());
            }
            if (SetterCustomNumberValue4.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getNumberValue4());
            }
            if (SetterCustomNumberValue5.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getNumberValue5());
            }

            if (SetterCustomDateValue1.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getDateValue1());
            }
            if (SetterCustomDateValue2.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, method, loggingCustomData.getDateValue2());
            }
            if (SetterCustomDateValue3.class.equals(annotation.annotationType())) {
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
            throw new IllegalStateException(
                "Invalid annotation is used! Property 'publisherTypes' is not found in '" + annotation.getClass()
                    .getSimpleName() + "'!");
        }

        if (!f) {
            return;
        }

        Class<? extends TypeConvertor<?, ?>> typeConvertorClass = null;
        try {
            Method convertorMethod = annotation.annotationType().getMethod("convertor", new Class<?>[] {});
            typeConvertorClass = (Class<? extends TypeConvertor<?, ?>>) convertorMethod.invoke(annotation);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Invalid annotation is used! Property 'convertor' is not found in '" + annotation.getClass()
                    .getSimpleName() + "'!");
        }

        if (!(DefaultTypeConvertor.class.equals(typeConvertorClass) || DefaultStringConvertor.class
            .equals(typeConvertorClass) || DefaultNumberConvertor.class
                .equals(typeConvertorClass) || DefaultDateConvertor.class.equals(typeConvertorClass))) {
            TypeConvertor<Object, Object> convertor = null;
            try {
                convertor = (TypeConvertor<Object, Object>) typeConvertorClass.newInstance();
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error(String.format(
                        "Failed to instantiate a type convertor '%s'! Null value has been used as result value!",
                        typeConvertorClass.getSimpleName()), e);
                }
                value = null;
            }
            if (convertor != null) {
                try {
                    value = convertor.convert(value);
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error(String.format(
                            "Failed on type convertation for method '%s'! Null value has been used as a result value!",
                            methodToString(method)), e);
                    }
                    value = null;
                }
            }
        }
        try {
            method.invoke(target, value);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(
                    String.format("Failed on method '%s' invoke! Please, check that method is annotated correctly!",
                        methodToString(method)),
                    e);
            }
        }

    }

    @SuppressWarnings("unchecked")
    private void useLoggingInfoInsertValue(LoggingInfo loggingInfo,
            Object target,
            Annotation annotation,
            Method method) {
        if (annotation instanceof UseLoggingInfoConvertor) {
            UseLoggingInfoConvertor useLoggingInfo = (UseLoggingInfoConvertor) annotation;
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
                        log.error(String.format("LoggingInfo convertor '%s' instantiation was failed!",
                            convertorClass.getSimpleName()), e);
                    }
                    return;
                }
                if (convertor != null) {
                    Object convertedValue = null;
                    try {
                        convertedValue = convertor.convert(loggingInfo);
                    } catch (Exception e) {
                        if (log.isErrorEnabled()) {
                            log.error(String.format(
                                "Failed on LoggingInfo convertation for method '%s'! Null value has been used as a convertation result!",
                                methodToString(method)), e);
                        }
                        convertedValue = null;
                    }
                    try {
                        method.invoke(target, convertedValue);
                    } catch (Exception e) {
                        if (log.isErrorEnabled()) {
                            log.error(
                                "Failed on method invokation! Please, check that method '" + methodToString(
                                    method) + "' is annotated correctly!",
                                e);
                        }
                    }
                }
            }
        }
    }
}
