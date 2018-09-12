package org.openl.rules.ruleservice.publish.jaxrs;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.rules.ruleservice.core.ExceptionType;
import org.openl.rules.ruleservice.core.RuleServiceWrapperException;

import com.fasterxml.jackson.core.JsonProcessingException;

public class JAXRSExceptionMapper implements ExceptionMapper<Exception> {

    private static final int INTERNAL_SERVER_ERROR_CODE = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    private static final int BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    private static final int UNPROCESSABLE_ENTITY = 422;

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

        int status = INTERNAL_SERVER_ERROR_CODE;
        if (t instanceof RuleServiceWrapperException) {
            RuleServiceWrapperException ruleServiceWrapperException = (RuleServiceWrapperException) t;
            ExceptionType type = ruleServiceWrapperException.getType();
            if (ExceptionType.USER_ERROR.equals(type) || ExceptionType.VALIDATION.equals(type)) {
                status = UNPROCESSABLE_ENTITY;
            }
            JAXRSErrorResponse errorResponse = new JAXRSErrorResponse(ruleServiceWrapperException.getSimpleMessage(),
                type.toString(), INTERNAL_SERVER_ERROR_CODE == status
                                                                     ? ExceptionUtils
                                                                         .getStackTrace(
                                                                             ruleServiceWrapperException.getCause())
                                                                         .replaceAll("\t", "    ")
                                                                         .split(System.lineSeparator())
                                                                     : null);
            return Response.status(status).entity(errorResponse).build();
        }
        ExceptionType type = ExceptionType.SYSTEM;
        if (t instanceof JsonProcessingException) {
            status = BAD_REQUEST;
            type = ExceptionType.BAD_REQUEST;
        }

        JAXRSErrorResponse errorResponse = new JAXRSErrorResponse(ExceptionUtils.getRootCauseMessage(e),
                type.toString(),
                ExceptionUtils.getStackTrace(e).replaceAll("\t", "    ").split(System.lineSeparator()));
        return Response.status(status)
            .type(MediaType.APPLICATION_JSON)
            .entity(errorResponse)
            .build();
    }

}
