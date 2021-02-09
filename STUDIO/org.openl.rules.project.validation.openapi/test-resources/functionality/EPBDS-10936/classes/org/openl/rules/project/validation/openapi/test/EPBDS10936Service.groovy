package org.openl.rules.project.validation.openapi.test

import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces

interface EPBDS10935Service {
    @POST
    @Path(value = "/getNewForUsedRate")
    @Consumes(value = [ "text/plain" ])
    @Produces(value = [ "text/plain" ])
    double getNewForUsedRate(String var1);
}

