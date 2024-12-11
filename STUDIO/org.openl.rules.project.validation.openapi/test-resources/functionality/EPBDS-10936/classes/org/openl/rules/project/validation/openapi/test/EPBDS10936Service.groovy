package org.openl.rules.project.validation.openapi.test

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces

interface EPBDS10935Service {
    @POST
    @Path(value = "/getNewForUsedRate")
    @Consumes(value = ["text/plain"])
    @Produces(value = ["text/plain"])
    double getNewForUsedRate(String var1);
}

