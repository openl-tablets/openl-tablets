package org.openl.rules.rest.common;

import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;

import org.openl.rules.rest.common.model.BaseError;
import org.openl.rules.rest.common.model.ValidationError;
import org.openl.rules.rest.exception.RestRuntimeException;
import org.openl.rules.rest.exception.ValidationException;
import org.openl.util.StringUtils;

/**
 * API Exception Handler
 *
 * @author Vladyslav Pikus
 */
@ControllerAdvice
@SuppressWarnings("NullableProblems")
public class ApiExceptionControllerAdvice extends ResponseEntityExceptionHandler {

    private static final String DEF_ERROR_PREFIX = "openl.error.";

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionControllerAdvice.class);

    private final MessageSource messageSource;

    @Autowired
    public ApiExceptionControllerAdvice(@Qualifier("validationMessageSource") MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationError> handleAllRestRuntimeExceptions(ValidationException e, WebRequest request) {
        var code = Optional.ofNullable(AnnotationUtils.findAnnotation(e.getClass(), ResponseStatus.class))
                .map(ResponseStatus::code)
                .orElse(HttpStatus.BAD_REQUEST);
        return _handleExceptionInternal(e,
                handleBindingResult(code, e.getBindingResult()),
                new HttpHeaders(),
                code,
                request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationError> handleConstraintViolationException(ConstraintViolationException e, WebRequest request) {
        var code = HttpStatus.BAD_REQUEST;
        return _handleExceptionInternal(e,
                handleConstraintViolations(code, e.getConstraintViolations()),
                new HttpHeaders(),
                code,
                request);
    }

    @ExceptionHandler(RestRuntimeException.class)
    public ResponseEntity<BaseError> handleAllRestRuntimeExceptions(RestRuntimeException e, WebRequest request) {
        var httpStatus = e.getHttpStatus();
        if (httpStatus != null) {
            return _handleExceptionInternal(e,
                    mapCommonException(httpStatus, e),
                    new HttpHeaders(),
                    httpStatus,
                    request);
        } else {
            HttpStatus code = HttpStatus.INTERNAL_SERVER_ERROR;
            LOG.error(e.getMessage(), e);
            return _handleExceptionInternal(e, mapCommonException(code, e), new HttpHeaders(), code, request);
        }
    }

    /**
     * Handle security exceptions which can be thrown by ACL
     */
    @ExceptionHandler({
            java.nio.file.AccessDeniedException.class,
            org.springframework.security.access.AccessDeniedException.class,
            SecurityException.class
    })
    public ResponseEntity<Object> handleSecurityErrors(Exception e, WebRequest request) {
        LOG.debug(e.getMessage(), e);
        var code = HttpStatus.FORBIDDEN;
        return handleExceptionInternal(e, mapCommonException(code, e), new HttpHeaders(), code, request);
    }

    @ExceptionHandler({Exception.class, RuntimeException.class})
    public ResponseEntity<Object> handleInternalErrors(Exception e, WebRequest request) {
        var code = Optional.ofNullable(AnnotationUtils.findAnnotation(e.getClass(), ResponseStatus.class))
                .map(ResponseStatus::code)
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
        if (code == HttpStatus.INTERNAL_SERVER_ERROR) {
            LOG.error(e.getMessage(), e);
        } else {
            LOG.debug(e.getMessage(), e);
        }
        return handleExceptionInternal(e, e.getMessage(), new HttpHeaders(), code, request);
    }

    @ExceptionHandler(ConversionFailedException.class)
    public ResponseEntity<?> handleConversionFailedException(ConversionFailedException e, WebRequest request) {
        var causeEx = e.getCause();
        if (causeEx instanceof RestRuntimeException) {
            return handleAllRestRuntimeExceptions((RestRuntimeException) causeEx, request);
        } else {
            return handleInternalErrors(e, request);
        }
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        return handleExceptionInternal(e,
                handleBindingResult(status, e.getBindingResult()),
                new HttpHeaders(),
                status,
                request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception e,
                                                             Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode status,
                                                             WebRequest request) {
        var handledEx = super.handleExceptionInternal(e, body, headers, status, request);
        if (handledEx.hasBody()) {
            var handledBody = handledEx.getBody();
            if (handledBody instanceof BaseError) {
                return handledEx;
            } else {
                var builder = BaseError.builder()
                        .message(Optional.ofNullable(handledBody)
                                .map(Object::toString)
                                .filter(StringUtils::isNotBlank)
                                .orElseGet(() -> HttpStatus.resolve(status.value()).getReasonPhrase()));
                return new ResponseEntity<>(builder.build(), handledEx.getHeaders(), handledEx.getStatusCode());
            }
        } else {
            var builder = BaseError.builder()
                    .code(buildErrorCode(status.value() + ".default.message"))
                    .message(e.getMessage());
            return new ResponseEntity<>(builder.build(), handledEx.getHeaders(), handledEx.getStatusCode());
        }
    }

    private <T extends BaseError> ResponseEntity<T> _handleExceptionInternal(Exception e,
                                                                             T body,
                                                                             HttpHeaders headers,
                                                                             HttpStatusCode status,
                                                                             WebRequest request) {
        if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, e, WebRequest.SCOPE_REQUEST);
        }
        return new ResponseEntity<>(body, headers, status);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
                                                        HttpHeaders headers,
                                                        HttpStatusCode status,
                                                        WebRequest request) {
        if (ex.getCause() instanceof ConversionFailedException) {
            return (ResponseEntity<Object>) handleConversionFailedException((ConversionFailedException) ex.getCause(), request);
        }
        var handledEx = super.handleTypeMismatch(ex, headers, status, request);
        var builder = ValidationError.builder()
                .message(HttpStatus.resolve(status.value()).getReasonPhrase())
                .addField(org.openl.rules.rest.common.model.FieldError.builder()
                        .field(getFieldName(ex))
                        .message(ex.getLocalizedMessage())
                        .rejectedValue(ex.getValue())
                        .build());
        return new ResponseEntity<>(builder.build(), handledEx.getHeaders(), handledEx.getStatusCode());
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
                        .map(fieldError -> org.openl.rules.rest.common.model.FieldError.builder()
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

    private ValidationError handleConstraintViolations(HttpStatus status, Set<ConstraintViolation<?>> constraintViolations) {
        var builder = ValidationError.builder();

        builder.message(status.getReasonPhrase());

        // Handle field errors
        constraintViolations.stream()
                .filter(violation -> isFieldError(violation.getPropertyPath()))
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString(), String.CASE_INSENSITIVE_ORDER))
                .map(violation -> org.openl.rules.rest.common.model.FieldError.builder()
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
        if (e instanceof RestRuntimeException) {
            RestRuntimeException restEx = (RestRuntimeException) e;
            builder.code(restEx.getErrorCode()).message(resolveLocalMessage(restEx));
        } else {
            builder.message(
                    Optional.ofNullable(e.getMessage()).filter(StringUtils::isNotBlank).orElseGet(status::getReasonPhrase));
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
