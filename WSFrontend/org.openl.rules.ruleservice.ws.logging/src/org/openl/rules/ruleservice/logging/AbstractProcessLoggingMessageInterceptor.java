package org.openl.rules.ruleservice.logging;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.LoggingMessage;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.staxutils.PrettyPrintXMLStreamWriter;
import org.apache.cxf.staxutils.StaxUtils;

/**
 * Abstract CXF interceptor for collecting data for logging to external source feature.
 * 
 * @author Marat Kamalov
 *
 */
public abstract class AbstractProcessLoggingMessageInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final int DEFAULT_LIMIT = 1024 * 1024;
    protected static final String BINARY_CONTENT_MESSAGE = "--- Binary Content ---";
    private static final List<String> BINARY_CONTENT_MEDIA_TYPES;

    static {
        BINARY_CONTENT_MEDIA_TYPES = new ArrayList<>();
        BINARY_CONTENT_MEDIA_TYPES.add("application/octet-stream");
        BINARY_CONTENT_MEDIA_TYPES.add("image/png");
        BINARY_CONTENT_MEDIA_TYPES.add("image/jpeg");
        BINARY_CONTENT_MEDIA_TYPES.add("image/gif");
    }

    protected int limit = DEFAULT_LIMIT;
    protected long threshold = -1;
    protected boolean prettyLogging;
    private boolean saveBinaryContent;

    public AbstractProcessLoggingMessageInterceptor(String phase) {
        super(phase);
    }

    public AbstractProcessLoggingMessageInterceptor(String id, String phase) {
        super(id, phase);
    }

    public void setLimit(int lim) {
        limit = lim;
    }

    public int getLimit() {
        return limit;
    }

    public void setPrettyLogging(boolean flag) {
        prettyLogging = flag;
    }

    public boolean isPrettyLogging() {
        return prettyLogging;
    }

    protected void writePayload(StringBuilder builder,
            CachedOutputStream cos,
            String encoding,
            String contentType) throws Exception {
        // Just transform the XML message when the cos has content
        if (isPrettyLogging() && (contentType != null && contentType.indexOf("xml") >= 0 && contentType.toLowerCase()
            .indexOf("multipart/related") < 0) && cos.size() > 0) {

            StringWriter swriter = new StringWriter();
            XMLStreamWriter xwriter = StaxUtils.createXMLStreamWriter(swriter);
            xwriter = new PrettyPrintXMLStreamWriter(xwriter, 2);
            try (InputStream in = cos.getInputStream()) {
                StaxUtils.copy(new StreamSource(in), xwriter);
            } catch (XMLStreamException xse) {
                // ignore
            } finally {
                try {
                    xwriter.flush();
                    xwriter.close();
                } catch (XMLStreamException xse2) {
                    // ignore
                }
            }

            String result = swriter.toString();
            if (result.length() < limit || limit == -1) {
                builder.append(swriter.toString());
            } else {
                builder.append(swriter.toString().substring(0, limit));
            }

        } else {
            if (StringUtils.isEmpty(encoding)) {
                cos.writeCacheTo(builder, limit);
            } else {
                cos.writeCacheTo(builder, encoding, limit);
            }
        }
    }

    protected void writePayload(StringBuilder builder, StringWriter stringWriter, String contentType) throws Exception {
        // Just transform the XML message when the cos has content
        if (isPrettyLogging() && contentType != null && contentType.indexOf("xml") >= 0 && stringWriter.getBuffer()
            .length() > 0) {

            StringWriter swriter = new StringWriter();
            XMLStreamWriter xwriter = StaxUtils.createXMLStreamWriter(swriter);
            xwriter = new PrettyPrintXMLStreamWriter(xwriter, 2);
            StaxUtils.copy(new StreamSource(new StringReader(stringWriter.getBuffer().toString())), xwriter);
            xwriter.close();

            String result = swriter.toString();
            if (result.length() < limit || limit == -1) {
                builder.append(swriter.toString());
            } else {
                builder.append(swriter.toString().substring(0, limit));
            }

        } else {
            StringBuffer buffer = stringWriter.getBuffer();
            if (buffer.length() > limit) {
                builder.append(buffer.subSequence(0, limit));
            } else {
                builder.append(buffer);
            }
        }
    }

    protected abstract void handleMessage(LoggingMessage message);

    public void setSaveBinaryContent(boolean saveBinaryContent) {
        this.saveBinaryContent = saveBinaryContent;
    }

    public boolean isSaveBinaryContent() {
        return saveBinaryContent;
    }

    public boolean isBinaryContent(String contentType) {
        return contentType != null && BINARY_CONTENT_MEDIA_TYPES.contains(contentType);
    }
}
