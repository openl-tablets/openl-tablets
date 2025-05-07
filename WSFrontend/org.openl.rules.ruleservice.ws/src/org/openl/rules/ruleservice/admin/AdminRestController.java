package org.openl.rules.ruleservice.admin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.openl.info.OpenLVersion;
import org.openl.info.SysInfo;
import org.openl.rules.ruleservice.loader.DeployClasspathJarsBean;
import org.openl.rules.ruleservice.servlet.ServiceInfo;
import org.openl.rules.ruleservice.servlet.ServiceInfoProvider;
import org.openl.spring.env.DefaultPropertySource;

@Produces(MediaType.APPLICATION_JSON)
public class AdminRestController {

    @Autowired
    private DeployClasspathJarsBean deployClasspathJarService;

    @Autowired
    private ServiceInfoProvider serviceManager;

    @Value("#{uiConfig}")
    private Map<String, Object> uiConfig;

    /**
     * @return a list of descriptions of published OpenL services.
     */
    @GET
    @Path("/services")
    public Response getServiceInfo() {
        return Response.ok(serviceManager.getServicesInfo()).build();
    }

    /**
     * @return a list of descriptions of published OpenL services with serverSettings.
     */
    @GET
    @Path("/ui/info")
    public Response getServiceInfoWithSettings() {
        Map<String, Object> info = new HashMap<>(uiConfig);
        info.put("services", serviceManager.getServicesInfo());
        return Response.ok(info).build();
    }

    /**
     * @return a list of JVM metrics for monitoring purposes.
     */
    @GET
    @Path("/info/sys.json")
    public Response getSysInfo() {
        return Response.ok(SysInfo.get()).build();
    }

    /**
     * @return a list of properties about the OpenL build.
     */
    @GET
    @Path("/info/openl.json")
    public Response getOpenLInfo() {
        return Response.ok(OpenLVersion.get()).build();
    }

    /**
     * @return a list of messages of the given OpenL service.
     */
    @GET
    @Path("/services/{deployPath:.+}/errors/")
    public Response getServiceErrors(@PathParam("deployPath") final String deployPath) {
        return okOrNotFound(serviceManager.getServiceErrors(deployPath));
    }

    @GET
    @Path("/services/{deployPath:.+}/MANIFEST.MF")
    public Response getManifest(@PathParam("deployPath") final String deployPath) {
        return okOrNotFound(serviceManager.getManifest(deployPath));
    }

    @GET
    @Path("/healthcheck/readiness")
    public Response readiness() {
        if (!deployClasspathJarService.isDone()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        }
        Collection<ServiceInfo> servicesInfo = serviceManager.getServicesInfo();
        if (servicesInfo.isEmpty()) {
            return serviceManager.isReady() ? Response.ok("EMPTY", MediaType.TEXT_PLAIN_TYPE).build() : Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        }
        boolean anyFailed = servicesInfo.stream()
                .anyMatch(info -> ServiceInfo.ServiceStatus.FAILED.equals(info.getStatus()));

        return anyFailed ? Response.status(Response.Status.SERVICE_UNAVAILABLE).build() : Response.ok("READY", MediaType.TEXT_PLAIN_TYPE).build();
    }

    @GET
    @Path("/healthcheck/startup")
    public Response startup() {
        return Response.ok("UP", MediaType.TEXT_PLAIN_TYPE).build();
    }

    @GET
    @Path("/swagger-ui.json")
    public Response swaggerUIConfig() {
        var urls = serviceManager.getServicesInfo().stream()
                .filter(k -> getRestUrl(k) != null)
                .map(k -> Map.of("name", k.getName(), "url", getRestUrl(k) + "/openapi.json"))
                .collect(Collectors.toList());
        return Response.ok(Map.of("urls", urls)).build();
    }

    private String getRestUrl(ServiceInfo k) {
        return k.getUrls().get("RESTFUL");
    }

    private static Response okOrNotFound(Object entity) {
        return Response.status(entity == null ? Response.Status.NOT_FOUND : Response.Status.OK).entity(entity).build();
    }

    @GET
    @Path("/info/build.json")
    public Response getBuildInfo() {
        return Response.ok(OpenLVersion.getBuildInfo()).build();
    }

    @GET
    @Path("/config/application.properties")
    @Produces("text/plain;charset=UTF-8") // Because of the source code is encoded as UTF-8
    public Response getApplicationProperties() {
        return Response.ok((StreamingOutput) DefaultPropertySource::transferAllOpenLDefaultProperties).build();
    }
}
