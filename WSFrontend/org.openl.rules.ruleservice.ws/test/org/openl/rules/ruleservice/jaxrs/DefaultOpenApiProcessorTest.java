package org.openl.rules.ruleservice.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.swagger.v3.core.util.Json;
import org.junit.jupiter.api.Test;

class DefaultOpenApiProcessorTest {

    @Test
    void loadsDefaultSchemaAndPopulatesTitleAndServer() {
        var processor = new DefaultOpenApiProcessor(Json.mapper().copy().setDefaultMergeable(true), "myService");

        var result = processor.get();

        assertNotNull(result);
        assertNotNull(result.getInfo(), "default schema must contain an info section");
        assertEquals("myService", result.getInfo().getTitle());
        assertEquals(1, result.getServers().size());
        assertEquals(DefaultOpenApiProcessor.REQUEST_SERVER_URL, result.getServers().getFirst().getUrl());
    }
}
