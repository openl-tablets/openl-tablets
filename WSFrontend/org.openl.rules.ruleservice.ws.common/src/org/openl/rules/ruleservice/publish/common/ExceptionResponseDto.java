package org.openl.rules.ruleservice.publish.common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.rules.ruleservice.core.ExceptionDetails;
import org.openl.rules.ruleservice.core.ExceptionType;
import org.openl.rules.ruleservice.core.RuleServiceWrapperException;

public class ExceptionResponseDto {

    public static final int INTERNAL_SERVER_ERROR_CODE = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    public static final int BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    public static final int UNPROCESSABLE_ENTITY = 422;

    private final String message;
    private final String code;
    private final Object[] args;
    private final int statusCode;
    private final ExceptionType type;
    private final String detail;

    private ExceptionResponseDto(ExceptionDetails exDetails, int statusCode, ExceptionType type, String detail) {
        this.message = exDetails.getMessage();
        this.statusCode = statusCode;
        this.type = type;
        this.detail = detail;
        this.code = exDetails.getCode();
        this.args = exDetails.getArgs();
    }

    public String getMessage() {
        return message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public ExceptionType getType() {
        return type;
    }

    public String getDetail() {
        return detail;
    }

    public String getCode() {
        return code;
    }

    public Object[] getArgs() {
        return args;
    }

    public static ExceptionResponseDto createFrom(Exception e) {
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
        ExceptionType type = ExceptionType.SYSTEM;
        String detail = null;
        ExceptionDetails exceptionDetails;

        if (t instanceof RuleServiceWrapperException) {
            RuleServiceWrapperException wrapperException = (RuleServiceWrapperException) t;
            exceptionDetails = wrapperException.getDetails();
            type = wrapperException.getType();
            if (isUserErrorType(type)) {
                status = UNPROCESSABLE_ENTITY;
            } else {
                detail = ExceptionUtils.getStackTrace(wrapperException.getCause());
            }
        } else {
            exceptionDetails = new ExceptionDetails(ExceptionUtils.getRootCauseMessage(e));
            detail = ExceptionUtils.getStackTrace(e);
            try {
                Class<?> jsonProcessingException = Thread.currentThread()
                    .getContextClassLoader()
                    .loadClass("com.fasterxml.jackson.core.JsonProcessingException");
                if (t != null && jsonProcessingException.isAssignableFrom(t.getClass())) {
                    status = BAD_REQUEST;
                    type = ExceptionType.BAD_REQUEST;
                }
            } catch (ClassNotFoundException ignored) {
            }
        }

        return new ExceptionResponseDto(exceptionDetails, status, type, detail);
    }

    private static boolean isUserErrorType(ExceptionType type) {
        return ExceptionType.USER_ERROR.equals(type) || ExceptionType.VALIDATION.equals(type);
    }

}
