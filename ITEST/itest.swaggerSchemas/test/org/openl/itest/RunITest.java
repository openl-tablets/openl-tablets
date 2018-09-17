package org.openl.itest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.RestClientFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class RunITest {

    private static JettyServer server;
    private static String baseURI;

    private RestTemplate rest;
    private ObjectMapper objectMapper;

    @BeforeClass
    public static void setUp() throws Exception {
        server = new JettyServer();
        baseURI = server.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Before
    public void before() {
        rest = new RestClientFactory(baseURI).create();
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testSwaggerSchemaWithRuntimeContext() throws IOException {
        ResponseEntity<String> response = rest.getForEntity("/rules-with-runtime-context/swagger.json", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotBlank(response.getBody());

        JsonNode jsonNode = objectMapper.readTree(response.getBody());

        List<JsonNode> definitions = getDefinitions(jsonNode, "CalVehicleYearRequest", "CalVehicleYearRequest1", "DefaultRulesRuntimeContext");
        assertEquals(3, definitions.size());
        assertHasProperties(definitions.get(0), "modelYear", "vehEffectiveYear");
        assertHasProperties(definitions.get(1), "runtimeContext", "v");
        assertHasProperties(definitions.get(2), "currentDate", "requestDate", "lob", "nature", "usState", "country", "usRegion", "currency", "lang", "region", "caProvince", "caRegion");

        assertPropertyRef("#/definitions/DefaultRulesRuntimeContext", definitions.get(1), "runtimeContext");
        assertPropertyRef("#/definitions/CalVehicleYearRequest", definitions.get(1), "v");

        assertPostSchemaRef("#/definitions/CalVehicleYearRequest1", jsonNode, "/calVehicleYear");
    }

    @Test
    public void testSwaggerSchemaWithoutRuntimeContext() throws IOException {
        ResponseEntity<String> response = rest.getForEntity("/rules-without-runtime-context/swagger.json", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotBlank(response.getBody());

        JsonNode jsonNode = objectMapper.readTree(response.getBody());

        List<JsonNode> definitions = getDefinitions(jsonNode, "CalVehicleYearRequest", "CalVehicleYearRequest1", "SumTwoDoublesRequest1", "SumTwoDoublesRequest");
        assertEquals(4, definitions.size());
        assertHasProperties(definitions.get(0), "modelYear", "vehEffectiveYear");
        assertHasProperties(definitions.get(1), "v", "a");
        assertHasProperties(definitions.get(2), "a", "b");
        assertHasProperties(definitions.get(3), "foo");

        assertPropertyRef("#/definitions/CalVehicleYearRequest", definitions.get(1), "v");

        assertPostSchemaRef("#/definitions/CalVehicleYearRequest1", jsonNode, "/calVehicleYear");
        assertPostSchemaRef("#/definitions/SumTwoDoublesRequest1", jsonNode, "/sumTwoDoubles");
        assertPostSchemaRef("#/definitions/SumTwoDoublesRequest", jsonNode, "/bar");
    }

    private List<JsonNode> getDefinitions(JsonNode jsonNode, String... names) {
        JsonNode definition = jsonNode.get("definitions");
        List<JsonNode> res = new ArrayList<>();
        for (String name : names) {
            JsonNode node = definition.get(name);
            if (node != null) {
                res.add(node);
            }
        }
        return res;
    }

    private static void assertPostSchemaRef(String expectedRef, JsonNode node, String path) {
        assertEquals(expectedRef, node.get("paths").get(path).get("post").findValues("parameters").get(0).findValue("schema").get("$ref").asText());
    }

    private static void assertPropertyRef(String expectedRef, JsonNode node, String propName) {
        assertEquals(expectedRef, node.get("properties").get(propName).get("$ref").asText());
    }

    private static void assertHasProperties(JsonNode node, String... names) {
        JsonNode props = node.get("properties");
        for (String name : names) {
            if (!props.hasNonNull(name)) {
                fail("Property '" + name + "' must not be null");
            }
        }
    }

    private static void assertNotBlank(String s) {
        if (s == null || s.trim().isEmpty()) {
            fail("String cannot be blank");
        }
    }

}
