package org.openl.rules.ruleservice.spring;

import java.util.Map;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

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
