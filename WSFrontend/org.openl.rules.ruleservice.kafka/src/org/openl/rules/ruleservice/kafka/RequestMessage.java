package org.openl.rules.ruleservice.kafka;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class RequestMessage {
    private Object[] parameters;
    private Exception exception;
    private Method method;
    private final byte[] rawData;
    private Charset encoding = StandardCharsets.UTF_8;

    public RequestMessage(Method method, Object[] parameters, byte[] rawData, Charset encoding) {
        this(rawData, encoding);
        this.method = Objects.requireNonNull(method, "method cannot be null");
        this.parameters = Objects.requireNonNull(parameters, "methodArgs cannot be null");
    }

    public RequestMessage(Method method, Exception exception, byte[] rawData, Charset encoding) {
        this(rawData, encoding);
        this.exception = Objects.requireNonNull(exception, "exception cannot be null");
        this.method = method;
    }

    private RequestMessage(byte[] rawData, Charset encoding) {
        this.rawData = rawData;
        if (encoding != null) {
            this.encoding = encoding;
        }
    }

    public final Object[] getParameters() throws Exception {
        if (isSuccess()) {
            return parameters.clone();
        } else {
            throw exception;
        }
    }

    public final Method getMethod() throws Exception {
        if (method != null) {
            return method;
        } else {
            throw exception;
        }
    }

    public final boolean isSuccess() {
        return exception == null;
    }

    public final Exception getException() {
        return exception;
    }

    public final byte[] getRawData() {
        return rawData;
    }

    public final String asText() {
        return new String(rawData, encoding);
    }
}
