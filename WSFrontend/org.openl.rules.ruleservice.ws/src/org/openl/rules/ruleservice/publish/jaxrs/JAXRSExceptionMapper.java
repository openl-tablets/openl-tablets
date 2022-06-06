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

        String[] details = Optional.ofNullable(dto.getDetail())
            .map(s -> s.replaceAll("\t", "    "))
            .map(s -> s.split(System.lineSeparator()))
            .orElse(null);

        JAXRSErrorResponse errorResponse;
        if (dto.getCode() != null) {
            errorResponse = new JAXRSLocalizedErrorResponse(dto
                .getMessage(), dto.getCode(), dto.getType(), details);
        } else {
            // old style error when no localization properties
            errorResponse = new JAXRSErrorResponse(dto.getMessage(), dto.getType(), details);
        }

        return Response.status(dto.getStatusCode()).type(MediaType.APPLICATION_JSON).entity(errorResponse).build();
    }

}
