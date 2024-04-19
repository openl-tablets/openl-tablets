package org.openl.rules.ruleservice.core;

import org.apache.commons.lang3.exception.ExceptionUtils;

import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenLRuntimeException;
import org.openl.exception.OpenLUserRuntimeException;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultBeanPropertyNamingStrategy;

/**
 * Exception for wrapping exceptions for returning them from ruleservice.
 *
 * @author Marat Kamalov
 */
public class RuleServiceWrapperException extends RuleServiceRuntimeException {

    private static final long serialVersionUID = 3618613334261575918L;

    private final Object body;
    private final ExceptionType type;

    /**
     * Constructs a new RuleServiceWrapperException with the specified detail message and cause.
     *
     * @param details error details
     * @param type    the message type
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *                value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    private RuleServiceWrapperException(Object details, ExceptionType type, String message, Throwable cause) {
        super(message, cause);
        this.body = details;
        this.type = type;
    }

    public RuleServiceWrapperException(String message, ExceptionType type) {
        super(message);
        this.body = null;
        this.type = type;
    }

    public static RuleServiceWrapperException create(Throwable ex, SpreadsheetResultBeanPropertyNamingStrategy namingStrategy) {

        Object body = null;
        var type = ExceptionType.SYSTEM;
        var message = ex.getMessage();
        for (Throwable t : ExceptionUtils.getThrowableList(ex)) {
            if (t instanceof OpenLUserRuntimeException) {
                body = ((OpenLUserRuntimeException) t).getBody();
                body = SpreadsheetResult.convertSpreadsheetResult(body, namingStrategy);
                type = ExceptionType.USER_ERROR;
                message = t.getMessage();
            } else if (t instanceof OutsideOfValidDomainException) {
                type = ExceptionType.VALIDATION;
                message = t.getMessage();
            } else if (t instanceof OpenLRuntimeException) {
                type = ExceptionType.RULES_RUNTIME;
                message = t.getMessage();
            } else if (t instanceof OpenLCompilationException) {
                type = ExceptionType.COMPILATION;
                message = t.getMessage();
            }
        }
        return new RuleServiceWrapperException(body, type, message, ex);
    }

    /**
     * Returns simple Message
     */
    public Object getBody() {
        return body;
    }

    /**
     * Returns error type
     */
    public ExceptionType getType() {
        return type;
    }
}
