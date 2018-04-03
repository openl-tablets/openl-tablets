package org.openl.rules.ruleservice.publish.jaxrs;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.rules.ruleservice.core.ExceptionType;
import org.openl.rules.ruleservice.core.RuleServiceWrapperException;

public class JAXRSExceptionMapper implements ExceptionMapper<Exception> {

    public JAXRSExceptionMapper() {
    }

    @Override
    public Response toResponse(Exception e) {
        Throwable t = e;
        while (t instanceof InvocationTargetException || t instanceof UndeclaredThrowableException) {
            if (t instanceof InvocationTargetException) {
                t = ((InvocationTargetException) t).getTargetException();
            }
            if (t instanceof UndeclaredThrowableException) {
                t = ((UndeclaredThrowableException) t).getUndeclaredThrowable();
            }
        }

        if (t instanceof RuleServiceWrapperException) {
            RuleServiceWrapperException ruleServiceWrapperException = (RuleServiceWrapperException) t;
            Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
            ExceptionType type = ruleServiceWrapperException.getType();
            if (ExceptionType.USER_ERROR.equals(type)) {
                status = Response.Status.BAD_REQUEST;
            }
            JAXRSErrorResponse errorResponse = new JAXRSErrorResponse(ruleServiceWrapperException.getSimpleMessage(),
                type.toString(),
                Response.Status.INTERNAL_SERVER_ERROR.equals(status)
                                                                     ? ExceptionUtils
                                                                         .getStackTrace(
                                                                             ruleServiceWrapperException.getCause())
                                                                         .replaceAll("\t", "    ")
                                                                         .split(System.lineSeparator())
                                                                     : null);
            return Response.status(status).entity(errorResponse).build();
        }

        JAXRSErrorResponse errorResponse = new JAXRSErrorResponse(ExceptionUtils.getRootCauseMessage(e),
            ExceptionType.SYSTEM.toString(), 
            ExceptionUtils.getStackTrace(e).replaceAll("\t", "    ").split(System.lineSeparator()));
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .type(MediaType.APPLICATION_JSON)
            .entity(errorResponse)
            .build();
    }

}
