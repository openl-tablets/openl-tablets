package org.openl.itest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

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

    private final RestTemplate rest;
    private final URL baseURL;

    private HttpClient(RestTemplate rest, URL baseURL) {
        this.rest = rest;
        this.baseURL = baseURL;
    }

    static HttpClient create(URL url) {
        RestTemplate rest = new RestTemplate(getClientHttpFactory());

        rest.setUriTemplateHandler(new DefaultUriBuilderFactory(url.toExternalForm()));
        rest.setErrorHandler(NO_ERROR_HANDLER);
        return new HttpClient(rest, url);
    }

    private static ClientHttpRequestFactory getClientHttpFactory() {
        SimpleClientHttpRequestFactory httpRequestFactory = new SimpleClientHttpRequestFactory();
        int connectTimeout = Integer.parseInt(System.getProperty("http.timeout.connect"));
        int readTimeout = Integer.parseInt(System.getProperty("http.timeout.read"));
        httpRequestFactory.setConnectTimeout(connectTimeout);
        httpRequestFactory.setReadTimeout(readTimeout);
        return httpRequestFactory;
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
        try {
            switch (responseFile.substring(responseFile.lastIndexOf('.') + 1)) {
                case NO_BODY:
                    assertNull("Expected empty body for URL :" + url, body);
                    break;
                case ANY_BODY:
                    // Skip checcking of a response body
                    break;
                case "xml":

                    try (InputStream actual = body.getInputStream();
                            InputStream file = HttpClient.class.getResourceAsStream(responseFile)) {
                        Comparators.xml("File: [" + responseFile + "]", file, actual);
                    }
                    break;
                case "json":
                    compareJson(responseFile, body);
                    break;
                default:
                    compareBinary(responseFile, body);
            }
        } catch (Exception | AssertionError ex) {
            throw new RuntimeException(ex);
        }
    }

    private void compareBinary(String responseFile, Resource body) {
        try (InputStream actual = body.getInputStream();
                InputStream file = getClass().getResourceAsStream(responseFile)) {
            if (file == null) {
                throw new FileNotFoundException(String.format("File '%s' is not found", responseFile));
            }

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

    private static void compareJson(String responseFile, Resource body) {
        try (InputStream actual = body.getInputStream();
                InputStream file = HttpClient.class.getResourceAsStream(responseFile)) {
            if (file == null) {
                throw new FileNotFoundException(String.format("File '%s' is not found.", responseFile));
            }
            JsonNode actualNode = OBJECT_MAPPER.readTree(actual);
            JsonNode expectedNode = OBJECT_MAPPER.readTree(file);
            Comparators.compareJsonObjects(expectedNode, actualNode, "");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void send(String reqRespFiles) {
        send("/" + reqRespFiles + ".req", "/" + reqRespFiles + ".resp");
    }

    public void send(String requestFile, String responseFile) {
        try {
            HttpData header = HttpData.send(baseURL, requestFile);

            HttpData respHeader = HttpData.readFile(responseFile);

            header.assertTo(respHeader);

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
