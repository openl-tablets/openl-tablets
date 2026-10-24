package org.openl.itest;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

/**
 * OpenL Studio sends the OpenTelemetry data of its pages and of its MCP server to the collector its environment names.
 *
 * <p>The build starts the test with the {@code OTEL_*} variables the Docker image reads: the address of the collector
 * the test runs on a port the build reserved, a header with a token, the name of the service and a resource attribute.
 * The collector keeps every post it receives.
 *
 * <p>A browser is not run: the requests of {@code test-resources/pages} are the ones a page makes, the settings it
 * reads after the sign-in and the OTLP/JSON its SDK posts. The MCP server is the real one, which OpenL Studio runs
 * with Node.js.
 *
 * @author Yury Molchan
 */
class TelemetryTest {

    /** The trace of the MCP client: {@code traceparent} of {@code test-resources/mcp}. */
    private static final String CLIENT_TRACE = "0af7651916cd43dd8448eb211c80319c";
    private static final String TOKEN = "Bearer itest-token";
    private static final String TRACES = "/v1/traces";
    private static final String METRICS = "/v1/metrics";
    private static final ObjectMapper JSON = new ObjectMapper();

    /** A post the collector received. */
    private record Post(String path, String contentType, String authorization, JsonNode body) {
    }

    private static final List<Post> RECEIVED = new CopyOnWriteArrayList<>();
    private static HttpServer collector;
    private static HttpClient client;

    @BeforeAll
    static void setUp() throws IOException {
        var port = Integer.parseInt(System.getProperty("otel.collector.port"));
        collector = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        collector.createContext("/", exchange -> {
            var headers = exchange.getRequestHeaders();
            var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            RECEIVED.add(new Post(exchange.getRequestURI().getPath(), headers.getFirst("Content-Type"),
                    headers.getFirst("Authorization"), JSON.readTree(body)));
            var answer = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, answer.length);
            exchange.getResponseBody().write(answer);
            exchange.close();
        });
        collector.start();
        client = JettyServer.get()
                .withInitParam("mcp.enabled", "true")
                .withInitParam("mcp.node", node())
                .start();
    }

    @AfterAll
    static void tearDown() throws Exception {
        client.close();
        collector.stop(0);
    }

    /**
     * A page reads what to send and the name of its service, and posts its spans and metrics to OpenL Studio, which
     * passes them on to the collector with the header of the environment. The spans of the page claim to be the
     * backend, {@code OpenL-ITest}: they reach the collector as the service of the pages all the same. A signal OpenL
     * Studio does not send, and data that is not JSON, are refused.
     */
    @Test
    void passesTheTelemetryOfThePagesOnToTheCollector() {
        client.test("test-resources/pages");

        var spans = postsOf(TRACES, "OpenL-ITest-ui").toList();
        assertEquals(1, spans.size(), "the spans the page posted");
        assertEquals("application/json", spans.getFirst().contentType());
        assertEquals(TOKEN, spans.getFirst().authorization());
        assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", spans.getFirst().body()
                .at("/resourceSpans/0/scopeSpans/0/spans/0/traceId").asText());

        var metrics = postsOf(METRICS, "OpenL-ITest-ui").toList();
        assertEquals(1, metrics.size(), "the metrics the page posted");
        assertEquals(TOKEN, metrics.getFirst().authorization());
    }

    /**
     * The MCP server continues the trace of its client: the request it serves, the tool call and its call to the REST
     * API of OpenL Studio are spans of that trace, sent as the service of OpenL Studio with the suffix {@code -mcp}.
     * It measures the durations of all three.
     */
    @Test
    void sendsTheSpansAndTheMetricsOfTheMcpServer() {
        client.test("test-resources/mcp");

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            var spans = mcpSpans().filter(span -> span.path("traceId").asText().equals(CLIENT_TRACE)).toList();
            var names = spans.stream().map(span -> span.path("name").asText()).toList();
            // The request at /mcp, the tool call, and the calls of the tool to the REST API of OpenL Studio
            assertTrue(names.containsAll(List.of("POST", "tools/call openl_list_repositories", "GET")),
                    names::toString);
            var toolCall = spans.stream().filter(span -> span.path("name").asText().startsWith("tools/call"))
                    .findFirst().orElseThrow();
            assertEquals("openl_list_repositories", attribute(toolCall, "gen_ai.tool.name"));
            assertEquals("tools/call", attribute(toolCall, "mcp.method.name"));
        });
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            var names = postsOf(METRICS, "OpenL-ITest-mcp")
                    .flatMap(post -> elements(post.body(), "/resourceMetrics/0/scopeMetrics"))
                    .flatMap(scope -> elements(scope, "/metrics"))
                    .map(metric -> metric.path("name").asText())
                    .toList();
            assertTrue(names.containsAll(List.of("mcp.server.operation.duration", "http.server.request.duration",
                    "http.client.request.duration")), names::toString);
        });
        assertTrue(postsOf(TRACES, "OpenL-ITest-mcp").allMatch(post -> TOKEN.equals(post.authorization())),
                "the MCP server sends the header of the environment");
    }

    /** The spans the MCP server sent. */
    private static Stream<JsonNode> mcpSpans() {
        return postsOf(TRACES, "OpenL-ITest-mcp").flatMap(post -> elements(post.body(), "/resourceSpans/0/scopeSpans"))
                .flatMap(scope -> elements(scope, "/spans"));
    }

    /** The posts to the path of a signal, {@code /v1/traces} or {@code /v1/metrics}, sent as the service. */
    private static Stream<Post> postsOf(String path, String service) {
        var resources = path.equals(TRACES) ? "resourceSpans" : "resourceMetrics";
        return RECEIVED.stream().filter(post -> post.path().equals(path)
                && attribute(post.body().path(resources).path(0).path("resource"), "service.name").equals(service));
    }

    /** The string value of an attribute of a span or a resource. */
    private static String attribute(JsonNode node, String key) {
        return elements(node, "/attributes").filter(attribute -> attribute.path("key").asText().equals(key))
                .map(attribute -> attribute.at("/value/stringValue").asText())
                .findFirst().orElse("");
    }

    private static Stream<JsonNode> elements(JsonNode node, String pointer) {
        return node.at(pointer).valueStream();
    }

    /** The Node.js runtime the build of this suite installs. */
    private static String node() {
        var windows = System.getProperty("os.name").startsWith("Windows");
        return Path.of("target", "node", windows ? "node.exe" : "node").toAbsolutePath().toString();
    }
}
