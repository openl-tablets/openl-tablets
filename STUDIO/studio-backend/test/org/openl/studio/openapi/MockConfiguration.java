package org.openl.studio.openapi;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.openl.rules.spring.openapi.conf.SpringMvcOpenApiConfiguration;
import org.openl.studio.common.projection.FieldProjectionSupport;

/**
 * Spring wiring for the studio OpenAPI integration tests.
 *
 * <p>Imports the generic OpenAPI generator and registers the field-projection beans, so that
 * {@link FieldProjectionOpenApiCustomizer} activates through the {@code OpenApiOperationCustomizer} SPI.
 */
@Configuration
@Import(SpringMvcOpenApiConfiguration.class)
@EnableWebMvc
public class MockConfiguration implements WebMvcConfigurer {

    @Bean
    public FieldProjectionSupport fieldProjectionSupport() {
        return new FieldProjectionSupport();
    }

    @Bean
    public FieldProjectionOpenApiCustomizer fieldProjectionOpenApiCustomizer(FieldProjectionSupport support) {
        return new FieldProjectionOpenApiCustomizer(support);
    }

    @Bean
    public MockMvc mockMvc(WebApplicationContext webContext) {
        return MockMvcBuilders.webAppContextSetup(webContext).build();
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new ByteArrayHttpMessageConverter());
        converters.add(new StringHttpMessageConverter());
        var jacksonMessageConverter = new MappingJackson2HttpMessageConverter(
                Jackson2ObjectMapperBuilder.json().build());
        jacksonMessageConverter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON));
        converters.add(jacksonMessageConverter);
    }
}
