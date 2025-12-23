package org.openl.studio.config;

import java.time.Clock;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.CustomValidatorBean;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import org.openl.rules.spring.openapi.conf.SpringMvcOpenApiConfiguration;

/**
 * Spring OpenL Studio API Configuration
 *
 * @author Vladyslav Pikus
 */
@Configuration
@Import(SpringMvcOpenApiConfiguration.class)
@EnableWebMvc
@EnableAsync
public class ApiConfig implements WebMvcConfigurer {

    // No custom argument resolvers in Wizard
    @Autowired(required = false)
    private List<HandlerMethodArgumentResolver> argumentResolvers;

    @Autowired(required = false)
    private List<Converter<?, ?>> converters;

    @Autowired
    @Qualifier("webstudioValidatorBean")
    private CustomValidatorBean validator;

    @Autowired
    private ObjectProvider<ObjectMapper> objectMapperProvider;

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new ByteArrayHttpMessageConverter());
        converters.add(new ResourceHttpMessageConverter());
        converters.add(new StringHttpMessageConverter());
        var jacksonMessageConverter = new MappingJackson2HttpMessageConverter(objectMapperProvider.getObject());
        jacksonMessageConverter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON,
                new MediaType("application", "merge-patch+json")));
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

    /**
     * This bean must be static, to be instantiated before the other post processors.
     * Otherwise, some are not instantiated.
     */
    @Bean
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static MethodValidationPostProcessor getMethodValidationPostProcessor(ApplicationContext context) {
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        ObjectProvider validatorBeanProvider = context.getBeanProvider(LocalValidatorFactoryBean.class);
        processor.setValidatorProvider((ObjectProvider<jakarta.validation.Validator>) validatorBeanProvider);
        return processor;
    }

    @Bean
    public StandardServletMultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    @Bean
    public ViewResolver internalResourceViewResolver() {
        InternalResourceViewResolver bean = new InternalResourceViewResolver();
        bean.setPrefix("/pages/");
        bean.setSuffix(".html");
        return bean;
    }

    @Bean("openApiMessageSource")
    public MessageSource openApiMessageSource() {
        var messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/openapi");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        if (converters != null) {
            converters.forEach(registry::addConverter);
        }
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
