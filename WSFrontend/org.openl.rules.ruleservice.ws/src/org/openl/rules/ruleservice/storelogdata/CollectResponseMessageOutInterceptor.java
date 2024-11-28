package org.openl.rules.ruleservice.storelogdata;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Objects;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.LoggingMessage;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedOutputStreamCallback;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CXF interceptor for collecting response data for logging to external source feature.
 *
 * @author Marat Kamalov
 */
@NoJSR250Annotations
public class CollectResponseMessageOutInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOG = LoggerFactory.getLogger(CollectResponseMessageOutInterceptor.class);

    private final StoreLogDataManager storeLoggingManager;

    public StoreLogDataManager getStoreLoggingManager() {
        return storeLoggingManager;
    }

    public CollectResponseMessageOutInterceptor(StoreLogDataManager storeLoggingManager) {
        super(Phase.PRE_STREAM);
        this.storeLoggingManager = storeLoggingManager;
    }

    @Override
    public void handleMessage(Message message) {
        handleAnyMessage(message);
    }

    @Override
    public void handleFault(Message message) {
        final StoreLogData storeLogData = StoreLogDataHolder.get();
        storeLogData.fault();
    }

    private void handleAnyMessage(Message message) {
        final OutputStream os = message.getContent(OutputStream.class);
        final Writer iowriter = message.getContent(Writer.class);
        if (os == null && iowriter == null) {
            return;
        }
        if (os != null) {
            if (storeLoggingManager.isAtLeastOneSync(StoreLogDataHolder.get())) {
                CacheAndWriteOutputStream newOut = new CacheAndWriteOutputStream(os);
                message.setContent(OutputStream.class, newOut);
                newOut.registerCallback(new LoggingCallback(message, os));
            } else {
                org.apache.cxf.io.CacheAndWriteOutputStream newOut = new org.apache.cxf.io.CacheAndWriteOutputStream(
                        os);
                message.setContent(OutputStream.class, newOut);
                newOut.registerCallback(new LoggingCallback(message, os));
            }
        } else {
            message.setContent(Writer.class, new LogWriter(message, iowriter));
        }
    }

    private static LoggingMessage setupBuffer(Message message) {
        String id = (String) message.getExchange().get(CollectRequestMessageInInterceptor.ID_KEY);
        if (id == null) {
            id = LoggingMessage.nextId();
            message.getExchange().put(CollectRequestMessageInInterceptor.ID_KEY, id);
        }
        final LoggingMessage buffer = new LoggingMessage("Response", id);

        append(message.get(Message.RESPONSE_CODE), buffer.getResponseCode());
        append(message.get(Message.ENCODING), buffer.getEncoding());
        append(message.get(Message.HTTP_REQUEST_METHOD), buffer.getHttpMethod());
        append(message.get(Message.CONTENT_TYPE), buffer.getContentType());
        append(message.get(Message.PROTOCOL_HEADERS), buffer.getHeader());


        String address = (String) message.get(Message.ENDPOINT_ADDRESS);
        if (address != null) {
            buffer.getAddress().append(address);
            String uri = (String) message.get(Message.REQUEST_URI);
            if (uri != null && !address.startsWith(uri)) {
                if (!address.endsWith("/") && !uri.startsWith("/")) {
                    buffer.getAddress().append("/");
                }
                buffer.getAddress().append(uri);
            }
        }
        return buffer;
    }

    private static void append(Object responseCode, StringBuilder builder) {
        if (responseCode != null) {
            builder.append(responseCode);
        }
    }

    private class LogWriter extends FilterWriter {
        StringWriter out2;
        int count;
        Message message;
        final int lim;

        public LogWriter(Message message, Writer writer) {
            super(writer);
            this.message = message;
            if (!(writer instanceof StringWriter)) {
                out2 = new StringWriter();
            }
            lim = Integer.MAX_VALUE;
        }

        @Override
        public void write(int c) throws IOException {
            super.write(c);
            if (out2 != null && count < lim) {
                out2.write(c);
            }
            count++;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            super.write(cbuf, off, len);
            if (out2 != null && count < lim) {
                out2.write(cbuf, off, len);
            }
            count += len;
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            super.write(str, off, len);
            if (out2 != null && count < lim) {
                out2.write(str, off, len);
            }
            count += len;
        }

        @Override
        public void close() throws IOException {
            LoggingMessage buffer = setupBuffer(message);
            StringWriter w2 = out2;
            if (w2 == null) {
                w2 = (StringWriter) out;
            }
            String ct = (String) message.get(Message.CONTENT_TYPE);
            try {
                StringBuilder builder = buffer.getPayload();
                // Just transform the XML message when the cos has content
                StringBuffer buffer1 = w2.getBuffer();
                if (buffer1.length() > Integer.MAX_VALUE) {
                    builder.append(buffer1.subSequence(0, Integer.MAX_VALUE));
                } else {
                    builder.append(buffer1);
                }
            } catch (Exception e) {
                LOG.debug("Ignored error: ", e);
            }
            String id = (String) message.getExchange().get(CollectRequestMessageInInterceptor.ID_KEY);
            LoggingMessage loggingMessage = new LoggingMessage(null, id);
            loggingMessage.getContentType().append(ct);
            loggingMessage.getPayload().append(buffer);
            handleMessage(loggingMessage);
            message.setContent(Writer.class, out);
            super.close();
        }
    }

    private void handleMessage(LoggingMessage loggingMessage) throws Fault {
        final StoreLogData storeLogData = StoreLogDataHolder.get();
        try {
            storeLogData.setResponseMessage(loggingMessage);
            storeLogData.setOutcomingMessageTime(ZonedDateTime.now());
            getStoreLoggingManager().store(storeLogData);
        } catch (StoreLogDataException e) {
            throw new Fault(e);
        } finally {
            StoreLogDataHolder.remove();
        }
    }

    class LoggingCallback implements CachedOutputStreamCallback {

        private final Message message;
        private final OutputStream origStream;
        private final int lim;

        public LoggingCallback(final Message msg, final OutputStream os) {
            this.message = msg;
            this.origStream = os;
            this.lim = Integer.MAX_VALUE;
        }

        @Override
        public void onFlush(CachedOutputStream cos) {
            // nothing to do
        }

        @Override
        public void onClose(CachedOutputStream cos) {
            LoggingMessage buffer = setupBuffer(message);

            String ct = (String) message.get(Message.CONTENT_TYPE);
            if (CollectRequestMessageInInterceptor.isBinaryContent(ct)) {
                buffer.getMessage().append("--- Binary Content ---").append('\n');
                handleMessage(buffer);
                return;
            }

            try {
                String encoding = (String) message.get(Message.ENCODING);
                // Just transform the XML message when the cos has content

                cos.writeCacheTo(buffer.getPayload(), Objects.requireNonNullElseGet(encoding, StandardCharsets.UTF_8::name));
            } catch (Exception e) {
                LOG.debug("Error occurred: ", e);
            }
            Fault fault = null;
            try {
                handleMessage(buffer);
            } catch (Fault f) {
                fault = f;
            }
            try {
                // empty out the cache
                cos.lockOutputStream();
                cos.resetOut(null, false);
            } catch (Exception e) {
                LOG.debug("Ignored error: ", e);
            }
            message.setContent(OutputStream.class, origStream);
            if (fault != null) {
                throw fault;
            } else {
                if (cos instanceof CacheAndWriteOutputStream) {
                    try {
                        ((CacheAndWriteOutputStream) cos).copyCacheToFlowThroughStream();
                    } catch (Exception e) {
                        LOG.debug("Ignored error: ", e);
                    }
                }
            }
        }
    }
}
