package org.openl.rules.ruleservice.logging;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openl.binding.MethodUtil;
import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.rules.ruleservice.logging.annotation.CustomValue;
import org.openl.rules.ruleservice.logging.annotation.DefaultConvertor;
import org.openl.rules.ruleservice.logging.annotation.DefaultDateConvertor;
import org.openl.rules.ruleservice.logging.annotation.DefaultNumberConvertor;
import org.openl.rules.ruleservice.logging.annotation.DefaultStringConvertor;
import org.openl.rules.ruleservice.logging.annotation.IncomingTime;
import org.openl.rules.ruleservice.logging.annotation.InputName;
import org.openl.rules.ruleservice.logging.annotation.OutcomingTime;
import org.openl.rules.ruleservice.logging.annotation.Publisher;
import org.openl.rules.ruleservice.logging.annotation.Request;
import org.openl.rules.ruleservice.logging.annotation.Response;
import org.openl.rules.ruleservice.logging.annotation.ServiceName;
import org.openl.rules.ruleservice.logging.annotation.Url;
import org.openl.rules.ruleservice.logging.annotation.WithLoggingInfoConvertor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingInfoMapper {

    private final Logger log = LoggerFactory.getLogger(LoggingInfoMapper.class);

    private static final Set<Class<? extends Annotation>> CUSTOM_ANNOTATIONS;
    private static final Set<Class<? extends Annotation>> MAPPING_ANNOTATIONS;

    static {
        Set<Class<? extends Annotation>> customAnnotations = new HashSet<>();
        customAnnotations.add(CustomValue.class);
        CUSTOM_ANNOTATIONS = Collections.unmodifiableSet(customAnnotations);

        Set<Class<? extends Annotation>> mappingAnnotations = new HashSet<>();
        mappingAnnotations.add(Publisher.class);
        mappingAnnotations.add(IncomingTime.class);
        mappingAnnotations.add(OutcomingTime.class);
        mappingAnnotations.add(InputName.class);
        mappingAnnotations.add(Url.class);
        mappingAnnotations.add(Request.class);
        mappingAnnotations.add(Response.class);
        mappingAnnotations.add(ServiceName.class);
        mappingAnnotations.add(WithLoggingInfoConvertor.class);

        MAPPING_ANNOTATIONS = Collections.unmodifiableSet(mappingAnnotations);
    }

    public void map(LoggingInfo loggingInfo, Object target) {
        LoggingCustomData loggingCustomData = loggingInfo.getLoggingCustomData();
        Class<?> targetClass = target.getClass();
        Class<?> clazz = targetClass;
        Map<Annotation, AnnotatedElement> customAnnotationElementsMap = new HashMap<>();
        Map<Annotation, AnnotatedElement> annotationElementsMap = new HashMap<>();
        while (clazz != Object.class) {
            for (final Method method : clazz.getDeclaredMethods()) {
                processAnnotatedElement(customAnnotationElementsMap, annotationElementsMap, method);
            }
            for (final Field field : clazz.getDeclaredFields()) {
                processAnnotatedElement(customAnnotationElementsMap, annotationElementsMap, field);
            }
            clazz = clazz.getSuperclass();
        }

        for (Entry<Annotation, AnnotatedElement> entry : annotationElementsMap.entrySet()) {
            Annotation annotation = entry.getKey();
            AnnotatedElement annotatedElement = entry.getValue();
            if (IncomingTime.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, annotatedElement, loggingInfo.getIncomingMessageTime());
            }
            if (OutcomingTime.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, annotatedElement, loggingInfo.getOutcomingMessageTime());
            }
            if (InputName.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, annotatedElement, loggingInfo.getInputName());
            }
            if (ServiceName.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo, target, annotation, annotatedElement, loggingInfo.getServiceName());
            }
            if (Publisher.class.equals(annotation.annotationType())) {
                insertValue(loggingInfo,
                    target,
                    annotation,
                    annotatedElement,
                    loggingInfo.getPublisherType().toString());
            }
            if (loggingInfo.getRequestMessage() != null) {
                if (Url.class
                    .equals(annotation.annotationType()) && loggingInfo.getRequestMessage().getAddress() != null) {
                    insertValue(loggingInfo,
                        target,
                        annotation,
                        annotatedElement,
                        loggingInfo.getRequestMessage().getAddress().toString());
                }
                if (Request.class
                    .equals(annotation.annotationType()) && loggingInfo.getResponseMessage().getPayload() != null) {
                    insertValue(loggingInfo,
                        target,
                        annotation,
                        annotatedElement,
                        loggingInfo.getRequestMessage().getPayload().toString());
                }
            } else {
                log.error("Not found request message in logging info!");
            }
            if (loggingInfo.getResponseMessage() != null) {
                if (Response.class
                    .equals(annotation.annotationType()) && loggingInfo.getResponseMessage().getPayload() != null) {
                    insertValue(loggingInfo,
                        target,
                        annotation,
                        annotatedElement,
                        loggingInfo.getResponseMessage().getPayload().toString());
                }
            } else {
                log.error("Not found response message in logging info!");
            }
            if (WithLoggingInfoConvertor.class.equals(annotation.annotationType())) {
                withLoggingInfoInsertValue(loggingInfo, target, annotation, annotatedElement);
            }
        }

        for (Entry<Annotation, AnnotatedElement> entry : customAnnotationElementsMap.entrySet()) {
            Annotation annotation = entry.getKey();
            AnnotatedElement annotatedElement = entry.getValue();
            if (CustomValue.class.equals(annotation.annotationType())) {
                CustomValue setterValue = (CustomValue) annotation;
                String key = setterValue.value();
                insertValue(loggingInfo, target, annotation, annotatedElement, loggingCustomData.getValue(key));
            }
        }
    }

    private void processAnnotatedElement(Map<Annotation, AnnotatedElement> customAnnotationElementsMap,
            Map<Annotation, AnnotatedElement> annotationElementsMap,
            final AnnotatedElement annotatedElement) {
        for (Class<? extends Annotation> annotationClass : CUSTOM_ANNOTATIONS) {
            Annotation annotation = annotatedElement.getAnnotation(annotationClass);
            if (annotation != null) {
                customAnnotationElementsMap.put(annotation, annotatedElement);
            }
        }
        for (Class<? extends Annotation> annotationClass : MAPPING_ANNOTATIONS) {
            Annotation annotation = annotatedElement.getAnnotation(annotationClass);
            if (annotation != null) {
                annotationElementsMap.put(annotation, annotatedElement);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void insertValue(LoggingInfo loggingInfo,
            Object target,
            Annotation annotation,
            AnnotatedElement annotatedElement,
            Object value) {
        boolean f = false;
        try {
            Method publisherTypeMethod = annotation.annotationType().getMethod("publisherTypes");
            PublisherType[] publisherTypes = (PublisherType[]) publisherTypeMethod.invoke(annotation);
            for (PublisherType publisherType : publisherTypes) {
                if (publisherType.equals(loggingInfo.getPublisherType())) {
                    f = true;
                    break;
                }
            }
        } catch (NoSuchMethodException e) {
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException(
                String.format("Failed to retrive property 'publisherTypes' from annotation '%s' declared on method!",
                    annotation.getClass().getTypeName()));
        }

        if (!f) {
            return;
        }

        Class<? extends Convertor<?, ?>> typeConvertorClass = null;
        try {
            Method convertorMethod = annotation.annotationType().getMethod("convertor");
            typeConvertorClass = (Class<? extends Convertor<?, ?>>) convertorMethod.invoke(annotation);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException(
                String.format("Invalid annotation is used! Property 'convertor' is not found in '%s'!",
                    annotation.getClass().getTypeName()));
        }

        if (!(DefaultConvertor.class.equals(typeConvertorClass) || DefaultStringConvertor.class
            .equals(typeConvertorClass) || DefaultNumberConvertor.class
                .equals(typeConvertorClass) || DefaultDateConvertor.class.equals(typeConvertorClass))) {
            Convertor<Object, Object> convertor = null;
            try {
                convertor = (Convertor<Object, Object>) typeConvertorClass.newInstance();
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error(
                        String.format("Failed to instantiate a type convertor '%s'! Null value is used as a result!",
                            typeConvertorClass.getTypeName()),
                        e);
                }
                value = null;
            }
            if (convertor != null) {
                try {
                    value = convertor.convert(value);
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error(String.format(
                            "Failed on type convertation for annotated element '%s'! Null value is used as a result!",
                            getAnnotatedElementRef(annotatedElement)), e);
                    }
                    value = null;
                }
            }
        }
        try {
            setValueWithAnnotatedElement(target, annotatedElement, value);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(
                    String.format("Failed on set a value! Please, check that the element '%s' is annotated correctly!",
                        getAnnotatedElementRef(annotatedElement)),
                    e);
            }
        }

    }

    private String getAnnotatedElementRef(AnnotatedElement annotatedElement) {
        if (annotatedElement instanceof Method) {
            Method method = (Method) annotatedElement;
            return MethodUtil.printMethod(method.getName(), method.getParameterTypes());
        } else if (annotatedElement instanceof Field) {
            Field field = (Field) annotatedElement;
            return field.getDeclaringClass().getTypeName() + "." + field.getName();
        }
        throw new IllegalStateException("Wrong type of annotated element! Only method and fields are supported!");
    }

    private void setValueWithAnnotatedElement(Object target,
            AnnotatedElement annotatedElement,
            Object value) throws InvocationTargetException, IllegalAccessException {
        if (annotatedElement instanceof Method) {
            Method method = (Method) annotatedElement;
            if (method.getParameterCount() == 0 && method.getName().startsWith("get")) {
                try {
                    Method m = method.getDeclaringClass()
                        .getMethod("set" + method.getName().substring(3), method.getReturnType());
                    m.invoke(target, value);
                    return;
                } catch (NoSuchMethodException e) {
                }
            }
            method.invoke(target, value);
            return;
        } else if (annotatedElement instanceof Field) {
            Field field = (Field) annotatedElement;
            field.setAccessible(true);
            field.set(target, value);
            return;
        }
        throw new IllegalStateException("Wrong type of annotated element! Only method and fields are supported!");
    }

    @SuppressWarnings("unchecked")
    private void withLoggingInfoInsertValue(LoggingInfo loggingInfo,
            Object target,
            Annotation annotation,
            AnnotatedElement annotatedElement) {
        WithLoggingInfoConvertor withLoggingInfoConvertor = (WithLoggingInfoConvertor) annotation;
        boolean f = false;
        for (PublisherType publisherType : withLoggingInfoConvertor.publisherTypes()) {
            if (publisherType.equals(loggingInfo.getPublisherType())) {
                f = true;
                break;
            }
        }
        if (f) {
            Class<? extends LoggingInfoConvertor<?>> convertorClass = withLoggingInfoConvertor.convertor();
            LoggingInfoConvertor<Object> convertor = null;
            try {
                convertor = (LoggingInfoConvertor<Object>) convertorClass.newInstance();
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error(
                        String.format("LoggingInfo convertor '%s' instantiation is failed!",
                            convertorClass.getTypeName()),
                        e);
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
                            "Failed on type convertation for annotated element '%s'! Null value is used as a result!",
                            getAnnotatedElementRef(annotatedElement)), e);
                    }
                    convertedValue = null;
                }
                try {
                    setValueWithAnnotatedElement(target, annotatedElement, convertedValue);
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error(String.format(
                            "Failed on set a value! Please, check that the element '%s' is annotated correctly!",
                            getAnnotatedElementRef(annotatedElement)), e);
                    }
                }
            }
        }
    }
}
