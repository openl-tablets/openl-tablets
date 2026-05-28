package org.openl.studio.common.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.projection.test.ProjectionTestController;
import org.openl.studio.config.ObjectMapperConfig;

/**
 * Verifies the configurable {@code fail-on-unknown-field=true} behaviour of response field projection.
 *
 * @author Vladyslav Pikus
 */
@SpringJUnitConfig(FieldProjectionStrictModeTest.TestConfig.class)
@WebAppConfiguration
class FieldProjectionStrictModeTest {

    private static final ObjectMapper READER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unknownFieldReturnsBadRequest() throws Exception {
        var result = mockMvc.perform(get("/projection-test/single").param("fields", "id,unknownField")).andReturn();
        assertEquals(400, result.getResponse().getStatus());
        var error = result.getResolvedException();
        assertInstanceOf(BadRequestException.class, error);
        assertEquals("openl.error.400.unknown.field.message", ((BadRequestException) error).getErrorCode());
    }

    @Test
    void requestingHiddenFieldReturnsBadRequest() throws Exception {
        // @JsonIgnore / WRITE_ONLY properties are not serializable, so they count as unknown
        var result = mockMvc.perform(get("/projection-test/single").param("fields", "id,secret")).andReturn();
        assertEquals(400, result.getResponse().getStatus());
        assertNotNull(result.getResolvedException());
    }

    @Test
    void unknownNestedFieldReturnsBadRequest() throws Exception {
        var result = mockMvc.perform(get("/projection-test/single").param("fields", "id,owner(bogusNested)")).andReturn();
        assertEquals(400, result.getResponse().getStatus());
        var error = result.getResolvedException();
        assertInstanceOf(BadRequestException.class, error);
        assertEquals("openl.error.400.unknown.field.message", ((BadRequestException) error).getErrorCode());
    }

    @Test
    void knownNestedFieldsAreProjectedSuccessfully() throws Exception {
        var result = mockMvc.perform(get("/projection-test/single").param("fields", "id,owner(login)")).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        var body = READER.readTree(result.getResponse().getContentAsString());
        assertEquals("login-1", body.get("owner").get("login").asText());
        assertFalse(body.get("owner").has("email"));
    }

    @Test
    void knownFieldsAreProjectedSuccessfully() throws Exception {
        var result = mockMvc.perform(get("/projection-test/single").param("fields", "id,name")).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        var body = READER.readTree(result.getResponse().getContentAsString());
        assertEquals("1", body.get("id").asText());
        assertEquals("name-1", body.get("name").asText());
        assertFalse(body.has("status"));
    }

    @Configuration
    @EnableWebMvc
    @ComponentScan(basePackageClasses = ProjectionTestController.class)
    static class TestConfig implements WebMvcConfigurer {

        @Autowired
        private ObjectProvider<ObjectMapper> objectMapperProvider;

        @Bean
        FieldProjectionProperties fieldProjectionProperties() {
            return new FieldProjectionProperties(true, true, "fields",
                    List.of("org.openl.studio.common.projection.test"));
        }

        @Bean
        FieldProjectionSupport fieldProjectionSupport(FieldProjectionProperties properties) {
            return new FieldProjectionSupport(properties);
        }

        @Bean
        ObjectMapper objectMapper(FieldProjectionSupport support) {
            return new ObjectMapperConfig().objectMapper(support);
        }

        @Bean
        FieldProjectionResponseBodyAdvice fieldProjectionResponseBodyAdvice(FieldProjectionSupport support,
                                                                            ObjectMapper objectMapper) {
            return new FieldProjectionResponseBodyAdvice(support, objectMapper);
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
