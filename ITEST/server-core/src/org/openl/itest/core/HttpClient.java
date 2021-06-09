package org.openl.itest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StreamUtils;
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

    public void get(String url, int status, String responseFile) {
        send(HttpMethod.GET, url, null, status, responseFile);
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
                    assertNotNull("Expected non-empty body for URL :" + url, body);

                    try (InputStream actual = body.getInputStream();
                            InputStream file = HttpClient.class.getResourceAsStream(responseFile)) {
                        Comparators.xml("File: [" + responseFile + "]", file, actual);
                    }
                    break;
                case "json":
                    assertNotNull("Expected non-empty body for URL :" + url, body);
                    compareJson(responseFile, body);
                    break;
                case "zip":
                    assertNotNull("Expected non-empty body for URL :" + url, body);
                    compareZip(responseFile, body);
                    break;
                default:
                    assertNotNull("Expected non-empty body for URL :" + url, body);
                    compareBinary(responseFile, body);
            }
        } catch (Exception | AssertionError ex) {
            if (body != null) {
                try (InputStream actual = body.getInputStream()) {
                    byte[] bytes = StreamUtils.copyToByteArray(actual);
                    HttpData.log(responseFile,
                        response.getStatusCode().toString(),
                        response.getHeaders().toSingleValueMap(),
                        bytes);
                } catch (Exception ignored) {
                    // Ignored
                }
            }
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else if (ex instanceof AssertionError) {
                throw (AssertionError) ex;
            }
            throw new RuntimeException(ex);
        }
    }

    private void compareZip(String responseFile, Resource body) throws IOException, URISyntaxException {
        try (ZipFile expected = new ZipFile(Paths.get(getClass().getResource(responseFile).toURI()).toFile())) {
            Set<String> expectedEntries = new HashSet<>();
            for (Enumeration<? extends ZipEntry> it = expected.entries(); it.hasMoreElements();) {
                ZipEntry entry = it.nextElement();
                if (entry.getName().endsWith("/")) {
                    // skip folder
                    continue;
                }
                expectedEntries.add(entry.getName());
            }
            Set<String> unexpectedEntries = new HashSet<>();
            try (ZipInputStream actual = new ZipInputStream(body.getInputStream())) {
                ZipEntry entry;
                while ((entry = actual.getNextEntry()) != null) {
                    if (entry.getName().endsWith("/")) {
                        // skip folder
                        continue;
                    }
                    if (expectedEntries.remove(entry.getName())) {
                        try (InputStream expectedStream = expected.getInputStream(expected.getEntry(entry.getName()))) {
                            compareStream(String.format("Zip entry: [%s/%s] at: ", responseFile, entry.getName()),
                                expectedStream,
                                actual);
                        }
                    } else {
                        unexpectedEntries.add(entry.getName());
                    }
                }
            }
            boolean failed = false;
            StringBuilder errorMessage = new StringBuilder();
            Function<String, String> tab = s -> "    " + s;
            if (!unexpectedEntries.isEmpty()) {
                failed = true;
                errorMessage.append("UNEXPECTED entries:\r\n")
                    .append(unexpectedEntries.stream().map(tab).collect(Collectors.joining("\r\n")));
            }
            if (!expectedEntries.isEmpty()) {
                if (failed) {
                    errorMessage.append("\r\n");
                } else {
                    failed = true;
                }
                errorMessage.append("MISSED entries:\r\n")
                    .append(expectedEntries.stream().map(tab).collect(Collectors.joining("\r\n")));
            }
            if (failed) {
                fail(errorMessage.toString());
            }
        }
    }

    private void compareBinary(String responseFile, Resource body) throws IOException {
        try (InputStream actual = body.getInputStream();
                InputStream file = getClass().getResourceAsStream(responseFile)) {
            if (file == null) {
                throw new FileNotFoundException(String.format("File '%s' is not found", responseFile));
            }
            compareStream("File: [" + responseFile + "] at: ", actual, file);
        }
    }

    private void compareStream(String message, InputStream expected, InputStream actual) throws IOException {
        int ch = expected.read();
        int counter = -1;
        while (ch != -1) {
            counter++;
            assertEquals(message + counter, ch, actual.read());
            ch = expected.read();
        }

        assertEquals(message + counter, -1, actual.read());
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

    /**
     * DO NOT MAKE THIS METHOD PUBLIC!!!
     *
     * Because of further migration effort for tests which do not follow the style naming.
     *
     * <pre>
     *     test-name[.{xml|txt|json|html|zip}].{HTTP-METHOD}.{req|resp}
     * </pre>
     */
    private void send(String requestFile, String responseFile) {
        try {
            HttpData header = HttpData.send(baseURL, requestFile);

            HttpData respHeader = HttpData.readFile(responseFile);

            header.assertTo(respHeader);

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
