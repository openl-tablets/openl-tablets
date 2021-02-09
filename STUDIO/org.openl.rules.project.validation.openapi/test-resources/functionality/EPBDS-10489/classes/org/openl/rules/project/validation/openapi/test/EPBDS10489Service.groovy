package org.openl.rules.project.validation.openapi.test;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.openl.rules.ruleservice.core.interceptors.RulesType;

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

