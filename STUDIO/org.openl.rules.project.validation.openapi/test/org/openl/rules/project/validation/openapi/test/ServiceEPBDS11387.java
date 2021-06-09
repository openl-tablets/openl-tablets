package org.openl.rules.project.validation.openapi.test;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.openl.rules.ruleservice.core.interceptors.RulesType;

interface ServiceEPBDS11387 {

    @RulesType(value = "PlanDetailsRequest")
    @POST
    @Path(value = "/PlanDetails")
    @Consumes(value = { "application/json" })
    @Produces(value = { "application/json" })
    Object PlanDetailsOPTIONS(@RulesType(value = "PlanDetailsRequest") Object arg0);

    @RulesType(value = "PlanDetailsRequest")
    @POST
    @Path(value = "/PlanDetails")
    @Consumes(value = { "application/json" })
    @Produces(value = { "application/json" })
    Object PlanDetailsHEAD(@RulesType(value = "PlanDetailsRequest") Object arg0);

    @RulesType(value = "PlanDetailsRequest")
    @POST
    @Path(value = "/PlanDetails")
    @Consumes(value = { "application/json" })
    @Produces(value = { "application/json" })
    Object PlanDetailsPATCH(@RulesType(value = "PlanDetailsRequest") Object arg0);

    @RulesType(value = "PlanDetailsRequest")
    @POST
    @Path(value = "/PlanDetails")
    @Consumes(value = { "application/json" })
    @Produces(value = { "application/json" })
    Object PlanDetailsDELETE(@RulesType(value = "PlanDetailsRequest") Object arg0);

    @RulesType(value = "PlanDetailsRequest")
    @POST
    @Path(value = "/PlanDetails")
    @Consumes(value = { "application/json" })
    @Produces(value = { "application/json" })
    Object PlanDetailsPOST(@RulesType(value = "PlanDetailsRequest") Object arg0);

    @RulesType(value = "PlanDetailsRequest")
    @POST
    @Path(value = "/PlanDetails")
    @Consumes(value = { "application/json" })
    @Produces(value = { "application/json" })
    Object PlanDetailsPUT(@RulesType(value = "PlanDetailsRequest") Object arg0);

    @RulesType(value = "PlanDetailsRequest")
    @GET
    @Path(value = "/PlanDetails")
    @Produces(value = { "application/json" })
    Object PlanDetailsGET(@QueryParam(value = "a") int arg0,
            @QueryParam(value = "b") double arg1,
            @QueryParam(value = "c") boolean arg2);

}