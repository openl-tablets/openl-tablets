package org.openl.itest.core.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openl.rules.ruleservice.databinding.JacksonObjectMapperFactoryBean;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriTemplateHandler;
import org.springframework.web.util.UriTemplateHandler;

/**
 * @author Vladyslav Pikus
 */
public class RestClientFactory {

    private final String address;
    private boolean supportVariations = false;

    public RestClientFactory(String address) {
        this.address = address;
    }

    public RestClientFactory setSupportVariations(boolean supportVariations) {
        this.supportVariations = supportVariations;
        return this;
    }

    public RestTemplate create() {
        RestTemplate rest = new RestTemplate();
        for (HttpMessageConverter<?> converter : rest.getMessageConverters()) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter jacksonConverter = (MappingJackson2HttpMessageConverter) converter;
                jacksonConverter.setObjectMapper(createObjectMapper());
            }
        }

        rest.setUriTemplateHandler(createUriHandler());
        return rest;
    }

    private ObjectMapper createObjectMapper() {
        JacksonObjectMapperFactoryBean objectMapperFactory = new JacksonObjectMapperFactoryBean();
        objectMapperFactory.setSupportVariations(supportVariations);
        return objectMapperFactory.createJacksonObjectMapper();
    }

    private UriTemplateHandler createUriHandler() {
        DefaultUriTemplateHandler uriHandler = new DefaultUriTemplateHandler();
        uriHandler.setBaseUrl(address);
        return uriHandler;
    }
}
