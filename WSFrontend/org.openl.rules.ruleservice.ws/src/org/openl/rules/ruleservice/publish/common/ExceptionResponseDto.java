package org.openl.rules.ruleservice.publish.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.rules.ruleservice.core.ExceptionType;
import org.openl.rules.ruleservice.core.RuleServiceWrapperException;

import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

public class ExceptionResponseDto {

    private static final int INTERNAL_SERVER_ERROR_CODE = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    private static final int BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    private static final int UNPROCESSABLE_ENTITY = 422;

    private String message;
    private int statusCode;
    private String type;
    private String detail;

    private ExceptionResponseDto(String message, int statusCode, String type, String detail) {
        this.message = message;
        this.statusCode = statusCode;
        this.type = type;
        this.detail = detail;
    }

    public String getMessage() {
        return message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getType() {
        return type;
    }

    public String getDetail() {
        return detail;
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
        String message = null;

        if (t instanceof RuleServiceWrapperException) {
            RuleServiceWrapperException wrapperException = (RuleServiceWrapperException) t;
            message = wrapperException.getSimpleMessage();
            type = wrapperException.getType();
            if (isUserErrorType(type)) {
                status = UNPROCESSABLE_ENTITY;
            } else {
                detail = ExceptionUtils.getStackTrace(wrapperException.getCause());
            }
        } else {
            message = ExceptionUtils.getRootCauseMessage(e);
            detail = ExceptionUtils.getStackTrace(e);
            if (t instanceof JsonProcessingException) {
                status = BAD_REQUEST;
                type = ExceptionType.BAD_REQUEST;
            }
        }

        return new ExceptionResponseDto(message, status, type.toString(), detail);
    }

    private static boolean isUserErrorType(ExceptionType type) {
        return ExceptionType.USER_ERROR.equals(type) || ExceptionType.VALIDATION.equals(type);
    }

}
