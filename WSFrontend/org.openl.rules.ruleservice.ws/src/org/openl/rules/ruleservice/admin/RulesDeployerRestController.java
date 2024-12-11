package org.openl.rules.ruleservice.admin;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.deployer.RulesDeployerService;
import org.openl.rules.ruleservice.loader.DeploymentsUpdatedEvent;
import org.openl.rules.ruleservice.management.ServiceManager;

/**
 * REST endpoint to deploy OpenL rules to the Web Service
 *
 * @author Vladyslav Pikus
 */
@Path("/deploy")
@Produces("application/json")
public class RulesDeployerRestController {

    @Autowired
    private RulesDeployerService rulesDeployerService;

    @Autowired
    private ServiceManager serviceManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * Deploys target zip input stream
     */
    @POST
    @Consumes("application/zip")
    @Produces("text/plain;charset=UTF-8")
    public Response deploy(@Context HttpServletRequest request) throws Exception {
        try {
            rulesDeployerService.deploy(request.getInputStream(), true);
            notifyDeploymentsUpdated();
            return Response.status(Status.CREATED).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * Redeploys target zip input stream
     */
    @POST
    @Path("/{deployPath:.+}")
    @Consumes("application/zip")
    @Produces("text/plain;charset=UTF-8")
    public Response deploy(@PathParam("deployPath") final String deployPath,
                           @Context HttpServletRequest request) throws Exception {
        try {
            rulesDeployerService.deploy(deployPath, request.getInputStream(), true);
            notifyDeploymentsUpdated();
            return Response.status(Status.CREATED).build();
        } catch (IllegalArgumentException e) {
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
        final String encodedFileName = URLEncoder.encode(deploymentName + ".zip", StandardCharsets.UTF_8.name())
                .replace("+", "%20");
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
        if (deleted) {
            notifyDeploymentsUpdated();
        }
        return Response.status(deleted ? Response.Status.OK : Status.NOT_FOUND).build();
    }

    private void notifyDeploymentsUpdated() {
        eventPublisher.publishEvent(new DeploymentsUpdatedEvent(this));
    }
}
