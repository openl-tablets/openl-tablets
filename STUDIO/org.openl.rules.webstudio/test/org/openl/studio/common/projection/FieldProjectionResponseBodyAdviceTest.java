package org.openl.studio.common.projection;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.openl.studio.common.JsonViewControllerAdvice;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.projection.test.ProjectionTestController;
import org.openl.studio.config.ObjectMapperConfig;

/**
 * Verifies response field projection with the default lenient unknown-field behaviour.
 *
 * @author Vladyslav Pikus
 */
@SpringJUnitConfig(FieldProjectionResponseBodyAdviceTest.TestConfig.class)
@WebAppConfiguration
class FieldProjectionResponseBodyAdviceTest {

    private static final ObjectMapper READER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void fullResponseWhenNoFieldsParameter() throws Exception {
        var body = json(get("/projection-test/single"));
        assertEquals("1", body.get("id").asText());
        assertEquals("name-1", body.get("name").asText());
        assertEquals("OPENED", body.get("status").asText());
        assertEquals("login-1", body.get("owner").get("login").asText());
        // hidden fields are never serialized
        assertFalse(body.has("secret"));
        assertFalse(body.has("writeOnly"));
    }

    @Test
    void partialResponseWhenFieldsRequested() throws Exception {
        var body = json(get("/projection-test/single").param("fields", "id,name"));
        assertEquals("1", body.get("id").asText());
        assertEquals("name-1", body.get("name").asText());
        assertFalse(body.has("status"));
        assertFalse(body.has("owner"));
    }

    @Test
    void blankFieldsParameterKeepsFullResponse() throws Exception {
        var body = json(get("/projection-test/single").param("fields", "   "));
        assertEquals("1", body.get("id").asText());
        assertEquals("OPENED", body.get("status").asText());
    }

    @Test
    void unknownFieldsAreIgnoredInLenientMode() throws Exception {
        var body = json(get("/projection-test/single").param("fields", "id,unknownField"));
        assertEquals("1", body.get("id").asText());
        assertFalse(body.has("name"));
        assertFalse(body.has("unknownField"));
    }

    @Test
    void projectionCannotExposeIgnoredFields() throws Exception {
        var body = json(get("/projection-test/single").param("fields", "id,secret,writeOnly"));
        assertEquals("1", body.get("id").asText());
        assertFalse(body.has("secret"));
        assertFalse(body.has("writeOnly"));
    }

    @Test
    void nestedObjectWithoutSubSelectionIsReturnedWhole() throws Exception {
        // a nested object selected without (...) keeps all of its fields
        var body = json(get("/projection-test/single").param("fields", "id,owner"));
        assertEquals("1", body.get("id").asText());
        assertFalse(body.has("status"));
        assertEquals("login-1", body.get("owner").get("login").asText());
        assertEquals("1@example.com", body.get("owner").get("email").asText());
    }

    @Test
    void nestedObjectIsProjectedRecursively() throws Exception {
        var body = json(get("/projection-test/single").param("fields", "id,owner(login)"));
        assertEquals("1", body.get("id").asText());
        assertFalse(body.has("name"));
        assertEquals("login-1", body.get("owner").get("login").asText());
        assertFalse(body.get("owner").has("email"));
    }

    @Test
    void nestedArrayOfObjectsIsProjectedPerElement() throws Exception {
        var body = json(get("/projection-test/single").param("fields", "name,members(email)"));
        assertEquals("name-1", body.get("name").asText());
        assertFalse(body.has("id"));
        var member = body.get("members").get(0);
        assertEquals("member-1@example.com", member.get("email").asText());
        assertFalse(member.has("login"));
    }

    @Test
    void multipleNestedSelectionsAreIndependent() throws Exception {
        var body = json(get("/projection-test/single").param("fields", "owner(email),members(login)"));
        assertFalse(body.has("id"));
        assertFalse(body.get("owner").has("login"));
        assertEquals("1@example.com", body.get("owner").get("email").asText());
        var member = body.get("members").get(0);
        assertEquals("member-1", member.get("login").asText());
        assertFalse(member.has("email"));
    }

    @Test
    void nestedSelectionWorksThroughPageWrapper() throws Exception {
        var body = json(get("/projection-test/page").param("fields", "id,owner(login)"));
        assertEquals(10, body.get("pageSize").asInt());
        var first = body.get("content").get(0);
        assertEquals("1", first.get("id").asText());
        assertFalse(first.has("name"));
        assertEquals("login-1", first.get("owner").get("login").asText());
        assertFalse(first.get("owner").has("email"));
    }

    @Test
    void listResponseIsProjectedPerElement() throws Exception {
        var body = json(get("/projection-test/list").param("fields", "id"));
        assertTrue(body.isArray());
        assertEquals("1", body.get(0).get("id").asText());
        assertFalse(body.get(0).has("name"));
        assertEquals("2", body.get(1).get("id").asText());
        assertFalse(body.get(1).has("name"));
    }

    @Test
    void setResponseIsProjectedPerElement() throws Exception {
        var body = json(get("/projection-test/set").param("fields", "id"));
        assertTrue(body.isArray());
        assertTrue(body.get(0).has("id"));
        assertFalse(body.get(0).has("name"));
    }

    @Test
    void paginatedResponseProjectsContentButKeepsMetadata() throws Exception {
        var body = json(get("/projection-test/page").param("fields", "id,name"));
        // pagination metadata is preserved
        assertEquals(0, body.get("pageNumber").asInt());
        assertEquals(10, body.get("pageSize").asInt());
        assertEquals(2, body.get("numberOfElements").asInt());
        assertEquals(2, body.get("total").asInt());
        // each element is projected
        var first = body.get("content").get(0);
        assertEquals("1", first.get("id").asText());
        assertEquals("name-1", first.get("name").asText());
        assertFalse(first.has("status"));
    }

    @Test
    void plainTextResponseIsUnaffected() throws Exception {
        var response = mockMvc.perform(get("/projection-test/text").param("fields", "id")).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().contains(MediaType.TEXT_PLAIN_VALUE));
        assertEquals("plain text", response.getContentAsString());
    }

    @Test
    void binaryResponseIsUnaffected() throws Exception {
        var response = mockMvc.perform(get("/projection-test/bytes").param("fields", "id")).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertArrayEquals("binary".getBytes(StandardCharsets.UTF_8), response.getContentAsByteArray());
    }

    @Test
    void emptyListResponseIsUnchangedByProjection() throws Exception {
        var body = json(get("/projection-test/list-empty").param("fields", "id"));
        assertTrue(body.isArray());
        assertEquals(0, body.size());
    }

    @Test
    void activeJsonViewAloneRestrictsResponseWithoutProjection() throws Exception {
        // Class-level @JsonView(Public) on the controller -> JsonViewControllerAdvice applies the view.
        // 'status' is @JsonView(Full) only, so it is excluded by the active view.
        var body = json(get("/viewed/single"));
        assertEquals("1", body.get("id").asText());
        assertEquals("name-1", body.get("name").asText());
        assertFalse(body.has("status"));
    }

    @Test
    void projectionIntersectsWithActiveJsonView() throws Exception {
        // View keeps {id, name}; projection further restricts to {id}.
        var body = json(get("/viewed/single").param("fields", "id"));
        assertEquals("1", body.get("id").asText());
        assertFalse(body.has("name"));
        assertFalse(body.has("status"));
    }

    @Test
    void projectionCannotExposeFieldHiddenByActiveJsonView() throws Exception {
        // 'status' is excluded by the Public view; selecting it cannot bypass the view.
        var body = json(get("/viewed/single").param("fields", "id,status"));
        assertEquals("1", body.get("id").asText());
        assertFalse(body.has("status"));
    }

    @Test
    void rejectsExcessivelyLongFieldsParameter() throws Exception {
        // > MAX_RAW_LENGTH (4096) characters of client-controlled input -> 400, no parse work.
        var huge = "x," + "y,".repeat(3000);
        var result = mockMvc.perform(get("/projection-test/single").param("fields", huge)).andReturn();
        assertEquals(400, result.getResponse().getStatus());
        assertInstanceOf(BadRequestException.class, result.getResolvedException());
        assertEquals("openl.error.400.fields.too.large.message",
                ((BadRequestException) result.getResolvedException()).getErrorCode());
    }

    @Test
    void rejectsExcessivelyDeepNesting() throws Exception {
        // 20 levels of '(' > MAX_DEPTH (16) -> 400 before unbounded tree growth.
        var deep = "x";
        for (int i = 0; i < 20; i++) {
            deep = "a(" + deep + ")";
        }
        var result = mockMvc.perform(get("/projection-test/single").param("fields", deep)).andReturn();
        assertEquals(400, result.getResponse().getStatus());
        assertInstanceOf(BadRequestException.class, result.getResolvedException());
    }

    @Test
    void rejectsTooManyFields() throws Exception {
        // 300 fields > MAX_NODES (256) -> 400 before tree-walk validation runs.
        var many = IntStream.range(0, 300).mapToObj(i -> "f" + i).collect(Collectors.joining(","));
        var result = mockMvc.perform(get("/projection-test/single").param("fields", many)).andReturn();
        assertEquals(400, result.getResponse().getStatus());
        assertInstanceOf(BadRequestException.class, result.getResolvedException());
    }

    @Test
    void rejectsUnmatchedClosingParen() throws Exception {
        var result = mockMvc.perform(get("/projection-test/single").param("fields", "id,name)")).andReturn();
        assertEquals(400, result.getResponse().getStatus());
        var error = (BadRequestException) result.getResolvedException();
        assertEquals("openl.error.400.fields.malformed.message", error.getErrorCode());
    }

    @Test
    void rejectsUnclosedOpeningParen() throws Exception {
        var result = mockMvc.perform(get("/projection-test/single").param("fields", "id,owner(login")).andReturn();
        assertEquals(400, result.getResponse().getStatus());
        var error = (BadRequestException) result.getResolvedException();
        assertEquals("openl.error.400.fields.malformed.message", error.getErrorCode());
    }

    @Test
    void rejectsGroupWithoutPrecedingName() throws Exception {
        var result = mockMvc.perform(get("/projection-test/single").param("fields", "id,(name)")).andReturn();
        assertEquals(400, result.getResponse().getStatus());
        var error = (BadRequestException) result.getResolvedException();
        assertEquals("openl.error.400.fields.malformed.message", error.getErrorCode());
    }

    @Test
    void acceptsEmptyFieldsBetweenCommas() throws Exception {
        // cosmetic noise is forgiven
        var body = json(get("/projection-test/single").param("fields", "id,,name"));
        assertEquals("1", body.get("id").asText());
        assertEquals("name-1", body.get("name").asText());
    }

    @Test
    void acceptsTrailingComma() throws Exception {
        var body = json(get("/projection-test/single").param("fields", "id,name,"));
        assertEquals("1", body.get("id").asText());
        assertEquals("name-1", body.get("name").asText());
        assertFalse(body.has("status"));
    }

    @Test
    void mergesRepeatedNestedSelections() throws Exception {
        // 'owner(login),owner(email)' merges via getOrAdd into a single owner{login,email} subtree.
        var body = json(get("/projection-test/single").param("fields", "owner(login),owner(email)"));
        var owner = body.get("owner");
        assertEquals("login-1", owner.get("login").asText());
        assertEquals("1@example.com", owner.get("email").asText());
    }

    private JsonNode json(RequestBuilder request) throws Exception {
        var result = mockMvc.perform(request).andReturn();
        if (result.getResolvedException() != null) {
            throw result.getResolvedException();
        }
        assertEquals(200, result.getResponse().getStatus());
        return READER.readTree(result.getResponse().getContentAsString());
    }

    @Configuration
    @EnableWebMvc
    @ComponentScan(basePackageClasses = ProjectionTestController.class)
    static class TestConfig implements WebMvcConfigurer {

        @Autowired
        private ObjectProvider<ObjectMapper> objectMapperProvider;

        @Bean
        FieldProjectionSupport fieldProjectionSupport() {
            return new FieldProjectionSupport();
        }

        @Bean
        ObjectMapper objectMapper(FieldProjectionSupport support) {
            return new ObjectMapperConfig().objectMapper(support);
        }

        @Bean
        FieldProjectionResponseBodyAdvice fieldProjectionResponseBodyAdvice(FieldProjectionSupport support) {
            return new FieldProjectionResponseBodyAdvice(support);
        }

        @Bean
        JsonViewControllerAdvice jsonViewControllerAdvice() {
            return new JsonViewControllerAdvice();
        }

        @Bean
        MockMvc mockMvc(WebApplicationContext context) {
            return MockMvcBuilders.webAppContextSetup(context).build();
        }

        @Override
        public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
            converters.add(new ByteArrayHttpMessageConverter());
            converters.add(new StringHttpMessageConverter());
            var jacksonConverter = new MappingJackson2HttpMessageConverter(objectMapperProvider.getObject());
            jacksonConverter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON));
            converters.add(jacksonConverter);
        }
    }
}
