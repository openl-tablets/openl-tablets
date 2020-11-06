package org.openl.rules.ruleservice.databinding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("rawtypes")
public class TextPlainDateMessageProvider implements MessageBodyWriter, MessageBodyReader {

    private static final Set<Class<?>> DATE_TYPE_SET = new HashSet<>();

    static {
        DATE_TYPE_SET.add(Date.class);
        DATE_TYPE_SET.add(Instant.class);
        DATE_TYPE_SET.add(ZonedDateTime.class);
        DATE_TYPE_SET.add(LocalDateTime.class);
        DATE_TYPE_SET.add(LocalDate.class);
        DATE_TYPE_SET.add(LocalTime.class);
    }

    private ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    @Override
    public boolean isWriteable(Class aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return isAcceptable(aClass, mediaType);
    }

    @Override
    public boolean isReadable(Class aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return isAcceptable(aClass, mediaType);
    }

    @SuppressWarnings("unchecked")
    private boolean isAcceptable(Class aClass, MediaType mediaType) {
        return MediaType.TEXT_PLAIN_TYPE.isCompatible(mediaType) && DATE_TYPE_SET.stream()
            .anyMatch(aClass::isAssignableFrom);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object readFrom(Class aClass,
            Type type,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap multivaluedMap,
            InputStream inputStream) throws IOException, WebApplicationException {
        final String str = IOUtils.toStringAndClose(inputStream);
        return objectMapper.readValue(quote(str), aClass);
    }

    @Override
    public void writeTo(Object o,
            Class aClass,
            Type type,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap multivaluedMap,
            OutputStream outputStream) throws IOException {

        String str = unquote(objectMapper.writeValueAsString(o));
        outputStream.write(str.getBytes(StandardCharsets.UTF_8));
    }

    private String quote(String str) {
        return "\"" + str + "\"";
    }

    private String unquote(String str) {
        if (StringUtils.isEmpty(str)) {
            return str;
        }
        return str.substring(1, str.length() - 1);
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
