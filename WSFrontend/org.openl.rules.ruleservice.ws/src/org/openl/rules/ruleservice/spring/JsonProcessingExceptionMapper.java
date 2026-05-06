package org.openl.rules.ruleservice.spring;

import java.util.Date;
import java.util.List;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.springframework.stereotype.Component;

import org.openl.rules.ruleservice.core.ExceptionType;
import org.openl.rules.ruleservice.publish.jaxrs.JAXRSErrorResponse;

/**
 * Returns 'Bad Request' for malformed JSON requests with sanitized, user-friendly messages.
 * Raw Jackson messages are deliberately discarded because they expose internal class names,
 * package paths, framework details, and generated OpenL bean names.
 *
 * @author Yury Molchan
 */
@Component
@Provider
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {

    @Override
    public Response toResponse(JsonProcessingException exception) {
        var message = buildMessage(exception);
        var details = new JAXRSErrorResponse(message, ExceptionType.BAD_REQUEST);
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(details).build();
    }

    private static String buildMessage(JsonProcessingException exception) {
        if (exception instanceof UnrecognizedPropertyException upe) {
            var field = formatPath(upe.getPath());
            return field.isEmpty() ? "Unknown field in request" : "Unknown field '%s'".formatted(field);
        }
        if (exception instanceof InvalidFormatException ife) {
            var field = formatPath(ife.getPath());
            var typeName = friendlyType(ife.getTargetType());
            return field.isEmpty() ? "Invalid %s format".formatted(typeName) : "Invalid %s format for field '%s'".formatted(typeName, field);
        }
        if (exception instanceof JsonMappingException jme) {
            var field = formatPath(jme.getPath());
            return field.isEmpty() ? "Invalid request body" : "Invalid value for field '%s'".formatted(field);
        }
        return "Request body is malformed";
    }

    private static String formatPath(List<JsonMappingException.Reference> path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        var sb = new StringBuilder();
        for (var ref : path) {
            var name = ref.getFieldName();
            if (name != null) {
                if (!sb.isEmpty()) {
                    sb.append('.');
                }
                sb.append(name);
            } else if (ref.getIndex() >= 0) {
                sb.append('[').append(ref.getIndex()).append(']');
            }
        }
        return sb.toString();
    }

    private static String friendlyType(Class<?> type) {
        return switch (type) {
            case Class<?> t when Date.class.isAssignableFrom(t) || t.getName().startsWith("java.time.") -> "date";
            case Class<?> t when t == Boolean.class || t == boolean.class -> "boolean";
            case Class<?> t when Number.class.isAssignableFrom(t) || t.isPrimitive() && t != char.class && t != void.class ->
                    "number";
            case Class<?> t when CharSequence.class.isAssignableFrom(t) || t == char.class || t == Character.class ->
                    "string";
            case Class<?> t when t.isEnum() -> "enum";
            case null, default -> "value";
        };
    }
}
