package org.openl.rules.ruleservice.publish.common;

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
    private final int statusCode;
    private final ExceptionType type;
    private final Object body;

    private ExceptionResponseDto(ExceptionDetails exDetails, int statusCode, ExceptionType type) {
        this.message = exDetails.getMessage();
        this.statusCode = statusCode;
        this.type = type;
        this.code = exDetails.getCode();
        this.body = exDetails.getBody();
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

    public String getCode() {
        return code;
    }

    public Object getBody() {
        return body;
    }

    public static ExceptionResponseDto createFrom(Exception e) {
        Throwable t = e;

        int status = INTERNAL_SERVER_ERROR_CODE;
        ExceptionType type = ExceptionType.SYSTEM;
        ExceptionDetails exceptionDetails;

        if (t instanceof RuleServiceWrapperException) {
            RuleServiceWrapperException wrapperException = (RuleServiceWrapperException) t;
            exceptionDetails = wrapperException.getDetails();
            type = wrapperException.getType();
            if (isUserErrorType(type)) {
                status = UNPROCESSABLE_ENTITY;
            }
        } else {
            exceptionDetails = new ExceptionDetails(ExceptionUtils.getRootCauseMessage(e));
        }

        return new ExceptionResponseDto(exceptionDetails, status, type);
    }

    private static boolean isUserErrorType(ExceptionType type) {
        return ExceptionType.USER_ERROR.equals(type) || ExceptionType.VALIDATION.equals(type);
    }

}
