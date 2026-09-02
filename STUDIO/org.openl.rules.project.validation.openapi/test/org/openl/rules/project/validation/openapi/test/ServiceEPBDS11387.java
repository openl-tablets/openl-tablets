package org.openl.rules.project.validation.openapi.test;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.openl.rules.ruleservice.core.interceptors.RulesType;

interface ServiceEPBDS11387 {

    @RulesType("PlanDetailsRequest")
    @POST
    @Path("/PlanDetails")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Object PlanDetailsOPTIONS(@RulesType("PlanDetailsRequest") Object arg0);

    @RulesType("PlanDetailsRequest")
    @POST
    @Path("/PlanDetails")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Object PlanDetailsHEAD(@RulesType("PlanDetailsRequest") Object arg0);

    @RulesType("PlanDetailsRequest")
    @POST
    @Path("/PlanDetails")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Object PlanDetailsPATCH(@RulesType("PlanDetailsRequest") Object arg0);

    @RulesType("PlanDetailsRequest")
    @POST
    @Path("/PlanDetails")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Object PlanDetailsDELETE(@RulesType("PlanDetailsRequest") Object arg0);

    @RulesType("PlanDetailsRequest")
    @POST
    @Path("/PlanDetails")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Object PlanDetailsPOST(@RulesType("PlanDetailsRequest") Object arg0);

    @RulesType("PlanDetailsRequest")
    @POST
    @Path("/PlanDetails")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Object PlanDetailsPUT(@RulesType("PlanDetailsRequest") Object arg0);

    @RulesType("PlanDetailsRequest")
    @GET
    @Path("/PlanDetails")
    @Produces({"application/json"})
    Object PlanDetailsGET(@QueryParam("a") int arg0,
                          @QueryParam("b") double arg1,
                          @QueryParam("c") boolean arg2);

}
