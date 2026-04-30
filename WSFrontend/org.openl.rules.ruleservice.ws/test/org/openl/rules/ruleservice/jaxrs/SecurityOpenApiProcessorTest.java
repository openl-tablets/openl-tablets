package org.openl.rules.ruleservice.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.junit.jupiter.api.Test;

class SecurityOpenApiProcessorTest {

    @Test
    void overlaysOAuth2SecuritySchemeFromBundledFile() {
        var existing = new OpenAPI();
        existing.setInfo(new Info().title("svc").version("1.0"));

        var processor = new SecurityOpenApiProcessor(Json.mapper().copy().setDefaultMergeable(true));

        OpenAPI result = processor.apply(existing);

        assertNotNull(result.getSecurity(), "security list must be populated");
        assertEquals(1, result.getSecurity().size());
        assertTrue(result.getSecurity().get(0).containsKey("OAuth2"));

        var components = result.getComponents();
        assertNotNull(components);
        var schemes = components.getSecuritySchemes();
        assertNotNull(schemes);
        assertTrue(schemes.containsKey("OAuth2"));
        assertEquals("bearer", schemes.get("OAuth2").getScheme());
        assertEquals("JWT", schemes.get("OAuth2").getBearerFormat());

        // existing info must survive — overlay merges into the supplied OpenAPI
        assertEquals("svc", result.getInfo().getTitle());
    }
}
