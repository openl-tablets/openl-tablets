package org.openl.rules.rest.resolver;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class MockConfiguration implements WebMvcConfigurer {

    @Autowired(required = false)
    private List<HandlerMethodArgumentResolver> argumentResolvers;

    @Bean
    public MockMvc mockMvc(WebApplicationContext webContext) {
        return MockMvcBuilders.webAppContextSetup(webContext).build();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        if (argumentResolvers != null) {
            resolvers.addAll(argumentResolvers);
        }
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
