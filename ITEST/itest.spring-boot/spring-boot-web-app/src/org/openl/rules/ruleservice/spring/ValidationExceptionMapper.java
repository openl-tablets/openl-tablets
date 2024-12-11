package org.openl.rules.ruleservice.spring;

import java.util.Map;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.springframework.stereotype.Component;

import org.openl.rules.ruleservice.core.ExceptionType;
import org.openl.rules.ruleservice.core.RuleServiceWrapperException;

@Component
@Provider
@Priority(Priorities.USER - 1)
public class ValidationExceptionMapper implements ExceptionMapper<RuleServiceWrapperException> {
    RuleServiceExceptionMapper parent = new RuleServiceExceptionMapper();

    @Override
    public Response toResponse(RuleServiceWrapperException exception) {

        if (exception.getType() != ExceptionType.USER_ERROR) {
            return Response.status(422)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("code", exception.getType().name(), "message", exception.getMessage()))
                    .build();
        }
        return parent.toResponse(exception);
    }
}
