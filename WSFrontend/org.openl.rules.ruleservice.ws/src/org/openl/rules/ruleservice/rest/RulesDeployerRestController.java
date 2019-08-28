package org.openl.rules.ruleservice.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.openl.rules.repository.api.FileItem;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.deployer.RulesDeployerService;
import org.openl.rules.ruleservice.publish.RuleServiceManager;

/**
 * REST endpoint to deploy OpenL rules to the Web Service
 *
 * @author Vladyslav Pikus
 */
@Produces("application/json")
public class RulesDeployerRestController {

    private final RulesDeployerService rulesDeployerService;
    private final RuleServiceManager ruleServiceManager;

    public RulesDeployerRestController(RulesDeployerService rulesDeployerService,
            RuleServiceManager ruleServiceManager) {
        this.rulesDeployerService = rulesDeployerService;
        this.ruleServiceManager = ruleServiceManager;
    }

    /**
     * Deploys target zip input stream
     */
    @POST
    @Path("/deploy")
    @Consumes("application/zip")
    public Response deploy(@Context HttpServletRequest request) throws Exception {
        rulesDeployerService.deploy(request.getInputStream(), false);
        return Response.status(Status.CREATED).build();
    }

    /**
     * Redeploys target zip input stream
     */
    @PUT
    @Path("/deploy")
    @Consumes("application/zip")
    public Response redeploy(@Context HttpServletRequest request) throws Exception {
        rulesDeployerService.deploy(request.getInputStream(), true);
        return Response.status(Status.CREATED).build();
    }

    /**
     * Read a file by the given path name.
     *
     * @return the file descriptor.
     * @throws IOException if not possible to read the file.
     */
    @GET
    @Path("/read/{serviceName}")
    @Produces("application/zip")
    public Response read(@PathParam("serviceName") final String serviceName) throws Exception {
        OpenLService service = ruleServiceManager.getServiceByName(serviceName);
        FileItem fileItem = rulesDeployerService.read(service.getServicePath());
        return Response.ok(fileItem.getStream())
            .header("Content-Disposition", "attachment;filename=\"" + serviceName + ".zip\"")
            .build();
    }

    /**
     * Delete a service.
     *
     * @param serviceName the name of the service to delete.
     */
    @DELETE
    @Path("/delete/{serviceName}")
    public Response delete(@PathParam("serviceName") final String serviceName) throws Exception {
        OpenLService service = ruleServiceManager.getServiceByName(serviceName);
        boolean deleted = rulesDeployerService.delete(service.getServicePath());
        return Response.status(deleted ? Response.Status.OK : Status.NOT_FOUND).build();
    }
}
