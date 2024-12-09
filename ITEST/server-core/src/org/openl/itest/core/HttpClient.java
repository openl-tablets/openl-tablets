package org.openl.itest.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * A simple HTTP client which allows to send a request file and compares a response with a response file.
 *
 * @author Yury Molchan
 */
public class HttpClient implements AutoCloseable{

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK_BOLD = "\u001B[2;36m";
    public static final String ANSI_RED_BOLD = "\u001B[1;31m";
    public static final String ANSI_GREEN_BOLD = "\u001B[1;32m";

    private final JettyServer server;
    private final URI baseURL;
    private final java.net.http.HttpClient client;
    private final ThreadLocal<String> cookie = new ThreadLocal<>();
    public final Map<String, String> localEnv = new HashMap<>();

    private final int retryTimeout;

    HttpClient(JettyServer server, URI baseURL) {
        this.server = server;
        this.baseURL = baseURL;
        var builder = java.net.http.HttpClient.newBuilder().version(java.net.http.HttpClient.Version.HTTP_1_1);

        int connectTimeout = Integer.parseInt(System.getProperty("http.timeout.connect"));
        if (connectTimeout > 0) {
            builder.connectTimeout(Duration.ofMillis(connectTimeout));
        }
        int retryTimeout = Integer.parseInt(System.getProperty("http.timeout.read")) * 2;
        if (retryTimeout <= 0) {
            retryTimeout = 120_000; // 2 minutes
        }
        this.retryTimeout = retryTimeout;
        this.client = builder.build();
    }

    private HttpRequest.Builder requestBuilder(String url, String[] headers) throws URISyntaxException {
        var uri = baseURL.resolve(url);
        var builder = HttpRequest.newBuilder().uri(uri);

        int readTimeout = Integer.parseInt(System.getProperty("http.timeout.read"));
        if (readTimeout > 0) {
            builder.timeout(Duration.ofMillis(readTimeout));
        }

        var c = cookie.get();
        if (c != null && !c.isBlank()) {
            builder.header("Cookie", c);
        }

        if (headers != null && headers.length > 0) {
            builder.headers(headers);
        }

        return builder;
    }

    public void send(String reqRespFiles) {
        send("test-resources/" + reqRespFiles + ".req", "test-resources/" + reqRespFiles + ".resp");
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
                    System.out.println(ANSI_GREEN_BOLD + "OK" + ANSI_RESET + " (" + (end - start) + "ms)");
                    return false;
                } catch (Exception | AssertionError ex) {
                    long end = System.currentTimeMillis();
                    System.out.println(ANSI_RED_BOLD + "FAIL" + ANSI_RESET + " (" + (end - start) + "ms)");
                    ex.printStackTrace();
                    return true;
                }
            }).filter(p -> p).count();
            assertEquals(0, errors, "Failed requests: ");
        } catch (IOException e) {
            fail("Test folder is not found: " + path);
        }
    }

    public <T> T getForObject(String url, Class<T> cl, int status, String... headers) {
        try {
            var req = requestBuilder(url, headers)
                    .GET()
                    .build();
            var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            assertEquals(status, resp.statusCode(), "URL :" + url);
            if (cl == String.class) {
                return (T) resp.body();
            }
            return HttpData.OBJECT_MAPPER.readValue(resp.body(), cl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void putForObject(String url, Object request, String... headers) {

        try {
            var req = requestBuilder(url, headers)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(HttpData.OBJECT_MAPPER.writeValueAsString(request)))
                    .build();

            var resp = client.send(req, HttpResponse.BodyHandlers.discarding());
            assertEquals(204, resp.statusCode(), "URL :" + url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * DO NOT MAKE THIS METHOD PUBLIC!!!
     * <p>
     * Because of further migration effort for tests which do not follow the style naming.
     *
     * <pre>
     *     test-name[.{xml|txt|json|html|zip}].{HTTP-METHOD}.{req|resp}
     * </pre>
     */
    private void send(String requestFile, String responseFile) {
        try {
            HttpData request = HttpData.readFile(requestFile);
            if (request == null) {
                throw new FileNotFoundException(requestFile);
            }

            var assertResponse = Objects.requireNonNullElse(HttpData.readFile(responseFile), HttpData.ok());
            String retry = request.getSetting("Retry");
            long timeout = System.currentTimeMillis();
            if ("yes".equals(retry)) {
                timeout += retryTimeout;
            }

            AssertionError error = null;
            HttpData response;
            do {
                if (error != null) {
                    TimeUnit.MILLISECONDS.sleep(100);
                }
                response = HttpData.send(baseURL, request, cookie.get(), localEnv);

                var c = response.getCookie();
                if (c != null && !c.isBlank()) {
                    cookie.set(c);
                }

                // Bulk update of OpenAPI files
//                if (Files.readAllLines(Paths.get(requestFile)).get(0).contains("/openapi.")) {
//                    response.writeBodyTo(responseFile);
//                }


                try {
                    response.assertTo(assertResponse);
                    error = null;
                } catch (AssertionError e) {
                    error = e;
                }
            } while (error != null && System.currentTimeMillis() < timeout);

            if (error != null) {
                response.log(requestFile);
                throw error;
            }

        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        server.stop();
    }
}
