package org.openl.rules.ruleservice.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

class CustomOpenApiProcessorTest {

    private CustomOpenApiProcessor newProcessor(Path rootDir, MockEnvironment env) throws Exception {
        URL url = rootDir.toUri().toURL();
        ClassLoader cl = new URLClassLoader(new URL[]{url}, getClass().getClassLoader());
        return new CustomOpenApiProcessor(Json.mapper().copy().setDefaultMergeable(true), cl, env);
    }

    private static OpenAPI emptyOpenAPI() {
        var openAPI = new OpenAPI();
        openAPI.setInfo(new Info().title("base-title").version("base-version"));
        return openAPI;
    }

    private static void writeJson(Path dir, String name, String body) throws IOException {
        Files.writeString(dir.resolve(name), body, StandardCharsets.UTF_8);
    }

    @Test
    void noMatchingFilesReturnsInputUnchanged(@TempDir Path tmp) throws Exception {
        var processor = newProcessor(tmp, new MockEnvironment());
        OpenAPI input = emptyOpenAPI();

        OpenAPI result = processor.apply(input);

        assertEquals("base-title", result.getInfo().getTitle());
        assertEquals("base-version", result.getInfo().getVersion());
    }

    @Test
    void singleConfigurationOverridesProvidedFields(@TempDir Path tmp) throws Exception {
        writeJson(tmp, "openapi-configuration.json", """
                { "openAPI": { "info": { "title": "from-base", "version": "9.9.9" } } }
                """);

        OpenAPI result = newProcessor(tmp, new MockEnvironment()).apply(emptyOpenAPI());

        assertEquals("from-base", result.getInfo().getTitle());
        assertEquals("9.9.9", result.getInfo().getVersion());
    }

    @Test
    void baseFileIsAppliedFirstThenSuffixedVariantsInNaturalOrder(@TempDir Path tmp) throws Exception {
        // base — always applied first regardless of natural-order ASCII rules
        writeJson(tmp, "openapi-configuration.json", """
                { "openAPI": { "info": { "title": "from-base", "version": "from-base" } } }
                """);
        // zeta — natural-order later than alpha
        writeJson(tmp, "openapi-configuration-zeta.json", """
                { "openAPI": { "info": { "version": "from-zeta" } } }
                """);
        // alpha — applied between base and zeta
        writeJson(tmp, "openapi-configuration-alpha.json", """
                { "openAPI": { "info": { "version": "from-alpha" } } }
                """);

        OpenAPI result = newProcessor(tmp, new MockEnvironment()).apply(emptyOpenAPI());

        // base is applied first (sets title), then alpha, then zeta — last write wins for version
        assertEquals("from-base", result.getInfo().getTitle());
        assertEquals("from-zeta", result.getInfo().getVersion());
    }

    @Test
    void resolvesSpringPropertyPlaceholdersWithDefaults(@TempDir Path tmp) throws Exception {
        writeJson(tmp, "openapi-configuration.json", """
                {
                  "openAPI": {
                    "info": {
                      "title": "${api.title}",
                      "version": "${api.version:9.9.9}"
                    }
                  }
                }
                """);
        var env = new MockEnvironment();
        env.setProperty("api.title", "resolved-title");
        // api.version intentionally absent — should fall back to default

        OpenAPI result = newProcessor(tmp, env).apply(emptyOpenAPI());

        assertEquals("resolved-title", result.getInfo().getTitle());
        assertEquals("9.9.9", result.getInfo().getVersion());
    }

    @Test
    void mergesNewSectionsIntoExistingOpenApi(@TempDir Path tmp) throws Exception {
        writeJson(tmp, "openapi-configuration.json", """
                {
                  "openAPI": {
                    "components": {
                      "securitySchemes": {
                        "basicAuth": { "type": "http", "scheme": "basic" }
                      }
                    }
                  }
                }
                """);

        OpenAPI result = newProcessor(tmp, new MockEnvironment()).apply(emptyOpenAPI());

        // existing info must be preserved — only components added
        assertEquals("base-title", result.getInfo().getTitle());
        assertNotNull(result.getComponents());
        assertNotNull(result.getComponents().getSecuritySchemes());
        var basic = result.getComponents().getSecuritySchemes().get("basicAuth");
        assertNotNull(basic);
        assertEquals("basic", basic.getScheme());
        assertNull(result.getServers(), "no servers configured by this overlay");
    }
}
