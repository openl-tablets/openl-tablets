package org.openl.rules.ruleservice.publish.jaxrs;

import java.util.Optional;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.openl.rules.ruleservice.publish.common.ExceptionResponseDto;

public class JAXRSExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception e) {
        ExceptionResponseDto dto = ExceptionResponseDto.createFrom(e);

        Object errorResponse;
        if (dto.getBody() != null) {
            errorResponse = new JAXRSUserDetailedErrorResponse(dto.getBody());
        } else if (dto.getCode() != null) {
            errorResponse = new JAXRSUserErrorResponse(dto.getMessage(), dto.getCode(), dto.getType());
        } else {
            // old style error when no localization properties
            errorResponse = new JAXRSErrorResponse(dto.getMessage(), dto.getType());
        }

        return Response.status(dto.getStatusCode()).type(MediaType.APPLICATION_JSON).entity(errorResponse).build();
    }

}
