package org.openl.studio.mcp.handler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.stereotype.Component;

import org.openl.studio.common.ExceptionMappingService;
import org.openl.studio.common.model.ValidationError;
import org.openl.studio.mcp.exception.McpToolInvocationException;

/**
 * Exception handler that intercepts method invocations and processes exceptions using ExceptionMappingService.
 */
@Component
public class McpToolsExceptionHandler implements MethodInterceptor {

    private final ExceptionMappingService exceptionMappingService;
    private final ObjectMapper objectMapper;

    public McpToolsExceptionHandler(ExceptionMappingService exceptionMappingService, ObjectMapper objectMapper) {
        this.exceptionMappingService = exceptionMappingService;
        this.objectMapper = objectMapper;
    }

    @Nullable
    @Override
    public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
        try {
            return invocation.proceed();
        } catch (Exception ex) {
            String text;
            try {
                var error = exceptionMappingService.processException(ex);
                text = error instanceof ValidationError
                        ? objectMapper.writeValueAsString(error)
                        : error.message;
            } catch (Exception ex2) {
                ex2.addSuppressed(ex);
                throw ex2;
            }
            throw new McpToolInvocationException(text, ex);
        }
    }
}
