package org.openl.rules.ruleservice.spring;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.springframework.stereotype.Component;

import org.openl.rules.ruleservice.core.ExceptionType;
import org.openl.rules.ruleservice.publish.jaxrs.JAXRSErrorResponse;

/**
 * Wraps the response into the JSON structure.
 *
 * @author Yury Molchan
 */
@Component
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {
    @Override
    public Response toResponse(WebApplicationException exception) {
        var response = exception.getResponse();
        if (response != null && response.hasEntity()) {
            return response;
        }
        if (response == null) {
            response = Response.serverError().build();
        }
        var originalMessage = exception.getMessage();
        var cause = exception.getCause();
        var message = cause != null ? cause.getMessage() : originalMessage;
        if (message == null) {
            message = cause != null ? cause.getClass().getName() : ((Throwable) exception).getClass().getName();
        }
        return JAXRSUtils.fromResponse(response, false)
                .type(MediaType.APPLICATION_JSON)
                .entity(new JAXRSErrorResponse(message, ExceptionType.SYSTEM))
                .build();
    }
}
