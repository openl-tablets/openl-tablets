package org.openl.rules.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class RestExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception e) {
        if (e instanceof SecurityException) {
            return  Response.status(Response.Status.FORBIDDEN.getStatusCode()).entity("You haven't privileges to do that.").build();
        }
        return  Response.status(Response.Status.BAD_REQUEST.getStatusCode()).entity(e.getMessage()).build();
    }

}