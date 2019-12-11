package org.openl.itest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Difference;
import org.xmlunit.diff.DifferenceEvaluator;
import org.xmlunit.diff.DifferenceEvaluators;
import org.xmlunit.diff.ElementSelectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A simple HTTP client which allows to send a request file and compares a response with a response file.
 * 
 * @author Yury Molchan
 */
public class HttpClient {

    private static final String ANY_BODY = "F0gupfmZFkK0RaK1NbnV";
    private static final String NO_BODY = "JhSC9dXQ1dkqZ7qHP1qZ";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RestTemplate rest;

    private HttpClient(RestTemplate rest) {
        this.rest = rest;
    }

    static HttpClient create(String baseURI) {
        RestTemplate rest = new RestTemplate();

        rest.setUriTemplateHandler(new DefaultUriBuilderFactory(baseURI));
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
                return "application/json;q=1.0, */*;q=0.1";
            case "xml":
                return "application/xml;q=1.0, */*;q=0.1";
            case "txt":
                return "text/plain;q=1.0, */*;q=0.1";
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

    public void get(String url) {
        send(HttpMethod.GET, url, null, 200, ANY_BODY);
    }

    public void get(String url, String responseFile) {
        send(HttpMethod.GET, url, null, 200, responseFile);
    }

    public void get(String url, int status) {
        send(HttpMethod.GET, url, null, status, NO_BODY);
    }

    public void get(String url, int status, String responseFile) {
        send(HttpMethod.GET, url, null, status, responseFile);
    }

    public <T> T get(String url, Class<T> clazz) {
        return request(HttpMethod.GET, url, null, 200, clazz);
    }

    public void post(String url, String requestFile, String responseFile) {
        send(HttpMethod.POST, url, requestFile, 200, responseFile);
    }

    public void post(String url, String requestFile, int status) {
        send(HttpMethod.POST, url, requestFile, status, ANY_BODY);
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
        send(HttpMethod.PUT, url, requestFile, status, NO_BODY);
    }

    public <T> T put(String url, String requestFile, Class<T> clazz) {
        return request(HttpMethod.PUT, url, requestFile, 200, clazz);
    }

    public void delete(String url) {
        send(HttpMethod.DELETE, url, null, 200, NO_BODY);
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
        switch (responseFile.substring(responseFile.lastIndexOf('.') + 1)) {
            case NO_BODY:
                assertNull("Expected empty body for URL :" + url, body);
                break;
            case ANY_BODY:
                // Skip checcking of a response body
                break;
            case "xml":
                compareXML(responseFile, body);
                break;
            case "json":
                compareJson(responseFile, body);
                break;
            default:
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
                            .replaceAll("\\s+", " ")
                            .replaceAll("#+", "\\\\d+")
                            .replaceAll("@+", "[@\\\\w]+")
                            .replaceAll("\\*+", "[^\uFFFF]*");
                        String noSpaces = testValue.replaceAll("\\s+", " ");
                        boolean matches = noSpaces
                            .equals(regExp) || Pattern.compile(regExp).matcher(noSpaces).matches();
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

    private void compareJson(String responseFile, Resource body) {
        try (InputStream actual = body.getInputStream();
                InputStream file = getClass().getResourceAsStream(responseFile)) {
            Map<String, Object> actualMap = OBJECT_MAPPER.readValue(actual, Map.class);
            Map<String, Object> expectedMap = OBJECT_MAPPER.readValue(file, Map.class);
            assertEquals(actualMap.keySet(), expectedMap.keySet());
            for (String expectedKey : expectedMap.keySet()) {
                compareJsonObjects(toJSON(expectedMap.get(expectedKey)),
                    toJSON(actualMap.get(expectedKey)));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void compareJsonObjects(String expectedJson, String actualJson) {
        try {
            Map<String, String> expectedJsonMap = splitJSON(expectedJson);
            Map<String, String> actualJsonMap = splitJSON(actualJson);
            assertEquals(expectedJsonMap.keySet(), actualJsonMap.keySet());
            if (expectedJsonMap.keySet().size() == 0) {
                String regExp = expectedJson.replaceAll("\\[", "\\\\[")
                    .replaceAll("]", "\\\\]")
                    .replaceAll("#+", "\"?\\\\d+\"?")
                    .replaceAll("\"?@+\"?", "\"?[@\\\\w]+\"?")
                    .replaceAll("\"?\\*+\"?", "[^\uFFFF]*");
                String noSpacesActual = actualJson.replaceAll("\\s+", " ");
                String noSpacesExpected = expectedJson.replaceAll("\\s+", " ");
                boolean matches = noSpacesActual
                    .equals(noSpacesExpected) || Pattern.compile(regExp).matcher(noSpacesActual).matches();
                if (!matches) {
                    fail("File: [" + expectedJson + "]\n" + noSpacesActual);
                }
            }
            for (String expectedKey : expectedJsonMap.keySet()) {
                compareJsonObjects(toJSON(expectedJsonMap.get(expectedKey)),
                    toJSON(actualJsonMap.get(expectedKey)));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String toJSON(Object obj) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(obj);
    }

    private static Map<String, String> splitJSON(String jsonString) throws IOException {
        JsonNode rootNode = OBJECT_MAPPER.readTree(jsonString);
        Map<String, String> splitMap = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = rootNode.fields();
        while (fieldsIterator.hasNext()) {
            Map.Entry<String, JsonNode> field = fieldsIterator.next();
            splitMap.put(field.getKey(), OBJECT_MAPPER.writeValueAsString(field.getValue()));
        }
        return splitMap;
    }
}
