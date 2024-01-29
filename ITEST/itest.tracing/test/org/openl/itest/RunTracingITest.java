package org.openl.itest;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErrAndOut;
import static org.apache.kafka.clients.consumer.ConsumerConfig.METADATA_MAX_AGE_CONFIG;
import static org.awaitility.Awaitility.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.utility.DockerImageName;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class RunTracingITest {

    @Rule

    private static final Logger LOG = LoggerFactory.getLogger(RunTracingITest.class);

    private static JettyServer server;
    private static HttpClient client;

    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:latest")).withKraft();

    @BeforeAll
    public static void setUp() throws Exception {
        KAFKA_CONTAINER.start();

        server = JettyServer.start(
                Map.of("ruleservice.kafka.bootstrap.servers", KAFKA_CONTAINER.getBootstrapServers()));
        client = server.client();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        server.stop();
        try {
            KAFKA_CONTAINER.stop();
        } catch (RuntimeException e) {
            LOG.warn("Error while trying to stop server", e);
        }
    }

    @Test
    public void testKafkaServiceSpan() throws Exception {
        var log = tapSystemErrAndOut(() -> {
            try (var producer = createKafkaProducer(); var consumer = createKafkaConsumer()) {
                consumer.subscribe(Collections.singletonList("hello-out-topic"));
                producer.send(new ProducerRecord<>("hello-in-topic", null, "{\"hour\": 5}"));

                checkKafkaResponse(consumer, (response) -> {
                    assertEquals("Good Morning", response.value());
                });
                consumer.unsubscribe();
            }
        });

        checkOpenLMethodsSpans(log, "Hello", "hello-in-topic publish", "openl-rules-opentelemetry", "io.opentelemetry.kafka-clients");
    }

    @Test
    public void testRESTServiceSpans() throws Exception {
        var log = tapSystemErrAndOut(() -> client.send("simple1.tracing.rest.post"));

        checkOpenLMethodsSpans(log, "Hello", "POST", "openl-rules-opentelemetry", "io.opentelemetry.http-url-connection");
    }

    private void checkOpenLMethodsSpans(String log, String expectedOpenLMethodSpanName, String expectedRootSpanName, String expectedScope, String expectedParentScope) throws InterruptedException {
        Thread.sleep(100); // Waiting logs due async deferred output
        ObjectMapper objectMapper = new ObjectMapper();
        List<ObjectNode> spanJsons;
        var allSpans = log.lines()
                .filter(line -> line.contains("OtlpJsonLoggingSpanExporter"))
                .map(s -> s.substring(s.indexOf('{')))
                .flatMap(s -> {
                    try {
                        var scopeSpans = objectMapper.readTree(s).get("scopeSpans").elements();

                        var spliterator = Spliterators.spliteratorUnknownSize(scopeSpans, Spliterator.ORDERED);
                        return StreamSupport.stream(spliterator, false);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .flatMap(s -> {
                    var scope = s.get("scope").get("name").asText();
                    var spans = s.get("spans").elements();
                    if (spans == null) {
                        return Stream.empty();
                    }
                    var spliterator = Spliterators.spliteratorUnknownSize(spans, Spliterator.ORDERED);
                    return StreamSupport.stream(spliterator, false).map(n -> ((ObjectNode) n).put("scope", scope));
                })
                .map(s -> {
                    Map<String, Object> spanAsMap = new HashMap<>();
                    s.fields().forEachRemaining(n -> {
                        JsonNode node = n.getValue();
                        var val = node.isArray() ? asMap(node) : node.asText();
                        spanAsMap.put(n.getKey(), val);
                    });
                    return spanAsMap;
                }).collect(Collectors.toList());

        var methodSpans = allSpans.stream()
                .filter(span -> span.get("name").equals(expectedOpenLMethodSpanName))
                .collect(Collectors.toList());

        assertEquals(1, methodSpans.size());
        var methodSpan = methodSpans.get(0);
        assertEquals(methodSpan.get("scope"), expectedScope);
        var spanAttributes = (Map) methodSpan.get("attributes");
        assertEquals("DecisionTable", spanAttributes.get("openl.table.type"));
        assertEquals("Main", spanAttributes.get("code.namespace"));
        assertEquals("Hello", spanAttributes.get("code.function"));

        var traceId = methodSpan.get("traceId");
        var parentSpans = allSpans.stream()
                .filter(s -> s.get("parentSpanId") == null)
                .filter(s -> s.get("traceId").equals(traceId))
                .collect(Collectors.toList());

        assertEquals(1, parentSpans.size());
        var parentSpan = parentSpans.get(0);
        assertEquals(expectedRootSpanName, parentSpan.get("name"));
        assertTrue(parentSpan.get("scope").toString().contains(expectedParentScope));
    }

    private static Map<String, String> asMap(JsonNode node) {

        var result = new HashMap<String, String>();
        node.elements().forEachRemaining(a -> {
            String key = a.get("key").asText();
            JsonNode vNode = a.get("value").get("stringValue");
            result.put(key, vNode != null ? vNode.asText() : null);
        });
        return result;
    }

    private static KafkaProducer<String, String> createKafkaProducer() {
        return new KafkaProducer<>(ImmutableMap.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA_CONTAINER.getBootstrapServers(),
                ProducerConfig.CLIENT_ID_CONFIG,
                UUID.randomUUID().toString()), new StringSerializer(), new StringSerializer());
    }

    private KafkaConsumer<String, String> createKafkaConsumer() {
        return new KafkaConsumer<>(ImmutableMap.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA_CONTAINER.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "tc-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest",
                METADATA_MAX_AGE_CONFIG,
                1000), new StringDeserializer(), new StringDeserializer());
    }

    private void checkKafkaResponse(KafkaConsumer<String, String> consumer, Consumer<ConsumerRecord<String, String>> check) {
        given().ignoreExceptions().atMost(20, TimeUnit.SECONDS).until(() -> {
            var records = consumer.poll(Duration.ofMillis(1000));
            if (records.isEmpty()) {
                return false;
            }
            assertEquals(1, records.count());
            var response = records.iterator().next();
            check.accept(response);
            return true;
        });
    }
}
