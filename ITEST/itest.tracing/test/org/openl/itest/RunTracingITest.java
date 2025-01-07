package org.openl.itest;

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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.StdErr;
import org.junitpioneer.jupiter.StdIo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.kafka.KafkaContainer;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class RunTracingITest {

    private static final Logger LOG = LoggerFactory.getLogger(RunTracingITest.class);

    private static HttpClient client;

    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer("apache/kafka-native:latest")
            .withEnv("KAFKA_LISTENERS", "PLAINTEXT://:9092,BROKER://:9093,CONTROLLER://:9094");// See KAFKA-18281

    @BeforeAll
    public static void setUp() throws Exception {
        KAFKA_CONTAINER.start();

        client = JettyServer.get()
                .withInitParam("ruleservice.kafka.bootstrap.servers", KAFKA_CONTAINER.getBootstrapServers())
                .start();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        client.close();
        try {
            KAFKA_CONTAINER.stop();
        } catch (RuntimeException e) {
            LOG.warn("Error while trying to stop server", e);
        }
    }

    @Test
    @StdIo
    public void testKafkaServiceSpan(StdErr stdOut) throws Exception {
        try (var producer = createKafkaProducer(); var consumer = createKafkaConsumer()) {
            consumer.subscribe(Collections.singletonList("hello-out-topic"));
            producer.send(new ProducerRecord<>("hello-in-topic", null, "5"));

            checkKafkaResponse(consumer, (response) -> {
                assertEquals("Good Morning", response.value());
            });
            consumer.unsubscribe();
        }
        Thread.sleep(500);
        var log = stdOut.capturedString();

        checkOpenLMethodsSpans(log, "Hello", "hello-in-topic publish", "openl-rules-opentelemetry", "io.opentelemetry.kafka-clients");
    }

    @Test
    @StdIo
    public void testRESTServiceSpans(StdErr stdOut) throws Exception {
        client.send("simple1.tracing.rest.post");

        Thread.sleep(500);
        var log = stdOut.capturedString();
        checkOpenLMethodsSpans(log, "Hello", "POST", "openl-rules-opentelemetry", "io.opentelemetry.java-http-client");
    }

    private void checkOpenLMethodsSpans(String log, String expectedOpenLMethodSpanName, String expectedRootSpanName, String expectedScope, String expectedParentScope) {
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
        return new KafkaProducer<>(Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA_CONTAINER.getBootstrapServers(),
                ProducerConfig.CLIENT_ID_CONFIG,
                UUID.randomUUID().toString()), new StringSerializer(), new StringSerializer());
    }

    private KafkaConsumer<String, String> createKafkaConsumer() {
        return new KafkaConsumer<>(Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
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
