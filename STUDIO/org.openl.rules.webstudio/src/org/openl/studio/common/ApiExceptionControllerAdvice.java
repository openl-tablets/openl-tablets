package org.openl.studio.common;

import java.util.Optional;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
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
public class ApiExceptionControllerAdvice extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionControllerAdvice.class);

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
            LOG.error(e.getMessage(), e);
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
        LOG.debug(e.getMessage(), e);
        var code = HttpStatus.FORBIDDEN;
        return handleExceptionInternal(e, exceptionMappingService.processException(e), new HttpHeaders(), code, request);
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
        if (causeEx instanceof RestRuntimeException ex) {
            return handleAllRestRuntimeExceptions(ex, request);
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
                exceptionMappingService.processException(e),
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

}
