package org.openl.rules.ruleservice.jaxrs;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.commons.lang3.StringUtils;

import org.openl.rules.openapi.OpenAPIConfiguration;
import org.openl.rules.ruleservice.core.OpenLService;

/**
 * A REST service for providing OpenAPI schema for each OpenL service independently.
 *
 * @author Yury Molchan
 */
@Path("/openapi.{type:json|yaml}")
public class OpenApiResource {

    private final Class<?> app;
    private final ObjectMapper mapper;
    private SoftReference<String> jsonApi; // Cached OpenAPI schema in JSON format
    private SoftReference<String> yamlApi; // Cached OpenAPI schema in YAML format
    private final boolean authenticationEnabled;
    private final OpenLService service;

    OpenApiResource(Class<?> app, ObjectMapper mapper, OpenLService service, boolean authenticationEnabled) {
        this.app = app;
        this.mapper = mapper;
        this.service = service;
        this.authenticationEnabled = authenticationEnabled;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/yaml"})
    @Operation(hidden = true)
    public Response getOpenAPI(@Context UriInfo uriInfo, @PathParam("type") String type) throws Exception {
        if (StringUtils.isNotBlank(type) && type.trim().equalsIgnoreCase("yaml")) {
            var openAPI = yamlApi != null ? yamlApi.get() : null;
            if (openAPI == null) {
                openAPI = Yaml.mapper().writeValueAsString(getOpenAPI(uriInfo));
                yamlApi = new SoftReference<>(openAPI);
            }
            return Response.status(Response.Status.OK).entity(openAPI).type("application/yaml").build();
        } else {
            var openAPI = jsonApi != null ? jsonApi.get() : null;
            if (openAPI == null) {
                openAPI = Json.mapper().writeValueAsString(getOpenAPI(uriInfo));
                jsonApi = new SoftReference<>(openAPI);
            }
            return Response.status(Response.Status.OK).entity(openAPI).type(MediaType.APPLICATION_JSON_TYPE).build();
        }
    }

    private OpenAPI getOpenAPI(UriInfo uriInfo) throws IOException {
        // Load default configuration
        ObjectMapper openApiMapper = Json.mapper().copy().setDefaultMergeable(true);
        var openAPI = openApiMapper.readValue(getClass().getResource("/openapi-default.json"), OpenAPI.class);
        openAPI.getInfo().setTitle(service.getName());
        var servers = new ArrayList<Server>();
        servers.add(new Server().url(StringUtils.substringBeforeLast(uriInfo.getRequestUri().toString(), "/")));
        openAPI.setServers(servers);

        // Load custom override configuration
        // https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Integration-and-Configuration#configuration
        var custom = getClass().getResource("/openapi-configuration.json");
        if (custom != null) {
            var res = openApiMapper.readerForUpdating(new ConfigWrapper(openAPI)).readValue(custom);
            openAPI = ((ConfigWrapper) res).openAPI;
        }
        if (authenticationEnabled) {
            openAPI = openApiMapper.readerForUpdating(openAPI).readValue(getClass().getResource("/openapi-security.json"));
        }

        openAPI = OpenAPIConfiguration.generateOpenAPI(openAPI, app, mapper);
        return openAPI;
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConfigWrapper {
        public final OpenAPI openAPI;

        public ConfigWrapper(OpenAPI openAPI) {
            this.openAPI = openAPI;
        }
    }
}
