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
import org.openl.rules.ruleservice.kafka.publish.Message;
import org.openl.rules.ruleservice.publish.common.MethodUtils;
import org.openl.util.ClassUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MessageDeserializer implements Deserializer<Message> {

    private static final String UTF8 = "UTF8";
    private final ObjectMapper objectMapper;
    private final OpenLService service;
    private final Map<String, Map<String, Entry>> methodMap;
    private final Entry methodParametersWrapperClassInfo;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private String encoding = UTF8;

    public MessageDeserializer(OpenLService service, ObjectMapper objectMapper, Method method) throws Exception {
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

    public MessageDeserializer(OpenLService service, ObjectMapper objectMapper) throws Exception {
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
        String beanName = "org.openl.rules.ruleservice.publish.kafka.ser.MessageDeserializer$" + m
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
    public Message deserialize(String topic, byte[] data) {
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
    public Message deserialize(String topic, Headers headers, byte[] data) {
        if (methodParametersWrapperClassInfo != null) { // This is method type message
            try {
                return buildMessage(methodParametersWrapperClassInfo, data);
            } catch (Exception e) {
                return new Message(methodParametersWrapperClassInfo.getMethod(),
                    new MessageFormatException("Message format is wrong.", e),
                    data);
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
                return buildMessage(entry, data);
            } catch (Exception e) {
                return new Message(m, new MessageFormatException("Message format is wrong.", e), data);
            }
        }
    }

    protected Message buildMessage(Entry entry, byte[] data) throws IOException, IllegalAccessException {
        final Method method = entry.getMethod();
        final int numOfParameters = method.getParameterCount();
        if (numOfParameters == 0) {
            return new Message(method, new Object[] {}, data);
        } else if (numOfParameters == 1) {
            Object arg = objectMapper.readValue(new String(data, encoding), method.getParameterTypes()[0]);
            return new Message(method, new Object[] { arg }, data);
        } else {
            Object wrapperTarget = objectMapper.readValue(new String(data, encoding), entry.getWrapperClass());
            Object[] parameters = new Object[numOfParameters];
            Field[] wrapperClassFields = entry.getWrapperClassFields();
            for (int i = 0; i < method.getParameterCount(); i++) {
                parameters[i] = wrapperClassFields[i].get(wrapperTarget);
            }
            return new Message(method, parameters, data);
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
