package org.openl.rules.ruleservice.jaxrs;

import java.lang.ref.SoftReference;
import java.util.function.Function;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.commons.lang3.StringUtils;

/**
 * A REST service for providing OpenAPI schema for each OpenL service independently.
 *
 * @author Yury Molchan
 */
@Path("/openapi.{type:json|yaml}")
public class OpenApiResource {

    private final Function<UriInfo, OpenAPI> processor;
    private SoftReference<String> jsonApi; // Cached OpenAPI schema in JSON format
    private SoftReference<String> yamlApi; // Cached OpenAPI schema in YAML format

    OpenApiResource(Function<UriInfo, OpenAPI> processor) {
        this.processor = processor;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/yaml"})
    @Operation(hidden = true)
    public Response getOpenAPI(@Context UriInfo uriInfo, @PathParam("type") String type) throws Exception {
        if (StringUtils.isNotBlank(type) && type.trim().equalsIgnoreCase("yaml")) {
            var openAPI = yamlApi != null ? yamlApi.get() : null;
            if (openAPI == null) {
                openAPI = Yaml.mapper().writeValueAsString(processor.apply(uriInfo));
                yamlApi = new SoftReference<>(openAPI);
            }
            return Response.status(Response.Status.OK).entity(openAPI).type("application/yaml").build();
        } else {
            var openAPI = jsonApi != null ? jsonApi.get() : null;
            if (openAPI == null) {
                openAPI = Json.mapper().writeValueAsString(processor.apply(uriInfo));
                jsonApi = new SoftReference<>(openAPI);
            }
            return Response.status(Response.Status.OK).entity(openAPI).type(MediaType.APPLICATION_JSON_TYPE).build();
        }
    }
}
