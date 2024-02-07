package org.openl.itest;

import static org.apache.kafka.clients.consumer.ConsumerConfig.METADATA_MAX_AGE_CONFIG;
import static org.awaitility.Awaitility.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.utility.DockerImageName;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

public class RunTracingITest {

    private static final Logger LOG = LoggerFactory.getLogger(RunTracingITest.class);
    public static final int AWAIT_TIMEOUT = 1;

    final String TEST_REST_URL = "/deployment1/simple1/Hello";

    private static JettyServer server;
    private static HttpClient client;
    private static MockTracer tracer;

    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:latest")).withKraft();

    @BeforeAll
    public static void setUp() throws Exception {
        KAFKA_CONTAINER.start();

        tracer = new MockTracer();
        GlobalTracer.registerIfAbsent(tracer);
        server = JettyServer.startSharingClassLoader(
                Map.of("ruleservice.kafka.bootstrap.servers", KAFKA_CONTAINER.getBootstrapServers()));
        client = server.client();
    }

    private interface Procedure {
        void invoke();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        server.stop();
        doQuite(KAFKA_CONTAINER::stop);
    }

    private static void doQuite(Procedure procedure) {
        try {
            procedure.invoke();
        } catch (RuntimeException e) {
            LOG.warn("Error while trying to stop server", e);
        }
    }

    @Test
    public void testRESTServiceSpans() {
        client.send("simple1.tracing.rest.post");
        given().ignoreExceptions().await().atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS).until(() -> {
            List<MockSpan> finishedSpans = tracer.finishedSpans();
            Optional<MockSpan> restSpan = findSpanByURL(finishedSpans, TEST_REST_URL);
            if (!restSpan.isPresent()) {
                return false;
            }
            final MockSpan span = restSpan.get();
            assertEquals(Tags.SPAN_KIND_SERVER, span.tags().get(Tags.SPAN_KIND.getKey()));
            assertEquals("POST", span.operationName());
            assertEquals(5, span.tags().size());
            assertEquals("java-web-servlet", span.tags().get(Tags.COMPONENT.getKey()));
            assertEquals(200, span.tags().get(Tags.HTTP_STATUS.getKey()));
            String url = (String) span.tags().get(Tags.HTTP_URL.getKey());
            assertTrue(url.contains(TEST_REST_URL));
            return true;
        });
    }

    @Test
    public void testKafkaServiceSpan() {
        final String REQUEST = "{\"hour\": 5}";
        testKafka("hello-in-topic", "hello-out-topic", null, REQUEST, "Good Morning");

        List<MockSpan> mockSpans = tracer.finishedSpans();

        Optional<MockSpan> outTopicSpan = findKafkaSpan(mockSpans, "To_hello-out-topic");
        assertTrue(outTopicSpan.isPresent());
        MockSpan toKafkaSpan = outTopicSpan.get();
        final long outTraceId = toKafkaSpan.context().traceId();

        Optional<MockSpan> from = findKafkaSpan(mockSpans, outTraceId, "From_hello-in-topic");
        assertTrue(from.isPresent());

        MockSpan fromKafka = from.get();
        assertTrue(fromKafka.references().isEmpty());

        Optional<MockSpan> serviceCall = findKafkaSpan(mockSpans, outTraceId, "simple1-tracing");
        assertTrue(serviceCall.isPresent());
        MockSpan serviceSpan = serviceCall.get();
        assertTrue(serviceSpan.tags().containsKey("Service Name"));
        MockSpan.Reference reference = serviceSpan.references().get(0);
        assertEquals("child_of", reference.getReferenceType());

        MockSpan.Reference toKafkaRef = toKafkaSpan.references().get(0);
        assertEquals("child_of", toKafkaRef.getReferenceType());

    }

    @Test
    public void testKafkaServiceDLTSpan() {
        final String REQUEST = "{\"hour\": a}";
        testKafka("hello-in-topic", "hello-dlt-topic", null, REQUEST, REQUEST);

        List<MockSpan> mockSpans = tracer.finishedSpans();

        Optional<MockSpan> toDlt = findKafkaSpan(mockSpans, "To_hello-dlt-topic");
        assertTrue(toDlt.isPresent());
        final MockSpan errorTopicSpan = toDlt.get();
        final long traceId = errorTopicSpan.context().traceId();

        Optional<MockSpan> fromKafka = findKafkaSpan(mockSpans, traceId, "From_hello-in-topic");
        assertTrue(fromKafka.isPresent());

        Optional<MockSpan> errorTrace = findKafkaSpan(mockSpans, traceId, "simple1-tracing");
        assertTrue(errorTrace.isPresent());
        MockSpan errorSpan = errorTrace.get();
        Map<String, Object> tags = errorSpan.tags();
        assertTrue((Boolean) tags.get("error"));
        assertEquals(1, errorSpan.logEntries().size());

    }

    @Test
    public void testSkipUrls() {
        client.send("admin/services.get");

        given().ignoreExceptions()
                .await()
                .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
                .until(() -> tracer.finishedSpans().stream().noneMatch(span -> {
                    final boolean containsURL = span.tags().containsKey(Tags.HTTP_URL.getKey());
                    final Object objectURL = span.tags().get(Tags.HTTP_URL.getKey());
                    final String stringURL = (String) objectURL;
                    return containsURL && stringURL.contains("/admin/services");
                }));

        client.send("simple2.tracing.ws.post");
        given().ignoreExceptions()
                .await()
                .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
                .until(() -> tracer.finishedSpans().stream().noneMatch(span -> {
                    final boolean containsURL = span.tags().containsKey(Tags.HTTP_URL.getKey());
                    final Object objectURL = span.tags().get(Tags.HTTP_URL.getKey());
                    final String stringURL = (String) objectURL;
                    return containsURL && stringURL.contains("/deployment1/simple2");
                }));
    }

    private Optional<MockSpan> findKafkaSpan(List<MockSpan> mockSpans, String topicName) {
        return mockSpans.stream()
                .filter(mockSpan -> mockSpan.operationName() != null && mockSpan.operationName().equals(topicName))
                .findFirst();
    }

    private Optional<MockSpan> findKafkaSpan(List<MockSpan> mockSpans, long traceId, String topicName) {
        return mockSpans.stream()
                .filter(mockSpan -> mockSpan.operationName() != null && mockSpan.context().traceId() == traceId && mockSpan
                        .operationName()
                        .equals(topicName))
                .findFirst();
    }

    private Optional<MockSpan> findSpanByURL(List<MockSpan> finishedSpans, String endpoint) {
        return finishedSpans.stream().filter(span -> {
            final boolean containsURL = span.tags().containsKey(Tags.HTTP_URL.getKey());
            final Object tagURL = span.tags().get(Tags.HTTP_URL.getKey());
            final String stringTagURL = (String) tagURL;
            return containsURL && stringTagURL.contains(endpoint);
        }).findFirst();
    }

    private void testKafka(String inTopic, String outTopic, String key, String value, String expectedValue) {
        var producerRecord = new ProducerRecord<>(inTopic, key, value);
        testKafka(producerRecord, outTopic, (response) -> {
            if (expectedValue != null) {
                assertEquals(expectedValue, response.value());
                assertEquals(producerRecord.key(), response.key());
            }
        });
    }

    private void testKafka(ProducerRecord<String, String> producerRecord,
                           String outTopic,
                           Consumer<ConsumerRecord<String, String>> check) {
        var servers = KAFKA_CONTAINER.getBootstrapServers();
        try (var producer = createKafkaProducer(servers); var consumer = createKafkaConsumer(servers)) {
            consumer.subscribe(Collections.singletonList(outTopic));
            producer.send(producerRecord);
            checkKafkaResponse(consumer, check);
            consumer.unsubscribe();
        }
    }

    private static KafkaProducer<String, String> createKafkaProducer(String bootstrapServers) {
        return new KafkaProducer<>(ImmutableMap.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers,
                ProducerConfig.CLIENT_ID_CONFIG,
                UUID.randomUUID().toString()), new StringSerializer(), new StringSerializer());
    }

    private KafkaConsumer<String, String> createKafkaConsumer(String bootstrapServers) {
        return new KafkaConsumer<>(ImmutableMap.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers,
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
