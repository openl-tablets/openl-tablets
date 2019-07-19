package org.openl.rules.ruleservice.publish.kafka;

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import org.junit.*;
import org.openl.itest.core.JettyServer;

import net.mguenther.kafka.junit.*;

public class KafkaRuleServicePublisherTest {
    private static JettyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server = new JettyServer();
        server.start();
    }

    @Rule
    public EmbeddedKafkaCluster cluster = provisionWith(EmbeddedKafkaClusterConfig.create()
        .provisionWith(EmbeddedKafkaConfig.create().with("listeners", "PLAINTEXT://:61099").build())
        .build());

    private void methodSimpleOk() throws Exception {
        KeyValue<String, String> record0 = new KeyValue<>(null, "{\"hour\": 5}");
        cluster.send(SendKeyValues.to("hello-in-topic", Collections.singletonList(record0)).useDefaults());

        ObserveKeyValues<String, String> observeRequest0 = ObserveKeyValues.on("hello-out-topic", 1).build();
        List<String> observedValues = cluster.observeValues(observeRequest0);
        Assert.assertEquals(1, observedValues.size());
        Assert.assertEquals("\"Good Morning\"", observedValues.get(0));
    }

    private void methodSimpleFail() throws Exception {
        KeyValue<String, String> record1 = new KeyValue<>(null, "5");
        cluster.send(SendKeyValues.to("hello-in-topic", Collections.singletonList(record1)).useDefaults());

        ObserveKeyValues<String, String> observeRequestDlt1 = ObserveKeyValues.on("hello-dlt-topic", 1).build();
        List<String> observedValuesDlt1 = cluster.observeValues(observeRequestDlt1);
        Assert.assertEquals(1, observedValuesDlt1.size());
    }

    private void serviceSimpleOk() throws Exception {
        KeyValue<String, String> record2 = new KeyValue<>(null, "{\"hour\": 5}");
        record2.addHeader("methodName", "Hello", Charset.forName("UTF8"));
        cluster.send(SendKeyValues.to("hello-in-topic-2", Collections.singletonList(record2)).useDefaults());

        ObserveKeyValues<String, String> observeRequest2 = ObserveKeyValues.on("hello-out-topic-2", 1).build();
        List<String> observedValues2 = cluster.observeValues(observeRequest2);
        Assert.assertEquals(1, observedValues2.size());
        Assert.assertEquals("\"Good Morning\"", observedValues2.get(0));
    }

    private void serviceSimpleFail() throws Exception {
        KeyValue<String, String> record3 = new KeyValue<>(null, "5");
        record3.addHeader("methodName", "Hello", Charset.forName("UTF8"));
        cluster.send(SendKeyValues.to("hello-in-topic-2", Collections.singletonList(record3)).useDefaults());

        ObserveKeyValues<String, String> observeRequestDlt3 = ObserveKeyValues.on("hello-dlt-topic-2", 1).build();
        List<String> observedValuesDlt3 = cluster.observeValues(observeRequestDlt3);
        Assert.assertEquals(1, observedValuesDlt3.size());
    }

    private void methodSimpleOkWithReplyTopic() throws Exception {
        KeyValue<String, String> record = new KeyValue<>(null, "{\"hour\": 5}");
        record.addHeader("kafka_replyTopic", "hello-reply-topic", Charset.forName("UTF8"));
        cluster.send(SendKeyValues.to("hello-in-topic", Collections.singletonList(record)).useDefaults());

        ObserveKeyValues<String, String> observeRequest0 = ObserveKeyValues.on("hello-reply-topic", 1)
            .with("group.id", "junit")
            .build();
        List<String> observedValues = cluster.observeValues(observeRequest0);
        Assert.assertEquals(1, observedValues.size());
        Assert.assertEquals("\"Good Morning\"", observedValues.get(0));
    }

    private String toString(byte[] bytes) {
        try {
            return new String(bytes, "UTF8");
        } catch (Exception e) {
            return null;
        }
    }

    private void serviceSimpleOkWithReplyTopic() throws Exception {
        KeyValue<String, String> record = new KeyValue<>(null, "{\"hour\": 5}");
        record.addHeader("methodName", "Hello", Charset.forName("UTF8"));
        record.addHeader("kafka_replyTopic", "hello-reply-topic", Charset.forName("UTF8"));
        cluster.send(SendKeyValues.to("hello-in-topic-2", Collections.singletonList(record)).useDefaults());

        ObserveKeyValues<String, String> observeRequest = ObserveKeyValues.on("hello-reply-topic", 1)
            .with("group.id", "junit")
            .build();
        List<String> observedValues = cluster.observeValues(observeRequest);
        Assert.assertEquals(1, observedValues.size());
        Assert.assertEquals("\"Good Morning\"", observedValues.get(0));
    }

    private void methodSimpleOkWithCorrelationId() throws Exception {
        KeyValue<String, String> record = new KeyValue<>(null, "{\"hour\": 5}");
        record.addHeader("kafka_replyTopic", "hello-reply-topic", Charset.forName("UTF8"));
        record.addHeader("kafka_correlationId", "42", Charset.forName("UTF8"));
        cluster.send(SendKeyValues.to("hello-in-topic", Collections.singletonList(record)).useDefaults());

        ObserveKeyValues<String, String> observeRequest = ObserveKeyValues.on("hello-reply-topic", 1)
            .with("group.id", "junit")
            .filterOnHeaders(e -> e.lastHeader("kafka_correlationId") != null && "42"
                .equals(toString(e.lastHeader("kafka_correlationId").value())))
            .build();
        List<String> observedValues = cluster.observeValues(observeRequest);
        Assert.assertEquals(1, observedValues.size());
        Assert.assertEquals("\"Good Morning\"", observedValues.get(0));
    }

    private void serviceSimpleOkWithCorrelationId() throws Exception {
        KeyValue<String, String> record = new KeyValue<>(null, "{\"hour\": 5}");
        record.addHeader("methodName", "Hello", Charset.forName("UTF8"));
        record.addHeader("kafka_replyTopic", "hello-reply-topic", Charset.forName("UTF8"));
        record.addHeader("kafka_correlationId", "42", Charset.forName("UTF8"));
        cluster.send(SendKeyValues.to("hello-in-topic-2", Collections.singletonList(record)).useDefaults());

        ObserveKeyValues<String, String> observeRequest = ObserveKeyValues.on("hello-reply-topic", 1)
            .with("group.id", "junit")
            .filterOnHeaders(e -> e.lastHeader("kafka_correlationId") != null && "42"
                .equals(toString(e.lastHeader("kafka_correlationId").value())))
            .build();
        List<String> observedValues = cluster.observeValues(observeRequest);
        Assert.assertEquals(1, observedValues.size());
        Assert.assertEquals("\"Good Morning\"", observedValues.get(0));
    }

    private String getHeaderValue(KeyValue<?, ?> v, String key) {
        try {
            if (v.getHeaders().lastHeader(key) != null) {
                return new String(v.getHeaders().lastHeader(key).value(), "UTF8");
            }
        } catch (UnsupportedEncodingException e) {
        }
        return null;
    }

    private void testDltHeaders() throws Exception {
        cluster.createTopic(TopicConfig.forTopic("hello-replydlt-topic").withNumberOfPartitions(10).build());

        KeyValue<String, String> record = new KeyValue<>(null, "5");
        record.addHeader("methodName", "Hello", Charset.forName("UTF8"));
        record.addHeader("kafka_replyTopic", "hello-reply-topic", Charset.forName("UTF8"));
        record.addHeader("kafka_replyPartition", "891", Charset.forName("UTF8"));
        record.addHeader("kafka_replyDltTopic", "hello-replydlt-topic", Charset.forName("UTF8"));
        record.addHeader("kafka_replyDltPartition", "5", Charset.forName("UTF8"));
        record.addHeader("kafka_correlationId", "42", Charset.forName("UTF8"));
        cluster.send(SendKeyValues.to("hello-in-topic-2", Collections.singletonList(record)).useDefaults());

        ObserveKeyValues<String, String> observeRequest = ObserveKeyValues.on("hello-replydlt-topic", 1)
            .with("group.id", "junit")
            .with("auto.offset.reset", "earliest")
            .build();

        List<KeyValue<String, String>> observedValues = cluster.observe(observeRequest);
        KeyValue<String, String> v = observedValues.get(0);
        Assert.assertEquals(1, observedValues.size());
        Assert.assertEquals("5", v.getValue());

        Assert.assertEquals("42", getHeaderValue(v, "kafka_correlationId"));
        Assert.assertEquals("Hello", getHeaderValue(v, "methodName"));
        Assert.assertEquals("hello-reply-topic", getHeaderValue(v, "kafka_replyTopic"));
        Assert.assertEquals("891", getHeaderValue(v, "kafka_replyPartition"));
        Assert.assertEquals("org.openl.rules.ruleservice.kafka.ser.MessageFormatException",
            getHeaderValue(v, "kafka_dlt-exception-fqcn"));
        Assert.assertEquals("Message format is wrong.", getHeaderValue(v, "kafka_dlt-exception-message"));
        Assert.assertNotNull(v.getHeaders().lastHeader("kafka_dlt-original-offset"));
        Assert.assertNotNull(v.getHeaders().lastHeader("kafka_dlt-original-partition"));
        Assert.assertNotNull(v.getHeaders().lastHeader("kafka_dlt-exception-stacktrace"));
        Assert.assertEquals("hello-in-topic-2", getHeaderValue(v, "kafka_dlt-original-topic"));
    }

    @Test
    public void test() throws Exception {
        methodSimpleOk();
        methodSimpleFail();
        serviceSimpleOk();
        serviceSimpleFail();
        methodSimpleOkWithReplyTopic();
        serviceSimpleOkWithReplyTopic();

        methodSimpleOkWithCorrelationId();
        serviceSimpleOkWithCorrelationId();

        testDltHeaders();
    }

    @After
    public void destroy() {
        cluster.close();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

}