package org.openl.rules.ruleservice.deployer;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * REST endpoint to deploy OpenL rules to the Web Service
 *
 * @author Vladyslav Pikus
 */
@Path("/rules")
@Produces("application/json")
public class RulesDeployerRestController {

    private final RulesDeployerService rulesDeployerService;

    public RulesDeployerRestController(RulesDeployerService rulesDeployerService) {
        this.rulesDeployerService = rulesDeployerService;
    }

    @POST
    @Path("/deploy")
    @Consumes("application/zip")
    public Response deploy(@Context HttpServletRequest request) throws Exception {
        rulesDeployerService.deploy(request.getInputStream(), false);
        return Response.status(Status.CREATED).build();
    }

    @PUT
    @Path("/deploy")
    @Consumes("application/zip")
    public Response redeploy(@Context HttpServletRequest request) throws Exception {
        rulesDeployerService.deploy(request.getInputStream(), true);
        return Response.status(Status.CREATED).build();
    }

}
