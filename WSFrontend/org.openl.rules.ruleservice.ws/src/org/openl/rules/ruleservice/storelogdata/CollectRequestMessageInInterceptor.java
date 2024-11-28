package org.openl.rules.ruleservice.storelogdata;

import java.io.InputStream;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Objects;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.LoggingMessage;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedWriter;
import org.apache.cxf.io.DelegatingInputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * CXF interceptor for collecting request data for logging to external source feature.
 *
 * @author Marat Kamalov
 */
@NoJSR250Annotations
public class CollectRequestMessageInInterceptor extends AbstractPhaseInterceptor<Message> {

    public static final String ID_KEY = CollectRequestMessageInInterceptor.class.getName() + ".ID";

    public CollectRequestMessageInInterceptor() {
        super(Phase.RECEIVE);
    }

    static boolean isBinaryContent(String contentType) {
        return contentType != null && (contentType.startsWith("image/") || contentType.equals("application/octet-stream"));
    }

    @Override
    public void handleMessage(Message message) {
        processMessage(message);
    }

    @Override
    public void handleFault(Message message) {
        final StoreLogData storeLogData = StoreLogDataHolder.get();
        storeLogData.fault();
        processMessage(message);
    }

    protected void processMessage(Message message) {
        if (message.containsKey(ID_KEY)) {
            return;
        }
        String id = (String) message.getExchange().get(ID_KEY);
        if (id == null) {
            id = LoggingMessage.nextId();
            message.getExchange().put(ID_KEY, id);
        }
        message.put(ID_KEY, id);
        final LoggingMessage buffer = new LoggingMessage("Request", id);

        append(message.get(Message.RESPONSE_CODE), buffer.getResponseCode());
        append(message.get(Message.ENCODING), buffer.getEncoding());
        append(message.get(Message.HTTP_REQUEST_METHOD), buffer.getHttpMethod());
        append(message.get(Message.CONTENT_TYPE), buffer.getContentType());
        append(message.get(Message.PROTOCOL_HEADERS), buffer.getHeader());

        String uri = (String) message.get(Message.REQUEST_URL);
        if (uri == null) {
            String address = (String) message.get(Message.ENDPOINT_ADDRESS);
            uri = (String) message.get(Message.REQUEST_URI);
            if (uri != null && uri.startsWith("/")) {
                if (address != null && !address.startsWith(uri)) {
                    uri = address + uri;
                }
            } else {
                uri = address;
            }
        }
        if (uri != null) {
            buffer.getAddress().append(uri);
            String query = (String) message.get(Message.QUERY_STRING);
            if (query != null) {
                buffer.getAddress().append("?").append(query);
            }
        }

        if (isBinaryContent((String) message.get(Message.CONTENT_TYPE))) {
            buffer.getMessage().append("--- Binary Content ---").append('\n');
            handleMessage(buffer);
            return;
        }

        InputStream is = message.getContent(InputStream.class);
        if (is != null) {
            logInputStream(message, is, buffer, (String) message.get(Message.ENCODING));
        } else {
            Reader reader = message.getContent(Reader.class);
            if (reader != null) {
                logReader(message, reader, buffer);
            }
        }
        handleMessage(buffer);
    }

    private static void append(Object headers, StringBuilder builder) {
        if (headers != null) {
            builder.append(headers);
        }
    }

    private void handleMessage(LoggingMessage loggingMessage) {
        StoreLogData storeLogData = StoreLogDataHolder.get();
        storeLogData.setRequestMessage(loggingMessage);
        storeLogData.setIncomingMessageTime(ZonedDateTime.now());
    }

    protected void logReader(Message message, Reader reader, LoggingMessage buffer) {
        try {
            CachedWriter writer = new CachedWriter();
            IOUtils.copyAndCloseInput(reader, writer);
            message.setContent(Reader.class, writer.getReader());

            writer.writeCacheTo(buffer.getPayload());
        } catch (Exception e) {
            throw new Fault(e);
        }
    }

    private void logInputStream(Message message, InputStream is, LoggingMessage buffer, String encoding) {
        try (CachedOutputStream bos = new CachedOutputStream()) {
            // use the appropriate input stream and restore it later
            InputStream bis = is instanceof DelegatingInputStream ? ((DelegatingInputStream) is).getInputStream() : is;

            // only copy up to the limit since that's all we need to log
            // we can stream the rest
            bis.transferTo(bos);
            bos.flush();
            bis = new SequenceInputStream(bos.getInputStream(), bis);

            // restore the delegating input stream or the input stream
            if (is instanceof DelegatingInputStream) {
                ((DelegatingInputStream) is).setInputStream(bis);
            } else {
                message.setContent(InputStream.class, bis);
            }

            // Just transform the XML message when the cos has content

            bos.writeCacheTo(buffer.getPayload(), Objects.requireNonNullElseGet(encoding, StandardCharsets.UTF_8::name));
        } catch (Exception e) {
            throw new Fault(e);
        }
    }
}
