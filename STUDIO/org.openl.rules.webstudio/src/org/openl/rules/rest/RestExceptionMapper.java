package org.openl.rules.rest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.openl.rules.rest.exception.ValidationException;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ResponseStatus;

public class RestExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = LoggerFactory.getLogger(RestExceptionMapper.class);

    @Override
    public Response toResponse(Exception e) {
        ResponseStatus status = e.getClass().getAnnotation(ResponseStatus.class);
        if (status != null) {
            HttpStatus code = status.code();
            if (e instanceof ValidationException) {
                return handleBindingResult(code, ((ValidationException) e).getBindingResult());
            } else {
                return Response.status(code.value()).entity(mapCommonException(code, e)).build();
            }
        } else {
            HttpStatus code = HttpStatus.INTERNAL_SERVER_ERROR;
            LOG.error(e.getMessage(), e);
            return Response.status(code.value()).entity(mapCommonException(code, e)).build();
        }
    }

    private Response handleBindingResult(HttpStatus status, BindingResult bindingResult) {
        Map<String, Object> dest = new LinkedHashMap<>();
        if (bindingResult.getGlobalErrorCount() == 1 && !bindingResult.hasFieldErrors()) {
            dest.put("message", bindingResult.getGlobalError().getDefaultMessage());
        } else {
            dest.put("message", status.getReasonPhrase());
            if (bindingResult.hasFieldErrors()) {
                dest.put("fields",
                    bindingResult.getFieldErrors().stream().map(this::mapFiledError).collect(Collectors.toList()));
            }
            if (bindingResult.hasGlobalErrors()) {
                dest.put("errors",
                    bindingResult.getGlobalErrors().stream().map(this::mapObjectError).collect(Collectors.toList()));
            }
        }
        return Response.status(Response.Status.BAD_REQUEST.getStatusCode()).entity(dest).build();
    }

    private Map<String, Object> mapCommonException(HttpStatus status, Exception e) {
        Map<String, Object> dest = new LinkedHashMap<>();
        dest.put("message",
            Optional.ofNullable(e.getMessage()).filter(StringUtils::isNotBlank).orElseGet(status::getReasonPhrase));
        return dest;
    }

    private Map<String, Object> mapFiledError(FieldError error) {
        Map<String, Object> dest = new LinkedHashMap<>();
        dest.put("field", error.getField());
        dest.put("rejectedValue", error.getRejectedValue());
        dest.put("message", error.getDefaultMessage());
        return dest;
    }

    private Map<String, Object> mapObjectError(ObjectError error) {
        Map<String, Object> dest = new LinkedHashMap<>();
        dest.put("message", error.getDefaultMessage());
        return dest;
    }

}