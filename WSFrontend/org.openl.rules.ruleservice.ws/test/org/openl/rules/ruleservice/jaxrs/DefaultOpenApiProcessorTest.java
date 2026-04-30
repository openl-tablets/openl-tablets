package org.openl.rules.ruleservice.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import jakarta.ws.rs.core.UriInfo;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

class DefaultOpenApiProcessorTest {

    @Test
    void loadsDefaultSchemaAndPopulatesTitleAndServer() {
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://example.com/api/myService/openapi.json"));

        var processor = new DefaultOpenApiProcessor(Json.mapper().copy().setDefaultMergeable(true), "myService");

        OpenAPI result = processor.apply(uriInfo);

        assertNotNull(result);
        assertNotNull(result.getInfo(), "default schema must contain an info section");
        assertEquals("myService", result.getInfo().getTitle());
        assertNotNull(result.getServers());
        assertEquals(1, result.getServers().size());
        assertEquals("http://example.com/api/myService", result.getServers().get(0).getUrl());
    }

    @Test
    void serverUrlStripsTrailingPathSegment() {
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://h/x/openapi.yaml"));

        var processor = new DefaultOpenApiProcessor(Json.mapper().copy().setDefaultMergeable(true), "svc");
        OpenAPI result = processor.apply(uriInfo);

        assertEquals("svc", result.getInfo().getTitle());
        assertEquals("http://h/x", result.getServers().get(0).getUrl());
    }
}
