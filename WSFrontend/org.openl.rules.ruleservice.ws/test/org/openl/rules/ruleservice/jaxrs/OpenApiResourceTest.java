package org.openl.rules.ruleservice.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

class OpenApiResourceTest {

    private static final UriInfo INTERNAL = request("http://10.0.0.5:8080/svc/openapi.json");
    private static final UriInfo PUBLIC = request("https://api.example.com/svc/openapi.yaml?redirect=/x");

    @Test
    void theSchemaIsBuiltOnceForBothFormats() throws Exception {
        var builds = new AtomicInteger();
        var resource = new OpenApiResource(() -> {
            builds.incrementAndGet();
            return new OpenAPI().info(new Info().title("svc"));
        });

        var json = resource.getOpenAPI(INTERNAL, "json");
        var yaml = resource.getOpenAPI(INTERNAL, "yaml");
        resource.getOpenAPI(INTERNAL, "json");
        resource.getOpenAPI(INTERNAL, "yaml");

        assertEquals(1, builds.get());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, json.getMediaType());
        assertEquals("svc", Json.mapper().readTree((String) json.getEntity()).at("/info/title").asText());
        assertEquals("application/yaml", yaml.getMediaType().toString());
        assertTrue(((String) yaml.getEntity()).contains("title: svc"));
    }

    @Test
    void theServerIsTheAddressTheSchemaIsRequestedFrom() throws Exception {
        var builds = new AtomicInteger();
        var resource = new OpenApiResource(() -> {
            builds.incrementAndGet();
            return new OpenAPI().servers(List.of(new Server().url(DefaultOpenApiProcessor.REQUEST_SERVER_URL),
                    new Server().url("https://example.com/reserved")));
        });

        assertEquals(List.of("http://10.0.0.5:8080/svc", "https://example.com/reserved"),
                servers(resource.getOpenAPI(INTERNAL, "json")));
        assertEquals(List.of("https://api.example.com/svc", "https://example.com/reserved"),
                servers(resource.getOpenAPI(PUBLIC, "json")));
        var yaml = Yaml.mapper().readTree((String) resource.getOpenAPI(PUBLIC, "yaml").getEntity());
        assertEquals("https://api.example.com/svc", yaml.at("/servers/0/url").asText());
        assertEquals(List.of("http://10.0.0.5:8080/svc", "https://example.com/reserved"),
                servers(resource.getOpenAPI(INTERNAL, "json")));
        assertEquals(1, builds.get());
    }

    @Test
    void theServersReplacedByTheServiceStayAsTheyAre() throws Exception {
        var resource = new OpenApiResource(() -> new OpenAPI().servers(List.of(new Server().url("https://own.com"))));

        assertEquals(List.of("https://own.com"), servers(resource.getOpenAPI(INTERNAL, "json")));
    }

    private static List<String> servers(Response response) throws Exception {
        var servers = Json.mapper().readTree((String) response.getEntity()).path("servers");
        return servers.valueStream().map(server -> server.path("url")).map(JsonNode::asText).toList();
    }

    private static UriInfo request(String uri) {
        var requestUri = URI.create(uri);
        var uriInfo = mock(UriInfo.class);
        when(uriInfo.getRequestUri()).thenReturn(requestUri);
        when(uriInfo.getAbsolutePath()).thenReturn(URI.create(StringUtils.substringBefore(uri, "?")));
        return uriInfo;
    }
}
