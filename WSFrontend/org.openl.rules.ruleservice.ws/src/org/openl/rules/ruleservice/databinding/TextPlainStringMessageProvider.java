package org.openl.rules.ruleservice.databinding;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.openl.meta.StringValue;
import org.openl.util.IOUtils;

/**
 * {@link StringValue} message body provider
 *
 * @author Vladyslav Pikus
 */
public class TextPlainStringMessageProvider implements MessageBodyWriter<StringValue>, MessageBodyReader<StringValue> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isAcceptable(type, mediaType);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isAcceptable(type, mediaType);
    }

    @Override
    public void writeTo(StringValue str,
            Class<?> aClass,
            Type type,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, Object> headers,
            OutputStream outputStream) throws IOException, WebApplicationException {
        String encoding = HttpUtils.getSetEncoding(mediaType, headers, StandardCharsets.UTF_8.name());
        outputStream.write(str.getValue().getBytes(encoding));
    }

    private boolean isAcceptable(Class<?> type, MediaType mediaType) {
        return MediaType.TEXT_PLAIN_TYPE.isCompatible(mediaType) && type == StringValue.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public StringValue readFrom(Class aClass,
            Type type,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap headers,
            InputStream input) throws IOException, WebApplicationException {
        String encoding = HttpUtils.getSetEncoding(mediaType, headers, StandardCharsets.UTF_8.name());
        try (Reader reader = new InputStreamReader(input, encoding)) {
            StringWriter writer = new StringWriter();
            IOUtils.copy(reader, writer);
            return new StringValue(writer.toString());
        }
    }

}
