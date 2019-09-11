package org.openl.itest.core;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.openl.rules.ruleservice.databinding.JacksonObjectMapperFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriTemplateHandler;
import org.springframework.web.util.UriTemplateHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Vladyslav Pikus, Yury Molchan
 */
public class RestClientFactory {

    private static final ResponseErrorHandler NO_ERROR_HANDLER = new ResponseErrorHandler() {
        @Override
        public boolean hasError(ClientHttpResponse response) {
            return false;
        }

        @Override
        public void handleError(ClientHttpResponse response) {
            // To prevent exception throwing and to provide ability to check error codes
        }
    };

    private static final HttpHeaders JSON_HEADERS;
    static {
        JSON_HEADERS = new HttpHeaders();
        JSON_HEADERS.setContentType(MediaType.APPLICATION_JSON);
        JSON_HEADERS.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    }
    private static final HttpHeaders ZIP_HEADERS;
    static {
        ZIP_HEADERS = new HttpHeaders();
        ZIP_HEADERS.set(HttpHeaders.CONTENT_TYPE, "application/zip");
        ZIP_HEADERS.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    }

    private final String address;
    private boolean supportVariations = false;

    public RestClientFactory(String address) {
        this.address = address;
    }

    public static HttpEntity<?> request(Object json) {
        return new HttpEntity<>(json, JSON_HEADERS);
    }

    public static HttpEntity<?> file(String path) {
        return new HttpEntity<>(new ClassPathResource(path), ZIP_HEADERS);
    }

    public static HttpEntity<?> json(String json) {
        return new HttpEntity<>(json.replace('`', '"'), JSON_HEADERS);
    }

    public static void assertText(String expected, String actual) {
        assertEquals(expected.replace('`', '"'), actual);
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
        rest.setErrorHandler(NO_ERROR_HANDLER);
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
