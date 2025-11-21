package org.openl.studio.common;

import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import org.openl.rules.rest.exception.RestRuntimeException;
import org.openl.rules.rest.exception.ValidationException;
import org.openl.studio.common.model.BaseError;
import org.openl.studio.common.model.ValidationError;
import org.openl.util.StringUtils;

@Component
public class ExceptionMappingService {

    private static final String DEF_ERROR_PREFIX = "openl.error.";

    private final MessageSource messageSource;

    public ExceptionMappingService(@Qualifier("validationMessageSource") MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String buildDefaultErrorCode(HttpStatusCode status) {
        return buildErrorCode(status.value() + ".default.message");
    }

    public BaseError processException(Exception ex) {
        return switch (ex) {
            case ConversionFailedException error when error.getCause() instanceof RestRuntimeException rex ->
                    processRestRuntimeException(rex);
            case ValidationException error -> {
                var code = Optional.ofNullable(AnnotationUtils.findAnnotation(error.getClass(), ResponseStatus.class))
                        .map(ResponseStatus::code)
                        .orElse(HttpStatus.BAD_REQUEST);
                yield handleBindingResult(code, error.getBindingResult());
            }
            case ConstraintViolationException error ->
                    handleConstraintViolations(error.getConstraintViolations());
            case MethodArgumentNotValidException error ->
                    handleBindingResult(HttpStatus.BAD_REQUEST, error.getBindingResult());
            case RestRuntimeException error -> processRestRuntimeException(error);
            case TypeMismatchException error -> handleTypeMismatchException(error);
            case Exception error when isSecurityException(error) -> mapCommonException(HttpStatus.FORBIDDEN, error);
            default -> {
                var httpStatus = Optional.ofNullable(AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class))
                        .map(ResponseStatus::code)
                        .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
                yield mapCommonException(httpStatus, ex);
            }
        };
    }

    private static boolean isSecurityException(Exception ex) {
        return ex instanceof java.nio.file.AccessDeniedException ||
                ex instanceof org.springframework.security.access.AccessDeniedException ||
                ex instanceof SecurityException;
    }

    private BaseError processRestRuntimeException(RestRuntimeException ex) {
        var httpStatus = Optional.ofNullable(ex.getHttpStatus())
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
        return mapCommonException(httpStatus, ex);
    }

    private ValidationError handleTypeMismatchException(TypeMismatchException ex) {
        return ValidationError.builder()
                .message(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .addField(org.openl.studio.common.model.FieldError.builder()
                        .field(getFieldName(ex))
                        .message(ex.getLocalizedMessage())
                        .rejectedValue(ex.getValue())
                        .build())
                .build();
    }

    private static String getFieldName(TypeMismatchException ex) {
        if (ex instanceof MethodArgumentTypeMismatchException) {
            return ((MethodArgumentTypeMismatchException) ex).getName();
        }
        return ex.getPropertyName();
    }

    private ValidationError handleBindingResult(HttpStatusCode status, BindingResult bindingResult) {
        var builder = ValidationError.builder();
        if (bindingResult.getGlobalErrorCount() == 1 && !bindingResult.hasFieldErrors()) {
            builder.code(buildErrorCode(bindingResult.getGlobalError().getCode()))
                    .message(resolveLocalMessage(bindingResult.getGlobalError()));
        } else {
            builder.message(HttpStatus.resolve(status.value()).getReasonPhrase());
            if (bindingResult.hasFieldErrors()) {
                bindingResult.getFieldErrors()
                        .stream()
                        .sorted(Comparator.comparing(FieldError::getField, String.CASE_INSENSITIVE_ORDER))
                        .map(fieldError -> org.openl.studio.common.model.FieldError.builder()
                                .code(buildErrorCode(fieldError.getCode()))
                                .field(fieldError.getField())
                                .rejectedValue(fieldError.getRejectedValue())
                                .message(resolveLocalMessage(fieldError))
                                .build())
                        .forEach(builder::addField);
            }
            if (bindingResult.hasGlobalErrors()) {
                bindingResult.getGlobalErrors()
                        .stream()
                        .sorted(Comparator.comparing(ObjectError::getCode, String.CASE_INSENSITIVE_ORDER))
                        .map(objErr -> BaseError.builder()
                                .code(buildErrorCode(objErr.getCode()))
                                .message(resolveLocalMessage(objErr))
                                .build())
                        .forEach(builder::addError);
            }
        }
        return builder.build();
    }

    private ValidationError handleConstraintViolations(Set<ConstraintViolation<?>> constraintViolations) {
        var builder = ValidationError.builder();

        builder.message(HttpStatus.BAD_REQUEST.getReasonPhrase());

        // Handle field errors
        constraintViolations.stream()
                .filter(violation -> isFieldError(violation.getPropertyPath()))
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString(), String.CASE_INSENSITIVE_ORDER))
                .map(violation -> org.openl.studio.common.model.FieldError.builder()
                        .code(buildErrorCode(violation.getMessageTemplate()))
                        .field(violation.getPropertyPath().toString())
                        .rejectedValue(violation.getInvalidValue())
                        .message(violation.getMessage())
                        .build())
                .forEach(builder::addField);

        // Handle global errors
        constraintViolations.stream()
                .filter(violation -> !isFieldError(violation.getPropertyPath()))
                .sorted(Comparator.comparing(ConstraintViolation::getMessageTemplate, String.CASE_INSENSITIVE_ORDER))
                .map(violation -> BaseError.builder()
                        .code(buildErrorCode(violation.getMessageTemplate()))
                        .message(violation.getMessage())
                        .build())
                .forEach(builder::addError);

        return builder.build();
    }

    private boolean isFieldError(Path propertyPath) {
        return propertyPath != null && !propertyPath.toString().isEmpty();
    }

    private String resolveLocalMessage(ObjectError error) {
        if (error == null) {
            return null;
        }
        if (error.getCodes() != null) {
            for (String code : error.getCodes()) {
                try {
                    return messageSource.getMessage(buildErrorCode(code), error.getArguments(), Locale.US);
                } catch (NoSuchMessageException ignored) {
                }
            }
            if (error.getDefaultMessage() == null || error.getDefaultMessage().isBlank()) {
                // if no default message just return first code
                return buildErrorCode(error.getCodes()[0]);
            }
        }
        return error.getDefaultMessage();
    }

    private String resolveLocalMessage(RestRuntimeException e) {
        if (e.getErrorCode() != null) {
            try {
                return messageSource.getMessage(e.getErrorCode(), e.getArgs(), Locale.US);
            } catch (NoSuchMessageException ignored) {
                return e.getErrorCode();
            }
        }
        return e.getMessage();
    }

    private BaseError mapCommonException(HttpStatus status, Exception e) {
        var builder = BaseError.builder();
        if (e instanceof RestRuntimeException restEx) {
            builder.code(restEx.getErrorCode()).message(resolveLocalMessage(restEx));
        } else {
            builder.message(Optional.ofNullable(e.getMessage())
                    .filter(StringUtils::isNotBlank)
                    .orElseGet(status::getReasonPhrase));
        }
        return builder.build();
    }

    private static String buildErrorCode(String errorSuffix) {
        if (errorSuffix == null) {
            return null;
        }
        return DEF_ERROR_PREFIX + errorSuffix;
    }
}
