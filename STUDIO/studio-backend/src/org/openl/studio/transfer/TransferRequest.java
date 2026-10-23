package org.openl.studio.transfer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.openl.studio.common.web.EntityVersions;

/**
 * A request to a transfer link as the REST endpoint that runs its operation reads it: the method, the parameters
 * and, for an upload, the body the link recorded or received and the version of the file it replaces, whatever the
 * client sent besides.
 *
 * @author Yury Molchan
 */
final class TransferRequest extends HttpServletRequestWrapper {

    @Getter
    private final String method;
    private final Map<String, String[]> parameters;
    private final @Nullable BodyStream body;
    /** The {@code If-Match} header naming the version of the file a replacement is based on, or null for none. */
    private final @Nullable String ifMatch;

    private TransferRequest(HttpServletRequest request, String method, Map<String, String> parameters,
                            @Nullable InputStream body, @Nullable String expectedVersion) {
        super(request);
        this.method = method;
        var values = new LinkedHashMap<String, String[]>();
        parameters.forEach((name, value) -> values.put(name, new String[]{value}));
        this.parameters = Collections.unmodifiableMap(values);
        this.body = body == null ? null : new BodyStream(body);
        this.ifMatch = expectedVersion == null ? null : EntityVersions.entityTag(expectedVersion);
    }

    /** A {@code GET} with the given parameters and no body. */
    static TransferRequest get(HttpServletRequest request, Map<String, String> parameters) {
        return new TransferRequest(request, "GET", parameters, null, null);
    }

    /**
     * A {@code POST} with the given parameters and the raw bytes of the file as its body. The caller closes the
     * body once the request is answered.
     */
    static TransferRequest post(HttpServletRequest request, Map<String, String> parameters, InputStream body) {
        return new TransferRequest(request, "POST", parameters, body, null);
    }

    /**
     * A {@code PUT} that replaces a file of the given version with the raw bytes of its body, so that a file changed
     * since that version is not written. The caller closes the body once the request is answered.
     */
    static TransferRequest put(HttpServletRequest request, Map<String, String> parameters, InputStream body,
                               String expectedVersion) {
        return new TransferRequest(request, "PUT", parameters, body, expectedVersion);
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
    public @Nullable String getHeader(String name) {
        return isIfMatch(name) ? ifMatch : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (!isIfMatch(name)) {
            return super.getHeaders(name);
        }
        return Collections.enumeration(ifMatch == null ? Collections.emptyList() : Collections.singletonList(ifMatch));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        var others = Collections.list(super.getHeaderNames()).stream().filter(name -> !isIfMatch(name));
        var names = ifMatch == null ? others : Stream.concat(others, Stream.of(HttpHeaders.IF_MATCH));
        return Collections.enumeration(names.toList());
    }

    /** Whether the header is the one naming a version, which only the link tells: what the client sent is not used. */
    private static boolean isIfMatch(String name) {
        return HttpHeaders.IF_MATCH.equalsIgnoreCase(name);
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
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    private static final class BodyStream extends ServletInputStream {

        private final InputStream in;
        @Getter
        private boolean finished;

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
