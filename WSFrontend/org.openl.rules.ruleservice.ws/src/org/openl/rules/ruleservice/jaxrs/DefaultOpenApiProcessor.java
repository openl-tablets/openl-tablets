package org.openl.rules.ruleservice.jaxrs;

import java.util.ArrayList;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * Head of the OpenAPI processor chain: loads the bundled {@code openapi-default.json} schema and populates the service
 * title together with the server the schema is requested from.
 *
 * <p>That server is named {@link #REQUEST_SERVER_URL}. {@link OpenApiResource} writes the address of each request in
 * its place, so the schema is built and written once for all of them.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class DefaultOpenApiProcessor implements Supplier<OpenAPI> {

    /**
     * The URL of the server the schema is requested from. It is text no schema holds otherwise: not a path template,
     * and kept as it is in both JSON and YAML.
     */
    static final String REQUEST_SERVER_URL = "urn:openl:request-server";

    private final ObjectMapper mapper;
    private final String title;

    @Override
    @SneakyThrows
    public OpenAPI get() {
        var result = mapper.readValue(getClass().getResourceAsStream("/openapi-default.json"), OpenAPI.class);
        result.getInfo().setTitle(title);
        var servers = new ArrayList<Server>();
        servers.add(new Server().url(REQUEST_SERVER_URL));
        result.setServers(servers);
        return result;
    }
}
