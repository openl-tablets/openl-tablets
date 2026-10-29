package org.openl.rules.ruleservice.jaxrs;

import java.lang.ref.SoftReference;
import java.util.function.Supplier;
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
 * <p>The schema is built once for both formats and all requests: a big service takes seconds to describe. The server
 * of the schema is the address it is requested from, without the query, so a schema requested through another host
 * or proxy names that one.
 *
 * @author Yury Molchan
 */
@Path("/openapi.{type:json|yaml}")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class OpenApiResource {

    private static final String YAML_MEDIA_TYPE = "application/yaml";

    private final Supplier<OpenAPI> processor;
    private @Nullable SoftReference<Documents> documents;

    @GET
    @Produces({MediaType.APPLICATION_JSON, YAML_MEDIA_TYPE})
    @Operation(hidden = true)
    public Response getOpenAPI(@Context UriInfo uriInfo,
                               @Nullable @PathParam("type") String type) throws JsonProcessingException {
        var serverUrl = StringUtils.substringBeforeLast(uriInfo.getAbsolutePath().toString(), "/");
        var written = getDocuments();
        if (type != null && "yaml".equalsIgnoreCase(type.trim())) {
            return Response.ok(withServer(written.yaml(), serverUrl), YAML_MEDIA_TYPE).build();
        }
        return Response.ok(withServer(written.json(), serverUrl), MediaType.APPLICATION_JSON).build();
    }

    private synchronized Documents getDocuments() throws JsonProcessingException {
        var written = documents != null ? documents.get() : null;
        if (written == null) {
            var openAPI = processor.get();
            written = new Documents(Json.mapper().writeValueAsString(openAPI), Yaml.mapper().writeValueAsString(openAPI));
            documents = new SoftReference<>(written);
        }
        return written;
    }

    private static String withServer(String document, String serverUrl) {
        return document.replace(DefaultOpenApiProcessor.REQUEST_SERVER_URL, serverUrl);
    }

    /** The schema of the service in both formats. The server it is requested from is not named yet. */
    private record Documents(String json, String yaml) {
    }
}
