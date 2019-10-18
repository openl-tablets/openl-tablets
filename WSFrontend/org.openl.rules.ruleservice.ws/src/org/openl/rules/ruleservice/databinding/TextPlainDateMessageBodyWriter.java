package org.openl.rules.ruleservice.databinding;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

@SuppressWarnings("rawtypes")
public class TextPlainDateMessageBodyWriter implements MessageBodyWriter {

    private static final Set<Class<?>> DATE_TYPE_SET = new HashSet<>();

    static {
        DATE_TYPE_SET.add(Date.class);
        DATE_TYPE_SET.add(ZonedDateTime.class);
        DATE_TYPE_SET.add(LocalDateTime.class);
        DATE_TYPE_SET.add(LocalDate.class);
        DATE_TYPE_SET.add(LocalTime.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isWriteable(Class aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return MediaType.TEXT_PLAIN_TYPE.equals(mediaType) && DATE_TYPE_SET.stream().anyMatch(aClass::isAssignableFrom);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeTo(Object o,
            Class aClass,
            Type type,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap multivaluedMap,
            OutputStream outputStream) throws IOException {
        String text = null;
        if (aClass.isAssignableFrom(Date.class)) {
            text = dateToString((Date) o);
        } else if (aClass.isAssignableFrom(ZonedDateTime.class)) {
            text = zonedDateTimeToString((ZonedDateTime) o);
        } else if (aClass.isAssignableFrom(LocalDateTime.class)) {
            text = localDateTimeToString((LocalDateTime) o);
        } else if (aClass.isAssignableFrom(LocalDate.class)) {
            text = localDateToString((LocalDate) o);
        } else if (aClass.isAssignableFrom(LocalTime.class)) {
            text = localTimeToString((LocalTime) o);
        } else {
            // should not happen as previously checked by method isWriteable
            throw new IllegalStateException("Unsupportable date type.");
        }
        outputStream.write(text.getBytes(StandardCharsets.UTF_8));
    }

    private String dateToString(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        if (cal.get(Calendar.SECOND) != 0) {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(date);
        } else if (cal.get(Calendar.HOUR_OF_DAY) != 0 || cal.get(Calendar.MINUTE) != 0) {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mmXXX").format(date);
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private String localDateToString(LocalDate localDate) {
        return DateTimeFormatter.ISO_LOCAL_DATE.format(localDate);
    }

    private String localTimeToString(LocalTime localTime) {
        if (localTime.getNano() != 0) {
            return DateTimeFormatter.ISO_LOCAL_TIME.format(localTime);
        } else if (localTime.getSecond() != 0) {
            return DateTimeFormatter.ofPattern("hh:mm:ss").format(localTime);
        }
        return DateTimeFormatter.ofPattern("hh:mm").format(localTime);
    }

    private String localDateTimeToString(LocalDateTime localDateTime) {
        if (localDateTime.getNano() != 0) {
            return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localDateTime);
        } else if (localDateTime.getSecond() != 0) {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(localDateTime);
        }
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(localDateTime);
    }

    private String zonedDateTimeToString(ZonedDateTime zonedDateTime) {
        if (zonedDateTime.getNano() != 0) {
            return DateTimeFormatter.ISO_ZONED_DATE_TIME.format(zonedDateTime);
        } else if (zonedDateTime.getSecond() != 0) {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'").format(zonedDateTime);
        }
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmXXX'['VV']'").format(zonedDateTime);
    }
}
