package org.openl.rules.ruleservice.spring;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.springframework.stereotype.Component;

import org.openl.rules.ruleservice.core.RuleServiceWrapperException;
import org.openl.rules.ruleservice.publish.jaxrs.JAXRSErrorResponse;
import org.openl.rules.ruleservice.publish.jaxrs.JAXRSOpenLServiceEnhancerHelper;

/**
 * Process exception from the RuleService engine.
 *
 * @author Yury Molchan
 */
@Component
@Provider
public class RuleServiceExceptionMapper implements ExceptionMapper<RuleServiceWrapperException> {

    @Override
    public Response toResponse(RuleServiceWrapperException wrapperException) {

        int status;
        Object errorResponse;
        var type = wrapperException.getType();
        var message = wrapperException.getMessage();
        switch (type) {
            case VALIDATION:
                errorResponse = new JAXRSErrorResponse(message, type);
                status = JAXRSOpenLServiceEnhancerHelper.UNPROCESSABLE_ENTITY;
                break;
            case USER_ERROR:
                errorResponse = wrapperException.getBody();
                status = JAXRSOpenLServiceEnhancerHelper.UNPROCESSABLE_ENTITY;
                break;
            default:
                status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
                errorResponse = new JAXRSErrorResponse(message, type);
                break;
        }
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(errorResponse).build();
    }

}
