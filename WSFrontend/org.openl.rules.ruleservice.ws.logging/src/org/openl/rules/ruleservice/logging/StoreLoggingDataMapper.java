package org.openl.rules.ruleservice.logging;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.header.Header;
import org.openl.binding.MethodUtil;
import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;
import org.openl.rules.ruleservice.logging.annotation.DefaultConvertor;
import org.openl.rules.ruleservice.logging.annotation.DefaultDateConvertor;
import org.openl.rules.ruleservice.logging.annotation.DefaultNumberConvertor;
import org.openl.rules.ruleservice.logging.annotation.DefaultStringConvertor;
import org.openl.rules.ruleservice.logging.annotation.IncomingTime;
import org.openl.rules.ruleservice.logging.annotation.InputName;
import org.openl.rules.ruleservice.logging.annotation.KafkaConsumerRecordHeader;
import org.openl.rules.ruleservice.logging.annotation.KafkaProducerRecordHeader;
import org.openl.rules.ruleservice.logging.annotation.OutcomingTime;
import org.openl.rules.ruleservice.logging.annotation.Publisher;
import org.openl.rules.ruleservice.logging.annotation.QualifyPublisherType;
import org.openl.rules.ruleservice.logging.annotation.Request;
import org.openl.rules.ruleservice.logging.annotation.Response;
import org.openl.rules.ruleservice.logging.annotation.ServiceName;
import org.openl.rules.ruleservice.logging.annotation.Url;
import org.openl.rules.ruleservice.logging.annotation.Value;
import org.openl.rules.ruleservice.logging.annotation.WithStoreLoggingDataConvertor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoreLoggingDataMapper {

    private final Logger log = LoggerFactory.getLogger(StoreLoggingDataMapper.class);

    private static final Set<Class<? extends Annotation>> CUSTOM_ANNOTATIONS;
    private static final Set<Class<? extends Annotation>> MAPPING_ANNOTATIONS;

    static {
        Set<Class<? extends Annotation>> customAnnotations = new HashSet<>();
        customAnnotations.add(Value.class);
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
        mappingAnnotations.add(WithStoreLoggingDataConvertor.class);

        MAPPING_ANNOTATIONS = Collections.unmodifiableSet(mappingAnnotations);
    }

    public void map(StoreLoggingData storeLoggingData, Object target) {
        if (target == null) {
            return;
        }

        CustomData customData = storeLoggingData.getCustomData();
        Class<?> targetClass = target.getClass();

        QualifyPublisherType qualifyPublisherTypeOnClass = targetClass.getAnnotation(QualifyPublisherType.class);
        if (qualifyPublisherTypeOnClass != null) {
            matchPublisherType(qualifyPublisherTypeOnClass.value(), storeLoggingData.getPublisherType());
        }

        Class<?> clazz = targetClass;
        List<Pair<Annotation, AnnotatedElement>> customAnnotationElements = new ArrayList<>();
        List<Pair<Annotation, AnnotatedElement>> annotationElements = new ArrayList<>();
        while (clazz != Object.class) {
            for (final Method method : clazz.getDeclaredMethods()) {
                processAnnotatedElement(customAnnotationElements, annotationElements, method);
            }
            for (final Field field : clazz.getDeclaredFields()) {
                processAnnotatedElement(customAnnotationElements, annotationElements, field);
            }
            clazz = clazz.getSuperclass();
        }

        for (Entry<Annotation, AnnotatedElement> entry : annotationElements) {
            Annotation annotation = entry.getKey();
            AnnotatedElement annotatedElement = entry.getValue();
            if (annotation instanceof IncomingTime) {
                injectValue(storeLoggingData,
                    target,
                    annotation,
                    annotatedElement,
                    storeLoggingData.getIncomingMessageTime());
            } else if (annotation instanceof OutcomingTime) {
                injectValue(storeLoggingData,
                    target,
                    annotation,
                    annotatedElement,
                    storeLoggingData.getOutcomingMessageTime());
            } else if (annotation instanceof InputName) {
                injectValue(storeLoggingData, target, annotation, annotatedElement, storeLoggingData.getInputName());
            } else if (annotation instanceof ServiceName) {
                injectValue(storeLoggingData, target, annotation, annotatedElement, storeLoggingData.getServiceName());
            } else if (annotation instanceof Publisher) {
                injectValue(storeLoggingData,
                    target,
                    annotation,
                    annotatedElement,
                    storeLoggingData.getPublisherType().toString());
            } else if (annotation instanceof Url) {
                if (storeLoggingData.getRequestMessage() != null && storeLoggingData.getRequestMessage()
                    .getAddress() != null) {
                    injectValue(storeLoggingData,
                        target,
                        annotation,
                        annotatedElement,
                        storeLoggingData.getRequestMessage().getAddress().toString());
                }
            } else if (annotation instanceof Request) {
                String request = null;
                switch (storeLoggingData.getPublisherType()) {
                    case KAFKA:
                        request = storeLoggingData.getConsumerRecord().value().asText();
                        break;
                    case RMI:
                        break;
                    case RESTFUL:
                    case WEBSERVICE:
                        if (storeLoggingData.getRequestMessage() != null && storeLoggingData.getRequestMessage()
                            .getPayload() != null) {
                            request = storeLoggingData.getRequestMessage().getPayload().toString();
                        }
                }
                injectValue(storeLoggingData, target, annotation, annotatedElement, request);
            } else if (annotation instanceof Response) {
                String response = null;
                switch (storeLoggingData.getPublisherType()) {
                    case KAFKA:
                        if (storeLoggingData.getDltRecord() != null) {
                            response = StringUtils.toEncodedString(storeLoggingData.getDltRecord().value(),
                                StandardCharsets.UTF_8);
                        } else if (storeLoggingData.getProducerRecord() != null) {
                            try {
                                response = storeLoggingData.getObjectSerializer()
                                    .writeValueAsString(storeLoggingData.getProducerRecord().value());
                            } catch (ProcessingException e) {
                                throw new RuleServiceRuntimeException(e);
                            }
                        }
                        break;
                    case RMI:
                        break;
                    case RESTFUL:
                    case WEBSERVICE:
                        if (storeLoggingData.getResponseMessage() != null && storeLoggingData.getResponseMessage()
                            .getPayload() != null) {
                            response = storeLoggingData.getResponseMessage().getPayload().toString();
                        }
                }
                injectValue(storeLoggingData, target, annotation, annotatedElement, response);
            } else if (annotation instanceof WithStoreLoggingDataConvertor) {
                withStoreLoggingDataInsertValue(storeLoggingData, target, annotation, annotatedElement);
            } else if (annotation instanceof KafkaConsumerRecordHeader) {
                KafkaConsumerRecordHeader consumerRecordHeader = (KafkaConsumerRecordHeader) annotation;
                if (storeLoggingData.getConsumerRecord() != null) {
                    Header header = storeLoggingData.getConsumerRecord()
                        .headers()
                        .lastHeader(consumerRecordHeader.value());
                    if (header != null) {
                        injectValue(storeLoggingData, target, annotation, annotatedElement, header.value());
                    }
                }
            } else if (annotation instanceof KafkaProducerRecordHeader) {
                KafkaProducerRecordHeader producerRecordHeader = (KafkaProducerRecordHeader) annotation;
                if (storeLoggingData.getProducerRecord() != null) {
                    Header header = storeLoggingData.getProducerRecord()
                        .headers()
                        .lastHeader(producerRecordHeader.value());
                    if (header != null) {
                        injectValue(storeLoggingData, target, annotation, annotatedElement, header.value());
                    }
                } else if (storeLoggingData.getDltRecord() != null) {
                    Header header = storeLoggingData.getDltRecord().headers().lastHeader(producerRecordHeader.value());
                    if (header != null) {
                        injectValue(storeLoggingData, target, annotation, annotatedElement, header.value());
                    }
                }
            }
        }

        for (

        Entry<Annotation, AnnotatedElement> entry : customAnnotationElements) {
            Annotation annotation = entry.getKey();
            AnnotatedElement annotatedElement = entry.getValue();
            if (annotation instanceof Value) {
                Value valueAnnotation = (Value) annotation;
                String key = valueAnnotation.value();
                injectValue(storeLoggingData, target, annotation, annotatedElement, customData.getValue(key));
            }
        }
    }

    private void processAnnotatedElement(List<Pair<Annotation, AnnotatedElement>> customAnnotationElements,
            List<Pair<Annotation, AnnotatedElement>> annotationElements,
            final AnnotatedElement annotatedElement) {
        for (Class<? extends Annotation> annotationClass : CUSTOM_ANNOTATIONS) {
            Annotation annotation = annotatedElement.getAnnotation(annotationClass);
            if (annotation != null) {
                customAnnotationElements.add(Pair.of(annotation, annotatedElement));
            }
        }
        for (Class<? extends Annotation> annotationClass : MAPPING_ANNOTATIONS) {
            Annotation annotation = annotatedElement.getAnnotation(annotationClass);
            if (annotation != null) {
                annotationElements.add(Pair.of(annotation, annotatedElement));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void injectValue(StoreLoggingData storeLoggingData,
            Object target,
            Annotation annotation,
            AnnotatedElement annotatedElement,
            Object value) {
        QualifyPublisherType qualifyPublisherType = annotatedElement.getAnnotation(QualifyPublisherType.class);
        if (qualifyPublisherType != null && !matchPublisherType(qualifyPublisherType.value(),
            storeLoggingData.getPublisherType())) {
            return;
        }

        Class<? extends Convertor<?, ?>> convertorClass = null;
        try {
            Method convertorMethod = annotation.annotationType().getMethod("convertor");
            convertorClass = (Class<? extends Convertor<?, ?>>) convertorMethod.invoke(annotation);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException(
                String.format("Invalid annotation is used! Property 'convertor' is not found in '%s'!",
                    annotation.getClass().getTypeName()));
        }

        if (!(DefaultConvertor.class.equals(convertorClass) || DefaultStringConvertor.class
            .equals(convertorClass) || DefaultNumberConvertor.class
                .equals(convertorClass) || DefaultDateConvertor.class.equals(convertorClass))) {
            Convertor<Object, Object> convertor = null;
            try {
                convertor = (Convertor<Object, Object>) convertorClass.newInstance();
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error(String.format(
                        "Convertor class instantiation is failed. Please, check that '%s' class isn't abstact and has a default constructor.",
                        convertorClass.getTypeName()), e);
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
            return MethodUtil.printQualifiedMethodName(method);
        } else if (annotatedElement instanceof Field) {
            Field field = (Field) annotatedElement;
            return field.getDeclaringClass().getTypeName() + "." + field.getName();
        }
        throw new IllegalStateException("Wrong type of annotated element! Only methods and fields are supported!");
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
        throw new IllegalStateException("Wrong type of annotated element! Only methods and fields are supported!");
    }

    @SuppressWarnings("unchecked")
    private void withStoreLoggingDataInsertValue(StoreLoggingData storeLoggingData,
            Object target,
            Annotation annotation,
            AnnotatedElement annotatedElement) {
        WithStoreLoggingDataConvertor withStoreLoggingDataConvertor = (WithStoreLoggingDataConvertor) annotation;
        QualifyPublisherType qualifyPublisherType = annotatedElement.getAnnotation(QualifyPublisherType.class);
        if (qualifyPublisherType == null || matchPublisherType(qualifyPublisherType.value(),
            storeLoggingData.getPublisherType())) {
            Class<? extends StoreLoggingDataConvertor<?>> convertorClass = withStoreLoggingDataConvertor.convertor();
            StoreLoggingDataConvertor<Object> convertor = null;
            try {
                convertor = (StoreLoggingDataConvertor<Object>) convertorClass.newInstance();
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error(String.format(
                        "StoreLoggingConvertor instantiation is failed. Please, check that '%s' class isn't abstact and has a default constructor.",
                        convertorClass.getTypeName()), e);
                }
                return;
            }
            if (convertor != null) {
                Object convertedValue = null;
                try {
                    convertedValue = convertor.convert(storeLoggingData);
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

    private boolean matchPublisherType(org.openl.rules.ruleservice.logging.annotation.PublisherType[] value,
            PublisherType publisherType) {
        switch (publisherType) {
            case KAFKA:
                return Arrays.stream(value)
                    .anyMatch(org.openl.rules.ruleservice.logging.annotation.PublisherType.KAFKA::equals);
            case WEBSERVICE:
                return Arrays.stream(value)
                    .anyMatch(org.openl.rules.ruleservice.logging.annotation.PublisherType.WEBSERVICE::equals);
            case RESTFUL:
                return Arrays.stream(value)
                    .anyMatch(org.openl.rules.ruleservice.logging.annotation.PublisherType.RESTFUL::equals);
            default:
                return false;
        }
    }
}
