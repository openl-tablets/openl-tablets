package org.openl.rules.ruleservice.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.concurrent.atomic.AtomicInteger;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.junit.jupiter.api.Test;

class OpenApiResourceTest {

    @Test
    void theSchemaIsBuiltOnceForBothFormats() throws Exception {
        var builds = new AtomicInteger();
        var resource = new OpenApiResource(uriInfo -> {
            builds.incrementAndGet();
            return new OpenAPI().info(new Info().title("svc"));
        });
        var uriInfo = mock(UriInfo.class);

        var json = resource.getOpenAPI(uriInfo, "json");
        var yaml = resource.getOpenAPI(uriInfo, "yaml");
        resource.getOpenAPI(uriInfo, "json");
        resource.getOpenAPI(uriInfo, "yaml");

        assertEquals(1, builds.get());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, json.getMediaType());
        assertEquals("svc", Json.mapper().readTree((String) json.getEntity()).at("/info/title").asText());
        assertEquals("application/yaml", yaml.getMediaType().toString());
        assertTrue(((String) yaml.getEntity()).contains("title: svc"));
    }
}
