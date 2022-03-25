package org.openl.rules.spring.openapi.app031;

import org.openl.rules.spring.openapi.app031.exception.ForbiddenException;
import org.openl.rules.spring.openapi.app031.exception.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import io.swagger.v3.oas.annotations.responses.ApiResponse;

@ControllerAdvice
public class ErrorControllerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ApiResponse(responseCode = "404", description = "Not Found")
    public ResponseEntity<ErrorDto> handleNotFoundException(Exception e, WebRequest request) {
        return null;
    }

    @ExceptionHandler(RuntimeException.class)
    @ApiResponse(responseCode = "400", description = "Bad Request")
    public ResponseEntity<ErrorDto> handleBadRequestException(Exception e, WebRequest request) {
        return null;
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorDto> handleForbiddenException(Exception e, WebRequest request) {
        return null;
    }

}
