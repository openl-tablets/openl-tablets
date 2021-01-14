package org.openl.rules.ruleservice.rest;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.deployer.RulesDeployInputException;
import org.openl.rules.ruleservice.deployer.RulesDeployerService;
import org.openl.rules.ruleservice.management.ServiceManager;

/**
 * REST endpoint to deploy OpenL rules to the Web Service
 *
 * @author Vladyslav Pikus
 */
@Path("/deploy")
@Produces("application/json")
public class RulesDeployerRestController {

    private RulesDeployerService rulesDeployerService;
    private ServiceManager serviceManager;

    @Resource
    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    @Resource
    public void setRulesDeployerService(RulesDeployerService rulesDeployerService) {
        this.rulesDeployerService = rulesDeployerService;
    }

    /**
     * Deploys target zip input stream
     */
    @POST
    @Consumes("application/zip")
    public Response deploy(@Context HttpServletRequest request) throws Exception {
        try {
            rulesDeployerService.deploy(request.getInputStream(), true);
            return Response.status(Status.CREATED).build();
        } catch (RulesDeployInputException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * Redeploys target zip input stream
     */
    @POST
    @Path("/{serviceName:.+}")
    @Consumes("application/zip")
    public Response deploy(@PathParam("serviceName") final String serviceName,
            @Context HttpServletRequest request) throws Exception {
        try {
            rulesDeployerService.deploy(serviceName, request.getInputStream(), true);
            return Response.status(Status.CREATED).build();
        } catch (RulesDeployInputException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * Read a file by the given path name.
     *
     * @return the file descriptor.
     * @throws IOException if not possible to read the file.
     */
    @GET
    @Path("/{serviceName:.+}")
    @Produces("application/zip")
    public Response read(@PathParam("serviceName") final String serviceName) throws Exception {
        OpenLService service = serviceManager.getServiceByName(serviceName);
        if (service == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        InputStream read = rulesDeployerService.read(service.getServicePath());
        return Response.ok(read)
            .header("Content-Disposition", "attachment;filename='" + serviceName + ".zip'")
            .build();
    }

    /**
     * Delete a service.
     *
     * @param serviceName the name of the service to delete.
     */
    @DELETE
    @Path("/{serviceName:.+}")
    public Response delete(@PathParam("serviceName") final String serviceName) throws Exception {
        OpenLService service = serviceManager.getServiceByName(serviceName);
        if (service == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        boolean deleted = rulesDeployerService.delete(service.getServicePath());
        return Response.status(deleted ? Response.Status.OK : Status.NOT_FOUND).build();
    }
}
