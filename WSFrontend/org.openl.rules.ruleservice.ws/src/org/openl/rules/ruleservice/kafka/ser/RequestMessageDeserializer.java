package org.openl.rules.ruleservice.kafka.ser;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceOpenLServiceInstantiationHelper;
import org.openl.rules.ruleservice.kafka.KafkaHeaders;
import org.openl.rules.ruleservice.kafka.RequestMessage;
import org.openl.rules.ruleservice.kafka.publish.KafkaHelpers;
import org.openl.rules.ruleservice.publish.common.MethodUtils;
import org.openl.types.IOpenMember;

public class RequestMessageDeserializer implements Deserializer<RequestMessage> {

    private final ObjectMapper objectMapper;
    private final OpenLService service;
    private final Map<String, Map<String, Entry>> methodMap;
    private final Entry methodParametersWrapperClassInfo;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private Charset encoding = StandardCharsets.UTF_8;

    public RequestMessageDeserializer(OpenLService service, ObjectMapper objectMapper, Method method) throws Exception {
        this.service = Objects.requireNonNull(service, "service cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
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
            encoding = Charset.forName((String) encodingValue);
        }
    }

    private Entry generateWrapperClass(Method m) throws Exception {
        IOpenMember openMember = RuleServiceOpenLServiceInstantiationHelper.getOpenMember(m, service.getServiceBean());
        String[] parameterNames = MethodUtils
                .getParameterNames(openMember, m, service.isProvideRuntimeContext(), service.isProvideVariations());
        return new Entry(m, parameterNames);
    }

    @Override
    public RequestMessage deserialize(String topic, byte[] data) {
        return null;
    }

    private String getStringFromHeaders(Headers headers, String key) throws UnsupportedEncodingException {
        Header header = headers.lastHeader(key);
        if (header != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return null;
    }

    @Override
    public RequestMessage deserialize(String topic, Headers headers, byte[] rawData) {
        if (methodParametersWrapperClassInfo != null) { // This is method type message
            try {
                return buildRequestMessage(methodParametersWrapperClassInfo, rawData);
            } catch (Exception e) {
                return new RequestMessage(methodParametersWrapperClassInfo.method, e, rawData, encoding);
            }
        } else {
            Method m = null;
            try {
                final String methodName = getStringFromHeaders(headers, KafkaHeaders.METHOD_NAME);
                final String methodParameters = getStringFromHeaders(headers, KafkaHeaders.METHOD_PARAMETERS);
                Entry entry = getCachedMethodParametersWrapperClassInfo(methodName, methodParameters);
                if (entry == null) {
                    m = KafkaHelpers.findMethodInService(service, methodName, methodParameters);
                    entry = generateWrapperClass(m);
                    putCachedMethodParametersWrapperClassInfo(methodName, methodParameters, entry);
                } else {
                    m = entry.method;
                }
                return buildRequestMessage(entry, rawData);
            } catch (Exception e) {
                return new RequestMessage(m, e, rawData, encoding);
            }
        }
    }

    protected RequestMessage buildRequestMessage(Entry entry, byte[] rawData) throws IOException {
        final Method method = entry.method;
        final int numOfParameters = method.getParameterCount();
        if (numOfParameters == 0) {
            return new RequestMessage(method, new Object[]{}, rawData, encoding);
        } else if (numOfParameters == 1) {
            Object arg = objectMapper.readValue(new String(rawData, encoding), method.getParameterTypes()[0]);
            return new RequestMessage(method, new Object[]{arg}, rawData, encoding);
        } else {
            Object[] parameters = new Object[numOfParameters];

            var tree = objectMapper.readTree(rawData);
            if (!tree.isObject()) {
                throw new IllegalArgumentException("Expecting a JSON object");
            }
            for (int i = 0; i < method.getParameterCount(); i++) {
                var name = entry.paramNames[i];
                var type = method.getParameterTypes()[i];
                var node = tree.get(name);
                parameters[i] = objectMapper.treeToValue(node, type);
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
        private final Method method;
        private final String[] paramNames;

        public Entry(Method method, String[] paramNames) {
            this.method = Objects.requireNonNull(method);
            this.paramNames = Objects.requireNonNull(paramNames);
        }
    }
}
