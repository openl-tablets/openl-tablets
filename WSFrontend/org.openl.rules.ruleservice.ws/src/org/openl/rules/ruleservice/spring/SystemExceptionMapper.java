package org.openl.rules.ruleservice.spring;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final Logger log = LoggerFactory.getLogger(SystemExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        log.error("Something went wrong...", exception);
        var type = ExceptionType.SYSTEM;
        var message = ExceptionUtils.getRootCauseMessage(exception);
        var errorResponse = new JAXRSErrorResponse(message, type);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(errorResponse).build();
    }
}
