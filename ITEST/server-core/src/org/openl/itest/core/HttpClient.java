package org.openl.itest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.openl.rules.ruleservice.databinding.JacksonObjectMapperFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriTemplateHandler;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Difference;
import org.xmlunit.diff.DifferenceEvaluator;
import org.xmlunit.diff.DifferenceEvaluators;
import org.xmlunit.diff.ElementSelectors;

/**
 * A simple HTTP client which allows to send a request file and compares a response with a response file.
 * 
 * @author Yury Molchan
 */
public class HttpClient {

    private RestTemplate rest;

    private HttpClient(RestTemplate rest) {
        this.rest = rest;
    }

    static HttpClient create(String baseURI) {
        RestTemplate rest = new RestTemplate();
        for (HttpMessageConverter<?> converter : rest.getMessageConverters()) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                JacksonObjectMapperFactoryBean objectMapperFactory = new JacksonObjectMapperFactoryBean();
                objectMapperFactory.setSupportVariations(true);
                ((MappingJackson2HttpMessageConverter) converter)
                    .setObjectMapper(objectMapperFactory.createJacksonObjectMapper());
            }
        }

        DefaultUriTemplateHandler uriHandler = new DefaultUriTemplateHandler();
        uriHandler.setBaseUrl(baseURI);
        rest.setUriTemplateHandler(uriHandler);
        rest.setErrorHandler(NO_ERROR_HANDLER);
        return new HttpClient(rest);
    }

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

    private static String getMediaType(String path) {
        if (path == null) {
            return null;
        }
        switch (path.substring(path.lastIndexOf('.') + 1)) {
            case "zip":
                return "application/zip";
            case "json":
                return "application/json";
            case "xml":
                return "application/xml";
            case "txt":
                return "application/json"; // FIXME: EPBDS-8931 text/plain;
            default:
                return null;
        }
    }

    private static HttpEntity<?> file(String requestFile, String responseFile) {
        return new HttpEntity<>(requestFile != null ? new ClassPathResource(requestFile) : null,
            getHeaders(requestFile, responseFile));
    }

    private static HttpHeaders getHeaders(String requestFile, String responseFile) {
        String contentType = getMediaType(requestFile);
        String accept = getMediaType(responseFile);
        HttpHeaders headers = new HttpHeaders();
        if (contentType != null) {
            headers.set(HttpHeaders.CONTENT_TYPE, contentType);
        }
        if (accept != null) {
            headers.set(HttpHeaders.ACCEPT, accept);
        } else {
            headers.set(HttpHeaders.ACCEPT, "application/xml;q=0.9, application/json;q=1.0, */*;q=0.8");
        }
        return headers;
    }

    public void get(String url, String responseFile) {
        send(HttpMethod.GET, url, null, 200, responseFile);
    }

    public void get(String url, int status) {
        send(HttpMethod.GET, url, null, status, null);
    }

    public <T> T get(String url, Class<T> clazz) {
        return request(HttpMethod.GET, url, null, 200, clazz);
    }

    public void post(String url, String requestFile, String responseFile) {
        send(HttpMethod.POST, url, requestFile, 200, responseFile);
    }

    public void post(String url, String requestFile, int status) {
        send(HttpMethod.POST, url, requestFile, status, null);
    }

    public void post(String url, String requestFile, int status, String responseFile) {
        send(HttpMethod.POST, url, requestFile, status, responseFile);
    }

    public <T> T post(String url, String requestFile, Class<T> clazz) {
        return request(HttpMethod.POST, url, requestFile, 200, clazz);
    }

    public void put(String url, String requestFile, String responseFile) {
        send(HttpMethod.PUT, url, requestFile, 200, responseFile);
    }

    public void put(String url, String requestFile, int status) {
        send(HttpMethod.PUT, url, requestFile, status, null);
    }

    public <T> T put(String url, String requestFile, Class<T> clazz) {
        return request(HttpMethod.PUT, url, requestFile, 200, clazz);
    }

    public void delete(String url) {
        send(HttpMethod.DELETE, url, null, 200, (String) null);
    }

    private <T> T request(HttpMethod method, String url, String requestFile, int status, Class<T> clazz) {
        ResponseEntity<T> response = rest.exchange(url, method, file(requestFile, null), clazz);
        assertEquals("URL :" + url, status, response.getStatusCodeValue());
        return response.getBody();
    }

    private void send(HttpMethod method, String url, String requestFile, int status, String responseFile) {
        ResponseEntity<Resource> response = rest.exchange(url, method, file(requestFile, responseFile), Resource.class);
        assertEquals("URL :" + url, status, response.getStatusCodeValue());
        Resource body = response.getBody();
        if (responseFile == null) {
            assertNull("Expected empty body for URL :" + url, body);
        } else if (responseFile.endsWith(".xml")) {
            compareXML(responseFile, body);
        } else {
            compareBinary(responseFile, body);
        }
    }

    private void compareBinary(String responseFile, Resource body) {
        try (InputStream actual = body.getInputStream();
                InputStream file = getClass().getResourceAsStream(responseFile)) {

            int ch = file.read();
            int counter = -1;
            while (ch != -1) {
                counter++;
                assertEquals("File: [" + responseFile + "] at: " + counter, ch, actual.read());
                ch = file.read();
            }

            assertEquals("File: [" + responseFile + "] at: " + counter, -1, actual.read());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void compareXML(String responseFile, Resource body) {
        try (InputStream actual = body.getInputStream();
                InputStream file = getClass().getResourceAsStream(responseFile)) {
            DifferenceEvaluator evaluator = DifferenceEvaluators.chain(DifferenceEvaluators.Default, matchByPattern());
            Iterator<Difference> differences = DiffBuilder.compare(file)
                .withTest(actual)
                .ignoreWhitespace()
                .checkForSimilar()
                .withNodeMatcher(
                    new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes, ElementSelectors.byName))
                .withDifferenceEvaluator(evaluator)
                .build()
                .getDifferences()
                .iterator();
            if (differences.hasNext()) {
                fail("File: [" + responseFile + "]\n" + differences.next());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private DifferenceEvaluator matchByPattern() {
        return (comparison, outcome) -> {
            if (outcome == ComparisonResult.DIFFERENT) {
                Node control = comparison.getControlDetails().getTarget();
                Node test = comparison.getTestDetails().getTarget();
                if (control != null && test != null) {
                    String controlValue = control.getNodeValue();
                    String testValue = test.getNodeValue();
                    if (controlValue != null && testValue != null) {
                        String regExp = controlValue.replaceAll("\\\\", "\\\\\\\\")
                            .replaceAll("#+", "\\\\d+")
                            .replaceAll("@+", "[@\\\\w]+")
                            .replaceAll("\\*+", ".*");
                        boolean matches = Pattern.compile(regExp).matcher(testValue).matches();
                        if (matches) {
                            return ComparisonResult.SIMILAR;
                        }
                    }
                }

                return outcome;
            }
            return outcome;
        };
    }
}
