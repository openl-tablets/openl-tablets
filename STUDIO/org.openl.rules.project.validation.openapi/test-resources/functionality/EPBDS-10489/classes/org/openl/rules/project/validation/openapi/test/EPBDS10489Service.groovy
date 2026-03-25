package org.openl.rules.project.validation.openapi.test


import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces

import org.openl.rules.ruleservice.core.interceptors.RulesType

interface EPBDS10489Service {
    @RulesType(value = "Double")
    @GET
    @Path(value = "/getpetsSimpleType")
    @Produces(value = ["application/json"])
    Object[] getpetsSimpleType();

    @RulesType(value = "Pet")
    @GET
    @Path(value = "/getpetsA")
    @Produces(value = ["application/json"])
    Object getpetsA(@PathParam(value = "id") long var1);

    @RulesType(value = "Pet")
    @POST
    @Path(value = "/getpets")
    @Produces(value = ["application/json"])
    Object[] getpets();
}

