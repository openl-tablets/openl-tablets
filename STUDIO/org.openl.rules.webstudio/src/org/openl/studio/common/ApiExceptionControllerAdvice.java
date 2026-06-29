package org.openl.studio.common;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import jakarta.validation.ConstraintViolationException;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;

import org.openl.studio.common.exception.RestRuntimeException;
import org.openl.studio.common.exception.ValidationException;
import org.openl.studio.common.model.BaseError;
import org.openl.studio.common.model.ValidationError;
import org.openl.util.StringUtils;

/**
 * API Exception Handler
 *
 * @author Vladyslav Pikus
 */
@ControllerAdvice
@SuppressWarnings("NullableProblems")
@Slf4j
public class ApiExceptionControllerAdvice extends ResponseEntityExceptionHandler {


    private final ExceptionMappingService exceptionMappingService;

    public ApiExceptionControllerAdvice(ExceptionMappingService exceptionMappingService) {
        this.exceptionMappingService = exceptionMappingService;
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationError> handleAllRestRuntimeExceptions(ValidationException e, WebRequest request) {
        var code = Optional.ofNullable(AnnotationUtils.findAnnotation(e.getClass(), ResponseStatus.class))
                .map(ResponseStatus::code)
                .orElse(HttpStatus.BAD_REQUEST);
        return _handleExceptionInternal(e,
                (ValidationError) exceptionMappingService.processException(e),
                new HttpHeaders(),
                code,
                request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationError> handleConstraintViolationException(ConstraintViolationException e, WebRequest request) {
        var code = HttpStatus.BAD_REQUEST;
        return _handleExceptionInternal(e,
                (ValidationError) exceptionMappingService.processException(e),
                new HttpHeaders(),
                code,
                request);
    }

    @ExceptionHandler(RestRuntimeException.class)
    public ResponseEntity<BaseError> handleAllRestRuntimeExceptions(RestRuntimeException e, WebRequest request) {
        var httpStatus = e.getHttpStatus();
        if (httpStatus != null) {
            return _handleExceptionInternal(e,
                    exceptionMappingService.processException(e),
                    new HttpHeaders(),
                    httpStatus,
                    request);
        } else {
            HttpStatus code = HttpStatus.INTERNAL_SERVER_ERROR;
            log.error(e.getMessage(), e);
            return _handleExceptionInternal(e, exceptionMappingService.processException(e), new HttpHeaders(), code, request);
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
        log.debug(e.getMessage(), e);
        var code = HttpStatus.FORBIDDEN;
        return handleExceptionInternal(e, exceptionMappingService.processException(e), new HttpHeaders(), code, request);
    }

    /**
     * A malformed file path (absolute, or with illegal characters) is a client error, not a server
     * fault — answer 400 instead of letting it fall through to a 500.
     */
    @ExceptionHandler(java.nio.file.InvalidPathException.class)
    public ResponseEntity<Object> handleInvalidPath(java.nio.file.InvalidPathException e, WebRequest request) {
        log.debug(e.getMessage(), e);
        return handleExceptionInternal(e, e.getMessage(), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({Exception.class, RuntimeException.class})
    public ResponseEntity<Object> handleInternalErrors(Exception e, WebRequest request) {
        var code = Optional.ofNullable(AnnotationUtils.findAnnotation(e.getClass(), ResponseStatus.class))
                .map(ResponseStatus::code)
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
        if (code == HttpStatus.INTERNAL_SERVER_ERROR) {
            log.error(e.getMessage(), e);
        } else {
            log.debug(e.getMessage(), e);
        }
        return handleExceptionInternal(e, e.getMessage(), new HttpHeaders(), code, request);
    }

    @ExceptionHandler(ConversionFailedException.class)
    public ResponseEntity<?> handleConversionFailedException(ConversionFailedException e, WebRequest request) {
        if (e.getCause() instanceof RestRuntimeException ex) {
            return handleAllRestRuntimeExceptions(ex, request);
        }
        // A value that cannot be converted to the target type is a malformed request, not a server error.
        var message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
        return handleExceptionInternal(e, message, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        return handleExceptionInternal(e,
                exceptionMappingService.processException(e),
                new HttpHeaders(),
                status,
                request);
    }

    /**
     * Answers a malformed JSON request body with a sanitized message (e.g. an invalid enum value)
     * instead of leaking the framework's {@code ProblemDetail} text.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        log.debug(e.getMessage(), e);
        var body = BaseError.builder()
                .code(exceptionMappingService.buildDefaultErrorCode(status))
                .message(describeJsonError(e.getCause()))
                .build();
        return handleExceptionInternal(e, body, new HttpHeaders(), status, request);
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
                    .code(exceptionMappingService.buildDefaultErrorCode(status))
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
        return new ResponseEntity<>(exceptionMappingService.processException(ex), handledEx.getHeaders(), handledEx.getStatusCode());
    }

    /**
     * Builds a sanitized, user-friendly message for a malformed JSON request body. Raw Jackson
     * messages are deliberately discarded — they expose internal class names, package paths and
     * framework details. Mirrors the rule services' {@code JsonProcessingExceptionMapper}.
     */
    static String describeJsonError(Throwable cause) {
        if (cause instanceof UnrecognizedPropertyException upe) {
            var field = formatPath(upe.getPath());
            return field.isEmpty() ? "Unknown field in request" : "Unknown field '%s'".formatted(field);
        }
        if (cause instanceof InvalidFormatException ife) {
            var field = formatPath(ife.getPath());
            var typeName = friendlyType(ife.getTargetType());
            return field.isEmpty() ? "Invalid %s format".formatted(typeName)
                    : "Invalid %s format for field '%s'".formatted(typeName, field);
        }
        if (cause instanceof JsonMappingException jme) {
            var field = formatPath(jme.getPath());
            return field.isEmpty() ? "Invalid request body" : "Invalid value for field '%s'".formatted(field);
        }
        return "Request body is malformed";
    }

    private static String formatPath(List<JsonMappingException.Reference> path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        var sb = new StringBuilder();
        for (var ref : path) {
            var name = ref.getFieldName();
            if (name != null) {
                if (!sb.isEmpty()) {
                    sb.append('.');
                }
                sb.append(name);
            } else if (ref.getIndex() >= 0) {
                sb.append('[').append(ref.getIndex()).append(']');
            }
        }
        return sb.toString();
    }

    private static String friendlyType(Class<?> type) {
        return switch (type) {
            case Class<?> t when Date.class.isAssignableFrom(t) || t.getName().startsWith("java.time.") -> "date";
            case Class<?> t when t == Boolean.class || t == boolean.class -> "boolean";
            case Class<?> t when Number.class.isAssignableFrom(t) || t.isPrimitive() && t != char.class && t != void.class ->
                    "number";
            case Class<?> t when CharSequence.class.isAssignableFrom(t) || t == char.class || t == Character.class ->
                    "string";
            case Class<?> t when t.isEnum() -> "enum";
            case null, default -> "value";
        };
    }

}
