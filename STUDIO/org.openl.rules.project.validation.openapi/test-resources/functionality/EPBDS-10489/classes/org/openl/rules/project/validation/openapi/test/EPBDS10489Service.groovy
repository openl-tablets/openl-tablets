package org.openl.rules.project.validation.openapi.test

import org.openl.rules.ruleservice.core.interceptors.RulesType

import javax.ws.rs.*

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

