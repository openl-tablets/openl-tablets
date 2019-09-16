package org.openl.rules.ruleservice.kafka.ser;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.kafka.publish.KafkaHeaders;
import org.openl.rules.ruleservice.kafka.publish.KafkaHelpers;
import org.openl.rules.ruleservice.kafka.publish.RequestMessage;
import org.openl.rules.ruleservice.publish.common.MethodUtils;
import org.openl.util.ClassUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RequestMessageDeserializer implements Deserializer<RequestMessage> {

    private static final String UTF8 = "UTF8";
    private final ObjectMapper objectMapper;
    private final OpenLService service;
    private final Map<String, Map<String, Entry>> methodMap;
    private final Entry methodParametersWrapperClassInfo;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private String encoding = UTF8;

    public RequestMessageDeserializer(OpenLService service, ObjectMapper objectMapper, Method method) throws Exception {
        Objects.requireNonNull(service, "service can't be null.");
        Objects.requireNonNull(objectMapper, "objectMapper can't be null.");
        this.service = service;
        this.objectMapper = objectMapper;
        if (method != null) {
            methodParametersWrapperClassInfo = generateWrapperClass(method);
            methodMap = null;
        } else {
            methodMap = new HashMap<>();
            methodParametersWrapperClassInfo = null;
        }
    }

    public RequestMessageDeserializer(OpenLService service, ObjectMapper objectMapper) throws Exception {
        this(service, objectMapper, null);
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Object encodingValue = configs.get("value.deserializer.encoding");
        if (encodingValue == null) {
            encodingValue = configs.get("deserializer.encoding");
        }
        if (encodingValue instanceof String) {
            encoding = (String) encodingValue;
        }
    }

    private Entry generateWrapperClass(Method m) throws Exception {
        String[] parameterNames = MethodUtils.getParameterNames(m, service);
        String beanName = "org.openl.rules.ruleservice.publish.kafka.ser.KafkaRequestDeserializer$" + m
            .getName() + "$" + RandomStringUtils.random(16, true, false);

        int i = 0;
        JavaBeanClassBuilder beanClassBuilder = new JavaBeanClassBuilder(beanName);
        for (Class<?> type : m.getParameterTypes()) {
            beanClassBuilder.addField(parameterNames[i], type.getName());
            i++;
        }

        byte[] byteCode = beanClassBuilder.byteCode();
        Class<?> wrapperClazz = ClassUtils.defineClass(beanName, byteCode, service.getClassLoader());
        Field[] wrapperClazzFields = new Field[m.getParameterCount()];
        for (int j = 0; j < m.getParameterCount(); j++) {
            wrapperClazzFields[j] = wrapperClazz.getDeclaredField(parameterNames[j]);
            wrapperClazzFields[j].setAccessible(true);
        }

        return new Entry(m, wrapperClazz, wrapperClazzFields);
    }

    @Override
    public RequestMessage deserialize(String topic, byte[] data) {
        return null;
    }

    private String getStringFromHeaders(Headers headers, String key) throws UnsupportedEncodingException {
        Header header = headers.lastHeader(key);
        if (header != null) {
            return new String(header.value(), UTF8);
        }
        return null;
    }

    @Override
    public RequestMessage deserialize(String topic, Headers headers, byte[] rawData) {
        if (methodParametersWrapperClassInfo != null) { // This is method type message
            try {
                return buildRequestMessage(methodParametersWrapperClassInfo, rawData);
            } catch (Exception e) {
                return new RequestMessage(methodParametersWrapperClassInfo.getMethod(),
                    new RequestMessageFormatException("Invalid message format.", e),
                    rawData,
                    encoding);
            }
        } else {
            Method m = null;
            try {
                final String methodName = getStringFromHeaders(headers, KafkaHeaders.METHOD_NAME);
                final String methodParameters = getStringFromHeaders(headers, KafkaHeaders.METHOD_PARAMETERS);
                Entry entry = getCachedMethodParametersWrapperClassInfo(methodName, methodParameters);
                if (entry == null) {
                    Method m1 = KafkaHelpers.findMethodInService(service, methodName, methodParameters);
                    entry = generateWrapperClass(m1);
                    putCachedMethodParametersWrapperClassInfo(methodName, methodParameters, entry);
                }
                return buildRequestMessage(entry, rawData);
            } catch (Exception e) {
                return new RequestMessage(m,
                    new RequestMessageFormatException("Invalid message format.", e),
                    rawData,
                    encoding);
            }
        }
    }

    protected RequestMessage buildRequestMessage(Entry entry, byte[] rawData) throws IOException,
                                                                              IllegalAccessException {
        final Method method = entry.getMethod();
        final int numOfParameters = method.getParameterCount();
        if (numOfParameters == 0) {
            return new RequestMessage(method, new Object[] {}, rawData, encoding);
        } else if (numOfParameters == 1) {
            Object arg = objectMapper.readValue(new String(rawData, encoding), method.getParameterTypes()[0]);
            return new RequestMessage(method, new Object[] { arg }, rawData, encoding);
        } else {
            Object wrapperTarget = objectMapper.readValue(new String(rawData, encoding), entry.getWrapperClass());
            Object[] parameters = new Object[numOfParameters];
            Field[] wrapperClassFields = entry.getWrapperClassFields();
            for (int i = 0; i < method.getParameterCount(); i++) {
                parameters[i] = wrapperClassFields[i].get(wrapperTarget);
            }
            return new RequestMessage(method, parameters, rawData, encoding);
        }
    }

    private void putCachedMethodParametersWrapperClassInfo(String methodName, String methodParameters, Entry entry) {
        Lock writeLock = readWriteLock.writeLock();
        writeLock.lock();
        try {
            Map<String, Entry> t = methodMap.computeIfAbsent(methodName, e -> new HashMap<>());
            t.put(methodParameters, entry);
        } finally {
            writeLock.unlock();
        }

    }

    private Entry getCachedMethodParametersWrapperClassInfo(String methodName, String methodParameters) {
        Lock readLock = readWriteLock.readLock();
        readLock.lock();
        try {
            Map<String, Entry> t = methodMap.get(methodName);
            if (t != null) {
                return t.get(methodParameters);
            }
        } finally {
            readLock.unlock();
        }
        return null;
    }

    private static final class Entry {
        private Method method;
        private Class<?> wrapperClass;
        private Field[] wrapperClassFields;

        public Entry(Method method, Class<?> wrapperClass, Field[] wrapperClassFields) {
            Objects.requireNonNull(method);
            Objects.requireNonNull(wrapperClass);
            Objects.requireNonNull(wrapperClassFields);
            this.method = method;
            this.wrapperClass = wrapperClass;
            this.wrapperClassFields = wrapperClassFields;
        }

        public Method getMethod() {
            return method;
        }

        public Class<?> getWrapperClass() {
            return wrapperClass;
        }

        public Field[] getWrapperClassFields() {
            return wrapperClassFields;
        }
    }

    @Override
    public void close() {
    }
}
