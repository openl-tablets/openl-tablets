package org.openl.itest;

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static net.mguenther.kafka.junit.EmbeddedKafkaConfig.brokers;
import static org.awaitility.Awaitility.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig;
import net.mguenther.kafka.junit.KeyValue;
import net.mguenther.kafka.junit.ObserveKeyValues;
import net.mguenther.kafka.junit.SendKeyValues;

public class RunTracingITest {

    private static final Logger log = Logger.getLogger(RunTracingITest.class);
    public static final int AWAIT_TIMEOUT = 1;

    final String TEST_WS_URL = "/deployment1/simple1";
    final String TEST_REST_URL = "/REST/deployment1/simple1/Hello";

    private static JettyServer server;
    private static HttpClient client;
    private static MockTracer tracer;
    private static EmbeddedKafkaCluster cluster;

    @BeforeClass
    public static void setUp() throws Exception {
        tracer = new MockTracer();
        GlobalTracer.registerIfAbsent(tracer);
        server = JettyServer.startSharingClassLoader();
        client = server.client();
        cluster = provisionWith(EmbeddedKafkaClusterConfig.newClusterConfig()
            .configure(brokers().with("listeners", "PLAINTEXT://:61099"))
            .build());
        cluster.start();
    }

    private interface Procedure {
        void invoke();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
        doQuite(() -> cluster.stop());
    }

    private static void doQuite(Procedure procedure) {
        try {
            procedure.invoke();
        } catch (RuntimeException e) {
            log.warn(e);
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
    public void testWSServiceSpans() {
        client.send("simple1.tracing.ws.post");
        given().ignoreExceptions().await().atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS).until(() -> {
            List<MockSpan> mockSpans = tracer.finishedSpans();
            Optional<MockSpan> wsSpan = findSpanByURL(tracer.finishedSpans(), TEST_WS_URL);
            if (!wsSpan.isPresent()) {
                return false;
            }
            final MockSpan span = wsSpan.get();
            assertEquals(Tags.SPAN_KIND_SERVER, span.tags().get(Tags.SPAN_KIND.getKey()));
            assertEquals("POST", span.operationName());
            assertEquals(5, span.tags().size());
            assertEquals("java-web-servlet", span.tags().get(Tags.COMPONENT.getKey()));
            assertEquals(200, span.tags().get(Tags.HTTP_STATUS.getKey()));
            String url = (String) span.tags().get(Tags.HTTP_URL.getKey());
            assertTrue(url.contains(TEST_WS_URL));
            return true;
        });
    }

    @Test
    public void testKafkaServiceSpan() throws Exception {
        final String REQUEST = "{\"hour\": 5}";
        KeyValue<String, String> record = new KeyValue<>(null, REQUEST);
        cluster.send(SendKeyValues.to("hello-in-topic", Collections.singletonList(record)).useDefaults());
        ObserveKeyValues<String, String> observeRequest = ObserveKeyValues.on("hello-out-topic", 1)
            .with("metadata.max.age.ms", 1000)
            .build();
        List<String> observedValues = cluster.observeValues(observeRequest);
        assertEquals(1, observedValues.size());
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
    public void testKafkaServiceDLTSpan() throws Exception {
        final String REQUEST = "{\"hour\": a}";
        KeyValue<String, String> record = new KeyValue<>(null, REQUEST);
        cluster.send(SendKeyValues.to("hello-in-topic", Collections.singletonList(record)).useDefaults());

        ObserveKeyValues<String, String> observeRequestDlt = ObserveKeyValues.on("hello-dlt-topic", 1)
            .with("metadata.max.age.ms", 1000)
            .build();
        List<String> observedValues = cluster.observeValues(observeRequestDlt);
        assertEquals(1, observedValues.size());
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
}
