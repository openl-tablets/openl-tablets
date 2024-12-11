package org.openl.rules.project.validation.openapi.test


import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces

interface EPBDS10983Service {
    @GET
    @Path("/myMethod/{param1}/{param2}/{param3}")
    @Produces(["text/plain"])
    Double myMethod(@PathParam("param1") double var1,
                    @PathParam("param2") double var3,
                    @PathParam("param3") double var5);
}

