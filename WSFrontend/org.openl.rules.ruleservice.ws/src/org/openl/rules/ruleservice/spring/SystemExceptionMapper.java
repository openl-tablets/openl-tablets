package org.openl.rules.ruleservice.spring;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;

import org.openl.rules.ruleservice.core.ExceptionType;
import org.openl.rules.ruleservice.publish.jaxrs.JAXRSErrorResponse;

/**
 * Default mapper for handling all unprocessed exceptions
 *
 * @author Yury Molchan
 */
@Component
@Provider
public class SystemExceptionMapper implements ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception exception) {
        var type = ExceptionType.SYSTEM;
        var message = ExceptionUtils.getRootCauseMessage(exception);
        var errorResponse = new JAXRSErrorResponse(message, type);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(errorResponse).build();
    }
}
