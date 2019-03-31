package org.openl.rules.ruleservice.publish.jaxrs;

import org.openl.rules.ruleservice.publish.common.ExceptionResponseDto;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class JAXRSExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception e) {
        ExceptionResponseDto dto = ExceptionResponseDto.createFrom(e);

        JAXRSErrorResponse errorResponse = new JAXRSErrorResponse(dto.getMessage(),
            dto.getType(),
            dto.getDetail() != null ? dto.getDetail().replaceAll("\t", "    ").split(System.lineSeparator()) : null);

        return Response.status(dto.getStatusCode()).type(MediaType.APPLICATION_JSON).entity(errorResponse).build();
    }

}
