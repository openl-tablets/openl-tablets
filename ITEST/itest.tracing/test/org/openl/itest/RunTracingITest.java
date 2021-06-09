package org.openl.itest;

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static net.mguenther.kafka.junit.EmbeddedKafkaConfig.brokers;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.ThreadLocalScopeManager;
import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig;
import net.mguenther.kafka.junit.KeyValue;
import net.mguenther.kafka.junit.ObserveKeyValues;
import net.mguenther.kafka.junit.SendKeyValues;

public class RunTracingITest {

    private static final Logger log = Logger.getLogger(RunTracingITest.class);

    private static JettyServer server;
    private static HttpClient client;
    private static MockTracer tracer;
    private static EmbeddedKafkaCluster cluster;

    @BeforeClass
    public static void setUp() throws Exception {
        tracer = Mockito.spy(new MockTracer(new ThreadLocalScopeManager(), MockTracer.Propagator.TEXT_MAP));
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

    @Before
    public void beforeTest() {
        tracer.reset();
    }

    @Test
    public void testRESTServiceSpans() {
        client.send("simple1.tracing.rest.post");
        List<MockSpan> mockSpans = tracer.finishedSpans();
        assertEquals(1, mockSpans.size());
        MockSpan span = mockSpans.iterator().next();

        assertEquals(Tags.SPAN_KIND_SERVER, span.tags().get(Tags.SPAN_KIND.getKey()));
        assertEquals("POST", span.operationName());
        assertEquals(5, span.tags().size());
        assertEquals("java-web-servlet", span.tags().get(Tags.COMPONENT.getKey()));
        assertEquals(200, span.tags().get(Tags.HTTP_STATUS.getKey()));
        String url = (String) span.tags().get(Tags.HTTP_URL.getKey());
        assertTrue(url.contains("/REST/deployment1/simple1/Hello"));
    }

    @Test
    public void testWSServiceSpans() {
        client.send("simple1.tracing.ws.post");
        List<MockSpan> mockSpans = tracer.finishedSpans();

        assertEquals(1, mockSpans.size());

        MockSpan span = mockSpans.iterator().next();
        assertEquals(Tags.SPAN_KIND_SERVER, span.tags().get(Tags.SPAN_KIND.getKey()));
        assertEquals("POST", span.operationName());
        assertEquals(5, span.tags().size());
        assertEquals("java-web-servlet", span.tags().get(Tags.COMPONENT.getKey()));
        assertEquals(200, span.tags().get(Tags.HTTP_STATUS.getKey()));
        String url = (String) span.tags().get(Tags.HTTP_URL.getKey());
        assertTrue(url.contains("/deployment1/simple1"));
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

        Optional<MockSpan> from = mockSpans.stream()
            .filter(x -> x.operationName().equals("From_hello-in-topic"))
            .findFirst();
        assertTrue(from.isPresent());

        MockSpan fromKafka = from.get();
        assertEquals(1, fromKafka.context().traceId());
        assertTrue(fromKafka.references().isEmpty());

        Optional<MockSpan> serviceCall = mockSpans.stream()
            .filter(x -> x.operationName().equals("simple1-tracing"))
            .findFirst();
        assertTrue(serviceCall.isPresent());
        MockSpan serviceSpan = serviceCall.get();
        assertEquals(1, serviceSpan.context().traceId());
        assertTrue(serviceSpan.tags().containsKey("Service Name"));
        MockSpan.Reference reference = serviceSpan.references().get(0);
        assertEquals("child_of", reference.getReferenceType());

        Optional<MockSpan> toKafka = mockSpans.stream()
            .filter(x -> x.operationName().equals("To_hello-out-topic"))
            .findFirst();
        assertTrue(toKafka.isPresent());
        MockSpan toKafkaSpan = toKafka.get();
        assertEquals(1, toKafkaSpan.context().traceId());
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
        assertEquals(3, mockSpans.size());
        Optional<MockSpan> fromKafka = mockSpans.stream()
            .filter(x -> x.operationName().equals("From_hello-in-topic"))
            .findFirst();
        assertTrue(fromKafka.isPresent());
        long traceId = fromKafka.get().context().traceId();

        Optional<MockSpan> errorTrace = mockSpans.stream()
            .filter(x -> x.operationName().equals("simple1-tracing"))
            .findFirst();
        assertTrue(errorTrace.isPresent());
        MockSpan errorSpan = errorTrace.get();
        assertEquals(traceId, errorSpan.context().traceId());
        Map<String, Object> tags = errorSpan.tags();
        assertTrue((Boolean) tags.get("error"));
        assertEquals(1, errorSpan.logEntries().size());

        Optional<MockSpan> toDlt = mockSpans.stream()
            .filter(x -> x.operationName().equals("To_hello-dlt-topic"))
            .findFirst();
        assertTrue(toDlt.isPresent());
        assertEquals(traceId, toDlt.get().context().traceId());
    }

    @Test
    public void testSkipUrls() {
        client.send("admin/services.get");
        List<MockSpan> mockSpans = tracer.finishedSpans();
        assertEquals(0, mockSpans.size());

        client.send("simple2.tracing.ws.post");
        List<MockSpan> mockSpansIgnored = tracer.finishedSpans();
        assertEquals(0, mockSpansIgnored.size());
    }

}
