package org.openl.rules.rest.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.CustomValidatorBean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Spring WebStudio API Configuration
 *
 * @author Vladyslav Pikus
 */
@Configuration
@Import(ValidationConfiguration.class)
@EnableWebMvc
@ComponentScan(basePackages = "org.openl.rules.rest.common")
public class ApiConfig implements WebMvcConfigurer {

    // No custom argument resolvers in Wizard
    @Autowired(required = false)
    private List<HandlerMethodArgumentResolver> argumentResolvers;

    @Autowired
    @Qualifier("webstudioValidatorBean")
    private CustomValidatorBean validator;

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new ByteArrayHttpMessageConverter());
        converters.add(new StringHttpMessageConverter());
        var jacksonMessageConverter = new MappingJackson2HttpMessageConverter(objectMapper());
        jacksonMessageConverter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON));
        converters.add(jacksonMessageConverter);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        if (argumentResolvers != null) {
            resolvers.addAll(argumentResolvers);
        }
    }

    @Override
    public Validator getValidator() {
        return validator;
    }

    @Bean
    public ObjectMapper objectMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule())
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public StandardServletMultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
}
