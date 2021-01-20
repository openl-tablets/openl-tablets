package org.openl.rules.project.validation.openapi.test

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces

interface EPBDS10936Service {
    @GET
    @Path("/myMethod/{param1}/{param2}/{param3}")
    @Produces([ "text/plain" ])
    Double myMethod(@PathParam("param1") double var1,
                    @PathParam("param2") double var3,
                    @PathParam("param3") double var5);
}

