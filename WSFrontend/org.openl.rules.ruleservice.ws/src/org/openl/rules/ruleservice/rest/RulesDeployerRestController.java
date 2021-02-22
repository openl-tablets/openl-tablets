package org.openl.rules.ruleservice.rest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.stream.Collectors;

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
import javax.ws.rs.core.StreamingOutput;

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
    @Path("/{deployPath:.+}")
    @Consumes("application/zip")
    public Response deploy(@PathParam("deployPath") final String deployPath,
            @Context HttpServletRequest request) throws Exception {
        try {
            rulesDeployerService.deploy(deployPath, request.getInputStream(), true);
            return Response.status(Status.CREATED).build();
        } catch (RulesDeployInputException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * Read a file by the given deployment name.
     *
     * @return the file descriptor.
     * @throws IOException if not possible to read the file.
     */
    @GET
    @Path("/{deploymentName}.zip")
    @Produces("application/zip")
    public Response read(@PathParam("deploymentName") final String deploymentName) throws Exception {
        Collection<OpenLService> services = serviceManager.getServicesByDeployment(deploymentName);
        if (services.isEmpty()) {
            return Response.status(Status.NOT_FOUND).build();
        }
        final String encodedFileName = URLEncoder.encode(deploymentName + ".zip", StandardCharsets.UTF_8.name());
        return Response
            .ok((StreamingOutput) outputStream -> rulesDeployerService.read(deploymentName,
                services.stream().map(OpenLService::getDeployPath).collect(Collectors.toSet()),
                outputStream))
            .header("Content-Disposition",
                "attachment; filename='" + encodedFileName + "'; filename*=UTF-8''" + encodedFileName)
            .build();
    }

    /**
     * Delete a service.
     *
     * @param deploymentName the name of the service to delete.
     */
    @DELETE
    @Path("/{deploymentName}")
    public Response delete(@PathParam("deploymentName") final String deploymentName) throws Exception {
        Collection<OpenLService> services = serviceManager.getServicesByDeployment(deploymentName);
        if (services.isEmpty()) {
            return Response.status(Status.NOT_FOUND).build();
        }
        boolean deleted = rulesDeployerService.delete(deploymentName,
            services.stream().map(OpenLService::getDeployPath).collect(Collectors.toSet()));
        return Response.status(deleted ? Response.Status.OK : Status.NOT_FOUND).build();
    }
}
