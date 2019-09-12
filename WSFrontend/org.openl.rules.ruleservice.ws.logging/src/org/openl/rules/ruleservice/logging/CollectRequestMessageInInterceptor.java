package org.openl.rules.ruleservice.logging;

import java.io.InputStream;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.util.Date;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.LoggingMessage;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedWriter;
import org.apache.cxf.io.DelegatingInputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

/**
 * CXF interceptor for collecting request data for logging to external source feature.
 *
 * @author Marat Kamalov
 *
 */
@NoJSR250Annotations
public class CollectRequestMessageInInterceptor extends AbstractProcessLoggingMessageInterceptor {

    public static final String ID_KEY = CollectRequestMessageInInterceptor.class.getName() + ".ID";

    public CollectRequestMessageInInterceptor() {
        super(Phase.RECEIVE);
    }

    public CollectRequestMessageInInterceptor(String phase) {
        super(phase);
    }

    public CollectRequestMessageInInterceptor(String id, String phase) {
        super(id, phase);
    }

    public CollectRequestMessageInInterceptor(int lim) {
        this();
        limit = lim;
    }

    public CollectRequestMessageInInterceptor(String id, int lim) {
        this(id, Phase.RECEIVE);
        limit = lim;
    }

    @Override
    public void handleMessage(Message message) {
        store(message);
    }

    protected void store(Message message) {
        if (message.containsKey(ID_KEY)) {
            return;
        }
        String id = (String) message.getExchange().get(ID_KEY);
        if (id == null) {
            id = LoggingMessage.nextId();
            message.getExchange().put(ID_KEY, id);
        }
        message.put(ID_KEY, id);
        final LoggingMessage buffer = new LoggingMessage("Inbound Message\n----------------------------", id);

        if (!Boolean.TRUE.equals(message.get(Message.DECOUPLED_CHANNEL_MESSAGE))) {
            // avoid logging the default responseCode 200 for the decoupled
            // responses
            Integer responseCode = (Integer) message.get(Message.RESPONSE_CODE);
            if (responseCode != null) {
                buffer.getResponseCode().append(responseCode);
            }
        }

        String encoding = (String) message.get(Message.ENCODING);

        if (encoding != null) {
            buffer.getEncoding().append(encoding);
        }
        String httpMethod = (String) message.get(Message.HTTP_REQUEST_METHOD);
        if (httpMethod != null) {
            buffer.getHttpMethod().append(httpMethod);
        }
        String ct = (String) message.get(Message.CONTENT_TYPE);
        if (ct != null) {
            buffer.getContentType().append(ct);
        }
        Object headers = message.get(Message.PROTOCOL_HEADERS);

        if (headers != null) {
            buffer.getHeader().append(headers);
        }
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

        if (!isSaveBinaryContent() && isBinaryContent(ct)) {
            buffer.getMessage().append(BINARY_CONTENT_MESSAGE).append('\n');
            handleMessage(buffer);
            return;
        }

        InputStream is = message.getContent(InputStream.class);
        if (is != null) {
            logInputStream(message, is, buffer, encoding, ct);
        } else {
            Reader reader = message.getContent(Reader.class);
            if (reader != null) {
                logReader(message, reader, buffer);
            }
        }
        handleMessage(buffer);
    }

    @Override
    protected void handleMessage(LoggingMessage message) throws Fault {
        RuleServiceStoreLoggingData ruleServiceStoreLoggingData = RuleServiceStoreLoggingDataolder.get();
        ruleServiceStoreLoggingData.setRequestMessage(message);
        ruleServiceStoreLoggingData.setIncomingMessageTime(new Date());
    }

    protected void logReader(Message message, Reader reader, LoggingMessage buffer) {
        try {
            CachedWriter writer = new CachedWriter();
            IOUtils.copyAndCloseInput(reader, writer);
            message.setContent(Reader.class, writer.getReader());

            if (writer.getTempFile() != null) {
                // large thing on disk...
                buffer.getMessage().append("\nMessage (saved to tmp file):\n");
                buffer.getMessage().append("Filename: " + writer.getTempFile().getAbsolutePath() + "\n");
            }
            if (writer.size() > limit && limit != -1) {
                buffer.getMessage().append("(message truncated to " + limit + " bytes)\n");
            }
            writer.writeCacheTo(buffer.getPayload(), limit);
        } catch (Exception e) {
            throw new Fault(e);
        }
    }

    protected void logInputStream(Message message, InputStream is, LoggingMessage buffer, String encoding, String ct) {
        try (CachedOutputStream bos = new CachedOutputStream()) {
            if (threshold > 0) {
                bos.setThreshold(threshold);
            }
            // use the appropriate input stream and restore it later
            InputStream bis = is instanceof DelegatingInputStream ? ((DelegatingInputStream) is).getInputStream() : is;

            // only copy up to the limit since that's all we need to log
            // we can stream the rest
            IOUtils.copyAtLeast(bis, bos, limit == -1 ? Integer.MAX_VALUE : limit);
            bos.flush();
            bis = new SequenceInputStream(bos.getInputStream(), bis);

            // restore the delegating input stream or the input stream
            if (is instanceof DelegatingInputStream) {
                ((DelegatingInputStream) is).setInputStream(bis);
            } else {
                message.setContent(InputStream.class, bis);
            }

            if (bos.getTempFile() != null) {
                // large thing on disk...
                buffer.getMessage().append("\nMessage (saved to tmp file):\n");
                buffer.getMessage().append("Filename: " + bos.getTempFile().getAbsolutePath() + "\n");
            }
            if (bos.size() > limit && limit != -1) {
                buffer.getMessage().append("(message truncated to " + limit + " bytes)\n");
            }
            writePayload(buffer.getPayload(), bos, encoding, ct);
        } catch (Exception e) {
            throw new Fault(e);
        }
    }

    protected String formatLoggingMessage(LoggingMessage loggingMessage) {
        return loggingMessage.toString();
    }
}
