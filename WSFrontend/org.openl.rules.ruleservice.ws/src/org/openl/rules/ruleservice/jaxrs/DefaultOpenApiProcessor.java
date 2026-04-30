package org.openl.rules.ruleservice.jaxrs;

import java.util.ArrayList;
import java.util.function.Function;
import jakarta.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

/**
 * Head of the OpenAPI processor chain: loads the bundled {@code openapi-default.json} schema and populates the service
 * title together with the server URL derived from the request {@link UriInfo}.
 */
final class DefaultOpenApiProcessor implements Function<UriInfo, OpenAPI> {

    private final ObjectMapper mapper;
    private final String title;

    DefaultOpenApiProcessor(ObjectMapper mapper, String title) {
        this.mapper = mapper;
        this.title = title;
    }

    @Override
    @SneakyThrows
    public OpenAPI apply(UriInfo uriInfo) {
        OpenAPI result = mapper.readValue(getClass().getResourceAsStream("/openapi-default.json"), OpenAPI.class);
        result.getInfo().setTitle(title);
        var servers = new ArrayList<Server>();
        servers.add(new Server().url(StringUtils.substringBeforeLast(uriInfo.getRequestUri().toString(), "/")));
        result.setServers(servers);
        return result;
    }
}
