package org.openl.rules.project.validation.openapi.test;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.openl.rules.ruleservice.core.interceptors.RulesType;

/**
 * An annotation template with two operations of one path. It gives both the type {@code Plan} as the result, while
 * the rule of the POST operation returns a number.
 *
 * @author Yury Molchan
 */
public interface ServiceEPBDS11451 {

    @RulesType("Plan")
    @GET
    @Path("/Plan")
    @Produces("application/json")
    Object PlanGET(@QueryParam("name") String name);

    @RulesType("Plan")
    @POST
    @Path("/Plan")
    @Consumes("application/json")
    @Produces("application/json")
    Object PlanPOST(@RulesType("Plan") Object plan);
}
