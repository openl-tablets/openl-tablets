package org.openl.rules.rest.common;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.openl.rules.rest.exception.RestRuntimeException;
import org.openl.rules.rest.exception.ValidationException;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * API Exception Handler
 *
 * @author Vladyslav Pikus
 */
@ControllerAdvice
public class ApiExceptionControllerAdvice extends ResponseEntityExceptionHandler {

    private static final String DEF_ERROR_PREFIX = "openl.error.";

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionControllerAdvice.class);

    private final MessageSource messageSource;

    @Autowired
    public ApiExceptionControllerAdvice(@Qualifier("validationMessageSource") MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleAllRestRuntimeExceptions(ValidationException e, WebRequest request) {
        var code = Optional.ofNullable(AnnotationUtils.findAnnotation(e.getClass(), ResponseStatus.class))
            .map(ResponseStatus::code)
            .orElse(HttpStatus.BAD_REQUEST);
        return handleExceptionInternal(e,
            handleBindingResult(code, e.getBindingResult()),
            new HttpHeaders(),
            code,
            request);
    }

    @ExceptionHandler(RestRuntimeException.class)
    public ResponseEntity<Object> handleAllRestRuntimeExceptions(RestRuntimeException e, WebRequest request) {
        ResponseStatus status = AnnotationUtils.findAnnotation(e.getClass(), ResponseStatus.class);
        if (status != null) {
            final HttpStatus code = status.code();
            return handleExceptionInternal(e, mapCommonException(code, e), new HttpHeaders(), code, request);
        } else {
            HttpStatus code = HttpStatus.INTERNAL_SERVER_ERROR;
            LOG.error(e.getMessage(), e);
            return handleExceptionInternal(e, mapCommonException(code, e), new HttpHeaders(), code, request);
        }
    }

    @ExceptionHandler({ Exception.class, RuntimeException.class })
    public ResponseEntity<Object> handleInternalErrors(Exception e, WebRequest request) {
        LOG.error(e.getMessage(), e);
        return handleExceptionInternal(e, e.getMessage(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
            HttpHeaders headers,
            HttpStatus status,
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
            HttpStatus status,
            WebRequest request) {
        var handledEx = super.handleExceptionInternal(e, body, headers, status, request);
        if (handledEx.hasBody()) {
            var handledBody = handledEx.getBody();
            if (handledBody instanceof Map) {
                return handledEx;
            } else {
                Map<String, Object> dest = new LinkedHashMap<>();
                dest.put("message",
                    Optional.ofNullable(handledBody)
                        .map(Object::toString)
                        .filter(StringUtils::isNotBlank)
                        .orElseGet(status::getReasonPhrase));
                return new ResponseEntity<>(dest, handledEx.getHeaders(), handledEx.getStatusCode());
            }
        } else {
            Map<String, Object> dest = new LinkedHashMap<>();
            dest.put("code", buildErrorCode(status.value() + ".default.message"));
            dest.put("message", e.getMessage());
            return new ResponseEntity<>(dest, handledEx.getHeaders(), handledEx.getStatusCode());
        }
    }

    @Override
    protected ResponseEntity<Object> handleBindException(BindException e,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request) {
        var handledEx = super.handleBindException(e, headers, status, request);
        var bindingErrorModel = handleBindingResult(status, e.getBindingResult());
        return new ResponseEntity<>(bindingErrorModel, handledEx.getHeaders(), handledEx.getStatusCode());
    }

    private Map<String, Object> handleBindingResult(HttpStatus status, BindingResult bindingResult) {
        Map<String, Object> dest = new LinkedHashMap<>();
        if (bindingResult.getGlobalErrorCount() == 1 && !bindingResult.hasFieldErrors()) {
            dest.put("code", buildErrorCode(bindingResult.getGlobalError().getCode()));
            dest.put("message", resolveLocalMessage(bindingResult.getGlobalError()));
        } else {
            dest.put("message", status.getReasonPhrase());
            if (bindingResult.hasFieldErrors()) {
                dest.put("fields",
                    bindingResult.getFieldErrors()
                        .stream()
                        .sorted(Comparator.comparing(FieldError::getField, String.CASE_INSENSITIVE_ORDER))
                        .map(this::mapFiledError)
                        .collect(Collectors.toList()));
            }
            if (bindingResult.hasGlobalErrors()) {
                dest.put("errors",
                    bindingResult.getGlobalErrors()
                        .stream()
                        .sorted(Comparator.comparing(ObjectError::getCode, String.CASE_INSENSITIVE_ORDER))
                        .map(this::mapObjectError)
                        .collect(Collectors.toList()));
            }
        }
        return dest;
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
        }
        return error.getDefaultMessage();
    }

    private String resolveLocalMessage(RestRuntimeException e) {
        if (e.getErrorCode() != null) {
            try {
                return messageSource.getMessage(e.getErrorCode(), e.getArgs(), Locale.US);
            } catch (NoSuchMessageException ignored) {
            }
        }
        return e.getMessage();
    }

    private Map<String, Object> mapCommonException(HttpStatus status, Exception e) {
        Map<String, Object> dest = new LinkedHashMap<>();
        if (e instanceof RestRuntimeException) {
            RestRuntimeException restEx = (RestRuntimeException) e;
            dest.put("code", restEx.getErrorCode());
            dest.put("message", resolveLocalMessage(restEx));

        } else {
            dest.put("message",
                Optional.ofNullable(e.getMessage()).filter(StringUtils::isNotBlank).orElseGet(status::getReasonPhrase));
        }
        return dest;
    }

    private Map<String, Object> mapFiledError(FieldError error) {
        Map<String, Object> dest = new LinkedHashMap<>();
        dest.put("code", buildErrorCode(error.getCode()));
        dest.put("field", error.getField());
        dest.put("rejectedValue", error.getRejectedValue());
        dest.put("message", resolveLocalMessage(error));
        return dest;
    }

    private Map<String, Object> mapObjectError(ObjectError error) {
        Map<String, Object> dest = new LinkedHashMap<>();
        dest.put("code", buildErrorCode(error.getCode()));
        dest.put("message", resolveLocalMessage(error));
        return dest;
    }

    private static String buildErrorCode(String errorSuffix) {
        if (errorSuffix == null) {
            return null;
        }
        return DEF_ERROR_PREFIX + errorSuffix;
    }

}