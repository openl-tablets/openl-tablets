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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * A REST service for providing OpenAPI schema for each OpenL service independently.
 *
 * @author Yury Molchan
 */
@Path("/openapi.{type:json|yaml}")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class OpenApiResource {

    private final Function<UriInfo, OpenAPI> processor;
    private SoftReference<OpenAPI> model; // Cached OpenAPI schema both formats are written from
    private SoftReference<String> jsonApi; // Cached OpenAPI schema in JSON format
    private SoftReference<String> yamlApi; // Cached OpenAPI schema in YAML format

    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/yaml"})
    @Operation(hidden = true)
    public Response getOpenAPI(@Context UriInfo uriInfo,
                               @Nullable @PathParam("type") String type) throws JsonProcessingException {
        if (StringUtils.isNotBlank(type) && "yaml".equalsIgnoreCase(type.trim())) {
            var openAPI = yamlApi != null ? yamlApi.get() : null;
            if (openAPI == null) {
                openAPI = Yaml.mapper().writeValueAsString(getModel(uriInfo));
                yamlApi = new SoftReference<>(openAPI);
            }
            return Response.status(Response.Status.OK).entity(openAPI).type("application/yaml").build();
        }
        var openAPI = jsonApi != null ? jsonApi.get() : null;
        if (openAPI == null) {
            openAPI = Json.mapper().writeValueAsString(getModel(uriInfo));
            jsonApi = new SoftReference<>(openAPI);
        }
        return Response.status(Response.Status.OK).entity(openAPI).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * Returns the OpenAPI schema of the service. It is built once for both formats: a big service takes seconds to
     * describe.
     */
    private OpenAPI getModel(UriInfo uriInfo) {
        var openAPI = model != null ? model.get() : null;
        if (openAPI == null) {
            openAPI = processor.apply(uriInfo);
            model = new SoftReference<>(openAPI);
        }
        return openAPI;
    }
}
