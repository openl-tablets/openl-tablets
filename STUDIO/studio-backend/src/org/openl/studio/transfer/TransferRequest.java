package org.openl.studio.transfer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;

/**
 * A request to a transfer link as the REST endpoint that runs its operation reads it: the method, the parameters
 * and, for an upload, the body the link recorded or received, whatever the client sent besides.
 *
 * @author Yury Molchan
 */
final class TransferRequest extends HttpServletRequestWrapper {

    private final String method;
    private final Map<String, String[]> parameters;
    private final @Nullable BodyStream body;

    private TransferRequest(HttpServletRequest request, String method, Map<String, String> parameters,
                            @Nullable InputStream body) {
        super(request);
        this.method = method;
        var values = new LinkedHashMap<String, String[]>();
        parameters.forEach((name, value) -> values.put(name, new String[]{value}));
        this.parameters = Collections.unmodifiableMap(values);
        this.body = body == null ? null : new BodyStream(body);
    }

    /** A {@code GET} with the given parameters and no body. */
    static TransferRequest get(HttpServletRequest request, Map<String, String> parameters) {
        return new TransferRequest(request, "GET", parameters, null);
    }

    /**
     * A {@code POST} with the given parameters and the raw bytes of the file as its body. The caller closes the
     * body once the request is answered.
     */
    static TransferRequest post(HttpServletRequest request, Map<String, String> parameters, InputStream body) {
        return new TransferRequest(request, "POST", parameters, body);
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public @Nullable String getQueryString() {
        return null;
    }

    @Override
    public @Nullable String getParameter(String name) {
        var values = parameters.get(name);
        return values == null ? null : values[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameters;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

    @Override
    public String @Nullable [] getParameterValues(String name) {
        return parameters.get(name);
    }

    @Override
    public @Nullable String getContentType() {
        return body == null ? null : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    /** The body of the request: one stream, as a request has, and none for a {@code GET}. */
    @Override
    public ServletInputStream getInputStream() {
        return body == null ? new BodyStream(InputStream.nullInputStream()) : body;
    }

    /** The body of the request, read from the file it was received into. */
    private static final class BodyStream extends ServletInputStream {

        private final InputStream in;
        private boolean finished;

        BodyStream(InputStream in) {
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            var read = in.read();
            finished = read < 0;
            return read;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            var read = in.read(buffer, offset, length);
            finished = read < 0;
            return read;
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            throw new IllegalStateException("The body of a transfer is read blocking.");
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }
}
