package org.openl.rules.spring.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
public abstract class AbstractSpringOpenApiTest {

    private static final String TEST_RESOURCE = "functionality/%s.json";
    private static final ObjectMapper OBJECT_MAPPER = Jackson2ObjectMapperBuilder.json().build();

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testApp() throws Exception {
        var mockMvcResult = mockMvc.perform(get("/api-docs/openapi.json")).andExpect(status().isOk()).andReturn();
        var result = mockMvcResult.getResponse().getContentAsString();
        var expected = getResource(String.format(TEST_RESOURCE, getTestNumber()));
        assertJsonEquals(expected, result);
    }

    private String getTestNumber() {
        var packageName = getClass().getPackageName();
        return packageName.substring(packageName.lastIndexOf('.') + 1);
    }

    private static String getResource(String fileName) throws Exception {
        var resource = AbstractSpringOpenApiTest.class.getClassLoader().getResource(fileName);
        assertNotNull(resource);
        try (var input = resource.openStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void assertJsonEquals(String expectedJson, String actualJson) throws JsonProcessingException {
        compareJsonObjects(OBJECT_MAPPER.readTree(expectedJson), OBJECT_MAPPER.readTree(actualJson), "");
    }

    private static void compareJsonObjects(JsonNode expectedJson, JsonNode actualJson, String path) {
        if (Objects.equals(expectedJson, actualJson)) {
            return;
        }
        if (expectedJson == null || actualJson == null) {
            failDiff(expectedJson, actualJson, path);
        } else if (expectedJson.isTextual()) {
            // try to compare by a pattern
            String regExp = expectedJson.asText()
                    .replaceAll("\\[", "\\\\[")
                    .replaceAll("]", "\\\\]")
                    .replaceAll("#+", "[#\\\\d]+")
                    .replaceAll("@+", "[@\\\\w]+")
                    .replaceAll("\\*+", "[^\uFFFF]*");
            String actualText = actualJson.isTextual() ? actualJson.asText() : actualJson.toString();
            if (!Pattern.compile(regExp).matcher(actualText).matches()) {
                failDiff(expectedJson, actualJson, path);
            }
        } else if (expectedJson.isArray() && actualJson.isArray()) {
            for (int i = 0; i < expectedJson.size() || i < actualJson.size(); i++) {
                compareJsonObjects(expectedJson.get(i), actualJson.get(i), path + "[" + i + "]");
            }
        } else if (expectedJson.isObject() && actualJson.isObject()) {
            LinkedHashSet<String> names = new LinkedHashSet<>();
            expectedJson.fieldNames().forEachRemaining(names::add);
            actualJson.fieldNames().forEachRemaining(names::add);

            for (var name : names) {
                compareJsonObjects(expectedJson.get(name), actualJson.get(name), path + " > " + name);
            }
        } else {
            failDiff(expectedJson, actualJson, path);
        }
    }

    private static void failDiff(JsonNode expectedJson, JsonNode actualJson, String path) {
        assertEquals(expectedJson, actualJson, "Path: \\" + path);
    }
}
