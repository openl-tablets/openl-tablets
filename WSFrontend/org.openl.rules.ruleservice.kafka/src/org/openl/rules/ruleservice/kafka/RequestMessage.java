package org.openl.rules.ruleservice.kafka;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Objects;

public class RequestMessage {
    private Object[] parameters;
    private Exception exception;
    private Method method;
    private byte[] rawData;
    private String encoding = "UTF8";

    public RequestMessage(Method method, Object[] parameters, byte[] rawData, String encoding) {
        this(rawData, encoding);
        this.method = Objects.requireNonNull(method, "method cannot be null");
        this.parameters = Objects.requireNonNull(parameters, "methodArgs cannot be null");
    }

    public RequestMessage(Method method, Exception exception, byte[] rawData, String encoding) {
        this(rawData, encoding);
        this.exception = Objects.requireNonNull(exception, "exception cannot be null");
        this.method = method;
    }

    private RequestMessage(byte[] rawData, String encoding) {
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
        try {
            return new String(rawData, encoding);
        } catch (UnsupportedEncodingException e) {
            return new String(rawData);
        }
    }
}
