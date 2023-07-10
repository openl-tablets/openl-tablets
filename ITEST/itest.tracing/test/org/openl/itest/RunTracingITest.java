package org.openl.itest;

import static org.apache.kafka.clients.consumer.ConsumerConfig.METADATA_MAX_AGE_CONFIG;
import static org.awaitility.Awaitility.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.utility.DockerImageName;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class RunTracingITest {

    @Rule
    public final SystemErrRule console = new SystemErrRule().enableLog();

    private static final Logger LOG = LoggerFactory.getLogger(RunTracingITest.class);

    private static JettyServer server;
    private static HttpClient client;

    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.0")).withKraft();

    @BeforeClass
    public static void setUp() throws Exception {
        KAFKA_CONTAINER.start();

        server = JettyServer.start(
            Map.of("ruleservice.kafka.bootstrap.servers", KAFKA_CONTAINER.getBootstrapServers()));
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
        try {
            KAFKA_CONTAINER.stop();
        } catch (RuntimeException e) {
            LOG.warn("Error while trying to stop server", e);
        }
    }

    @Before
    public void prepare() {
        console.clearLog();
    }

    @Test
    public void testKafkaServiceSpan() throws InterruptedException {
        try (var producer = createKafkaProducer(); var consumer = createKafkaConsumer()) {
            consumer.subscribe(Collections.singletonList("hello-out-topic"));
            producer.send(new ProducerRecord<>("hello-in-topic", null, "{\"hour\": 5}"));

            checkKafkaResponse(consumer, (response) -> {
                assertEquals("Good Morning", response.value());
            });
            consumer.unsubscribe();
        }

        checkOpenLMethodsSpans("Hello", "hello-in-topic send", "openl-rules-opentelemetry", "io.opentelemetry.kafka-clients");
    }

    @Test
    public void testRESTServiceSpans() throws InterruptedException {
        client.send("simple1.tracing.rest.post");

        checkOpenLMethodsSpans("Hello", "POST", "openl-rules-opentelemetry", "io.opentelemetry.http-url-connection");
    }

    private void checkOpenLMethodsSpans(String expectedOpenLMethodSpanName, String expectedRootSpanName, String expectedScope, String expectedParentScope) throws InterruptedException {
        Thread.sleep(100); // Waiting logs due async deferred output
        String log = console.getLog();
        List<Map> allSpans = getSpanValuesAsMapFromJson(log);
        List<Map> methodSpans = allSpans.stream()
                .filter(span -> span.get("name").equals(expectedOpenLMethodSpanName))
                .collect(Collectors.toList());

        assertEquals(1, methodSpans.size());
        assertEquals(methodSpans.get(0).get("scope"), expectedScope);
        Map spanAttributes = (Map) methodSpans.get(0).get("attributes");
        assertEquals("DecisionTable", spanAttributes.get("openl.table.type"));
        assertEquals("Main", spanAttributes.get("code.namespace"));
        assertEquals("Hello", spanAttributes.get("code.function"));

        List<Map> parentSpans = methodSpans.stream()
                .map(s -> getSpanParent(allSpans, s))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        assertEquals(methodSpans.size(), parentSpans.size());
        Map parentSpan = getSpanByNameAndScope(expectedRootSpanName, expectedParentScope, parentSpans);
        assertNotNull(parentSpan);
    }

    private Map getSpanByNameAndScope(String name, String scope, List<Map> spans) {
        return spans.stream()
                .filter(s -> name.equals(s.get("name")))
                .filter(s -> ((String)(s.get("scope"))).contains(scope))
                .findFirst()
                .orElse(null);
    }

    private Map getSpanParent(List<Map> allSpans, Map span) {
        return allSpans.stream()
                .filter(s -> s.get("traceId").equals(span.get("traceId")))
                .filter(s -> s.get("parentSpanId") == null)
                .findFirst()
                .orElse(null);
    }

    private List<Map> getSpanValuesAsMapFromJson(String log) {
        List<String> spanJsons;
        try (Stream<String> stream = log.lines()) {
            spanJsons = stream.filter(line -> line.contains("OtlpJsonLoggingSpanExporter"))
                    .map(s -> s.substring(s.indexOf('{')))
                    .collect(Collectors.toList());
        }

        ObjectMapper objectMapper = new ObjectMapper();
        List<Map> allSpans = new ArrayList<>();
        spanJsons.forEach(spanJson ->
        {
            try {
                final String SCOPE = "scope";
                final String NAME = "name";
                final String TRACE_ID = "traceId";
                final String SPAN_ID = "spanId";
                final String PARENT_SPAN_ID = "parentSpanId";
                final String ATTRIBUTES = "attributes";

                objectMapper.readTree(spanJson).get("scopeSpans").elements().forEachRemaining(
                        scopeSpan -> {
                            String scope = scopeSpan.get(SCOPE).get(NAME).asText();
                            JsonNode spans = scopeSpan.get("spans");
                            if (spans != null) {
                                spans.elements().forEachRemaining(span -> {
                                    Map<String, Object> spanAsMap = new HashMap<>();
                                    spanAsMap.put(SCOPE, scope);
                                    spanAsMap.put(TRACE_ID, span.get(TRACE_ID).asText());
                                    spanAsMap.put(SPAN_ID, span.get(SPAN_ID).asText());
                                    spanAsMap.put(PARENT_SPAN_ID, span.get(PARENT_SPAN_ID) != null ? span.get(PARENT_SPAN_ID).asText() : null);
                                    spanAsMap.put(NAME, span.get(NAME).asText());
                                    Map<String,Object> attributes = new HashMap<>();
                                    span.get("attributes")
                                            .forEach(a -> attributes.put(a.get("key").asText(),
                                                    a.get("value").get("stringValue") != null ? a.get("value").get("stringValue").asText() : null));
                                    spanAsMap.put(ATTRIBUTES, attributes);
                                    allSpans.add(spanAsMap);
                                });
                            }
                        }
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        return allSpans;
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
