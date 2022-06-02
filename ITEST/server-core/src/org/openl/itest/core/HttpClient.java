package org.openl.itest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * A simple HTTP client which allows to send a request file and compares a response with a response file.
 * 
 * @author Yury Molchan
 */
public class HttpClient {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK_BOLD = "\u001B[1;30m";
    public static final String ANSI_RED_BOLD = "\u001B[1;31m";
    public static final String ANSI_GREEN_BOLD = "\u001B[1;32m";

    private final RestTemplate rest;
    private final URL baseURL;
    private final ThreadLocal<String> cookie = new ThreadLocal<>();

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

    public void send(String reqRespFiles) {
        send("/" + reqRespFiles + ".req", "/" + reqRespFiles + ".resp");
    }

    public void test() {
        test("test-resources");
    }

    /**
     * Traverses a directory recursively and sort testcases lexicographically.
     * So it allows grouping test suites and executing in predefined order.
     * It finds *.req files and match them with *.resp files.
     * <p>
     * Note. This method uses System.out and System.err for logging instead of Slf4j to provide consistent output
     * in cases when binding of the logger is failed.
     *
     * @param path a root directory where HTTP request files are stored
     */
    public void test(String path) {
        try (Stream<Path> walk = Files.walk(Paths.get(path))) {
            long errors = walk.map(Path::toString).filter(p -> p.endsWith(".req")).map(p -> p.substring(0, p.length() - 4)).sorted().map(p -> {
                System.out.print(ANSI_BLACK_BOLD + p + ANSI_RESET + " - ");
                long start = System.currentTimeMillis();
                try {
                    send(p + ".req", p + ".resp");
                    long end = System.currentTimeMillis();
                    System.out.println(ANSI_GREEN_BOLD + "OK" + ANSI_RESET + " (" + (end-start) + "ms)");
                    return false;
                } catch (Exception ex) {
                    long end = System.currentTimeMillis();
                    System.out.println(ANSI_RED_BOLD + "FAIL" + ANSI_RESET + " (" + (end-start) + "ms)");
                    ex.printStackTrace();
                    return true;
                }
            }).filter(p -> p).count();
            assertEquals("Failed requests: ", 0, errors);
        } catch (IOException e) {
            fail("Test folder is not found: " + path);
        }
    }

    public <T> T getForObject(String url, Class<T> cl, HttpStatus status) {
        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.hasLength(cookie.get())) {
            headers.add("Cookie", cookie.get());
        }
        return getForObject(url, cl, status, headers);
    }

    public <T> T getForObject(String url, Class<T> cl, HttpStatus status, HttpHeaders httpHeaders) {
        ResponseEntity<T> exchange = rest.exchange(url, HttpMethod.GET, new HttpEntity<>(httpHeaders), cl);
        assertEquals("URL :" + url, status, exchange.getStatusCode());
        return exchange.getBody();
    }

    public <T> T putForObject(String url, Object request, Class<T> cl, HttpStatus status, HttpHeaders httpHeaders) {
        ResponseEntity<T> exchange = rest.exchange(url, HttpMethod.PUT, new HttpEntity<>(request, httpHeaders), cl);
        assertEquals("URL :" + url, status, exchange.getStatusCode());
        return exchange.getBody();
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
            HttpData header = HttpData.send(baseURL, requestFile, cookie.get());

            if (StringUtils.hasLength(header.getCookie())) {
                cookie.set(header.getCookie());
            }

            HttpData respHeader = HttpData.readFile(responseFile);

            header.assertTo(respHeader);

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
