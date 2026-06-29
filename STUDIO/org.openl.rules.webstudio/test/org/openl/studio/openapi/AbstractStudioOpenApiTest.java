package org.openl.studio.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base test that compares a generated OpenAPI document to a fixture.
 *
 * <p>Each subclass lives in an {@code appXXX} sub-package containing the controllers under test. The
 * test fetches {@code GET /openapi.json} and compares it to {@code functionality/appXXX.json} on the
 * test classpath.
 *
 * <p>Fixture strings can use wildcards so volatile fields ({@code description}, {@code operationId}…)
 * do not pin the assertion: {@code *} matches anything, {@code #} matches digits, {@code @} matches
 * word characters.
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
public abstract class AbstractStudioOpenApiTest {

    private static final String FIXTURE = "functionality/%s.json";
    private static final ObjectMapper OBJECT_MAPPER = Jackson2ObjectMapperBuilder.json().build();

    @Autowired
    private MockMvc mockMvc;

    @Test
    protected void generatesOpenApiDocument() throws Exception {
        var actual = mockMvc.perform(get("/openapi.json")).andReturn().getResponse().getContentAsString();
        var expected = loadFixture(FIXTURE.formatted(appName()));
        compareJsonObjects(OBJECT_MAPPER.readTree(expected), OBJECT_MAPPER.readTree(actual), "");
    }

    private String appName() {
        var packageName = getClass().getPackageName();
        return packageName.substring(packageName.lastIndexOf('.') + 1);
    }

    private static String loadFixture(String resource) throws Exception {
        var url = AbstractStudioOpenApiTest.class.getClassLoader().getResource(resource);
        assertNotNull(url, "fixture not found on classpath: " + resource);
        try (var stream = url.openStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void compareJsonObjects(JsonNode expected, JsonNode actual, String path) {
        if (Objects.equals(expected, actual)) {
            return;
        }
        if (expected == null || actual == null) {
            assertEquals(expected, actual, "Path: " + path);
        } else if (expected.isTextual()) {
            var regex = expected.asText()
                    .replaceAll("\\[", "\\\\[")
                    .replaceAll("]", "\\\\]")
                    .replaceAll("#+", "[#\\\\d]+")
                    .replaceAll("@+", "[@\\\\w]+")
                    .replaceAll("\\*+", "[^￿]*");
            var actualText = actual.isTextual() ? actual.asText() : actual.toString();
            if (!Pattern.compile(regex).matcher(actualText).matches()) {
                assertEquals(expected, actual, "Path: " + path);
            }
        } else if (expected.isArray() && actual.isArray()) {
            for (int i = 0; i < expected.size() || i < actual.size(); i++) {
                compareJsonObjects(expected.get(i), actual.get(i), path + "[" + i + "]");
            }
        } else if (expected.isObject() && actual.isObject()) {
            var names = new LinkedHashSet<String>();
            expected.fieldNames().forEachRemaining(names::add);
            actual.fieldNames().forEachRemaining(names::add);
            for (var name : names) {
                compareJsonObjects(expected.get(name), actual.get(name), path + " > " + name);
            }
        } else {
            assertEquals(expected, actual, "Path: " + path);
        }
    }
}
