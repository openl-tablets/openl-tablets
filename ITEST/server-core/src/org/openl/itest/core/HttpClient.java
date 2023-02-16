package org.openl.itest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.stream.Stream;

/**
 * A simple HTTP client which allows to send a request file and compares a response with a response file.
 * 
 * @author Yury Molchan
 */
public class HttpClient {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK_BOLD = "\u001B[2;36m";
    public static final String ANSI_RED_BOLD = "\u001B[1;31m";
    public static final String ANSI_GREEN_BOLD = "\u001B[1;32m";

    private final URL baseURL;
    private final java.net.http.HttpClient client;
    private final ThreadLocal<String> cookie = new ThreadLocal<>();

    private HttpClient(URL baseURL) {
        this.baseURL = baseURL;
        int connectTimeout = Integer.parseInt(System.getProperty("http.timeout.connect"));
        this.client = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .build();
    }

    private HttpRequest.Builder requestBuilder(String url, String[] headers) throws URISyntaxException {
        var uri = baseURL.toURI().resolve(url);
        int readTimeout = Integer.parseInt(System.getProperty("http.timeout.read"));
        var bld = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMillis(readTimeout));

        var c = cookie.get();
        if (c != null && !c.isBlank()) {
            bld.header("Cookie", c);
        }

        if (headers != null && headers.length > 0) {
            bld.headers(headers);
        }

        return bld;
    }

    static HttpClient create(URL url) {
        return new HttpClient(url);
    }

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

    public <T> T getForObject(String url, Class<T> cl, int status, String... headers) {
        try {
            var req = requestBuilder(url, headers)
                    .GET()
                    .build();
            var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            assertEquals("URL :" + url, status, resp.statusCode());
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
            assertEquals("URL :" + url, 204, resp.statusCode());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

            var c = header.getCookie();
            if (c != null && !c.isBlank()) {
                cookie.set(c);
            }

            HttpData respHeader = HttpData.readFile(responseFile);

            header.assertTo(respHeader);

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
