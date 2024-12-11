package org.openl.rules.ruleservice.spring;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Component;

import org.openl.rules.ruleservice.core.ExceptionType;
import org.openl.rules.ruleservice.publish.jaxrs.JAXRSErrorResponse;

/**
 * Returns 'Bad Request' for malformed JSON objects.
 *
 * @author Yury Molchan
 */
@Component
@Provider
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {
    @Override
    public Response toResponse(JsonProcessingException exception) {
        var message = exception.getMessage();
        var details = new JAXRSErrorResponse(message, ExceptionType.BAD_REQUEST);
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(details).build();
    }
}
