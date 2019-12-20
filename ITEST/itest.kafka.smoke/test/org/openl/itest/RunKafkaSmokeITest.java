package org.openl.itest;

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.JettyServer;
import org.openl.rules.ruleservice.kafka.KafkaHeaders;

import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig;
import net.mguenther.kafka.junit.EmbeddedKafkaConfig;
import net.mguenther.kafka.junit.KeyValue;
import net.mguenther.kafka.junit.ObserveKeyValues;
import net.mguenther.kafka.junit.SendKeyValues;
import net.mguenther.kafka.junit.TopicConfig;

public class RunKafkaSmokeITest {
    private static JettyServer server;
    private static EmbeddedKafkaCluster cluster;

    @BeforeClass
    public static void setUp() throws Exception {
        cluster = provisionWith(EmbeddedKafkaClusterConfig.create()
            .provisionWith(EmbeddedKafkaConfig.create().with("listeners", "PLAINTEXT://:61099").build())
            .build());

        cluster.start();

        server = JettyServer.start();
    }

    @Test
    public void methodSimpleOk() throws Exception {
        KeyValue<String, String> record0 = new KeyValue<>("key1", "{\"hour\": 5}");
        cluster.send(SendKeyValues.to("hello-in-topic", Collections.singletonList(record0)).useDefaults());

        ObserveKeyValues<String, String> observeRequest = ObserveKeyValues.on("hello-out-topic", 1)
            .with("metadata.max.age.ms", 1000)
            .build();
        List<KeyValue<String, String>> observes = cluster.observe(observeRequest);
        Assert.assertEquals(1, observes.size());
        Assert.assertEquals("Good Morning", observes.get(0).getValue());
        Assert.assertEquals("key1", observes.get(0).getKey());
    }

    @Test
    public void methodSimpleFail() throws Exception {
        KeyValue<String, String> record1 = new KeyValue<>("key1", "5");
        cluster.send(SendKeyValues.to("hello-in-topic", Collections.singletonList(record1)).useDefaults());

        ObserveKeyValues<String, String> observeRequestDlt = ObserveKeyValues.on("hello-dlt-topic", 1)
            .with("metadata.max.age.ms", 1000)
            .build();
        List<KeyValue<String, String>> observesDlt = cluster.observe(observeRequestDlt);
        Assert.assertEquals(1, observesDlt.size());
        Assert.assertEquals("5", observesDlt.get(0).getValue());
        Assert.assertEquals("key1", observesDlt.get(0).getKey());
    }

    @Test
    public void serviceSimpleOk() throws Exception {
        KeyValue<String, String> record2 = new KeyValue<>("key1", "{\"hour\": 5}");
        record2.addHeader(KafkaHeaders.METHOD_NAME, "Hello", StandardCharsets.UTF_8);
        cluster.send(SendKeyValues.to("hello-in-topic-2", Collections.singletonList(record2)).useDefaults());

        ObserveKeyValues<String, String> observeRequest = ObserveKeyValues.on("hello-out-topic-2", 1)
            .with("metadata.max.age.ms", 1000)
            .build();
        List<KeyValue<String, String>> observes = cluster.observe(observeRequest);
        Assert.assertEquals(1, observes.size());
        Assert.assertEquals("Good Morning", observes.get(0).getValue());
        Assert.assertEquals("key1", observes.get(0).getKey());
    }

    @Test
    public void serviceSimpleFail() throws Exception {
        KeyValue<String, String> record3 = new KeyValue<>("key1", "5");
        record3.addHeader(KafkaHeaders.METHOD_NAME, "Hello", StandardCharsets.UTF_8);
        cluster.send(SendKeyValues.to("hello-in-topic-2", Collections.singletonList(record3)).useDefaults());

        ObserveKeyValues<String, String> observeRequestDlt = ObserveKeyValues.on("hello-dlt-topic-2", 1)
            .with("metadata.max.age.ms", 1000)
            .build();
        List<KeyValue<String, String>> observesDlt = cluster.observe(observeRequestDlt);
        Assert.assertEquals(1, observesDlt.size());
        Assert.assertEquals("5", observesDlt.get(0).getValue());
        Assert.assertEquals("key1", observesDlt.get(0).getKey());
    }

    @Test
    public void methodSimpleOkWithReplyTopic() throws Exception {
        KeyValue<String, String> record = new KeyValue<>("key1", "{\"hour\": 5}");
        record.addHeader(KafkaHeaders.REPLY_TOPIC, "hello-reply-topic", StandardCharsets.UTF_8);
        cluster.send(SendKeyValues.to("hello-in-topic", Collections.singletonList(record)).useDefaults());

        ObserveKeyValues<String, String> observeRequest = ObserveKeyValues.on("hello-reply-topic", 1)
            .with("metadata.max.age.ms", 1000)
            .with("group.id", "junit")
            .build();
        List<KeyValue<String, String>> observes = cluster.observe(observeRequest);
        Assert.assertEquals(1, observes.size());
        Assert.assertEquals("Good Morning", observes.get(0).getValue());
        Assert.assertEquals("key1", observes.get(0).getKey());
    }

    private String toString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Test
    public void serviceSimpleOkWithReplyTopic() throws Exception {
        KeyValue<String, String> record = new KeyValue<>("key1", "{\"hour\": 5}");
        record.addHeader(KafkaHeaders.METHOD_NAME, "Hello", StandardCharsets.UTF_8);
        record.addHeader(KafkaHeaders.REPLY_TOPIC, "hello-reply-topic", StandardCharsets.UTF_8);
        cluster.send(SendKeyValues.to("hello-in-topic-2", Collections.singletonList(record)).useDefaults());

        ObserveKeyValues<String, String> observeRequest = ObserveKeyValues.on("hello-reply-topic", 1)
            .with("metadata.max.age.ms", 1000)
            .with("group.id", "junit")
            .build();
        List<KeyValue<String, String>> observes = cluster.observe(observeRequest);
        Assert.assertEquals(1, observes.size());
        Assert.assertEquals("Good Morning", observes.get(0).getValue());
        Assert.assertEquals("key1", observes.get(0).getKey());
    }

    @Test
    public void methodSimpleOkWithCorrelationId() throws Exception {
        KeyValue<String, String> record = new KeyValue<>("key1", "{\"hour\": 5}");
        record.addHeader(KafkaHeaders.REPLY_TOPIC, "hello-reply-topic", StandardCharsets.UTF_8);
        record.addHeader(KafkaHeaders.CORRELATION_ID, "42", StandardCharsets.UTF_8);
        cluster.send(SendKeyValues.to("hello-in-topic", Collections.singletonList(record)).useDefaults());

        ObserveKeyValues<String, String> observeRequest = ObserveKeyValues.on("hello-reply-topic", 1)
            .with("metadata.max.age.ms", 1000)
            .with("group.id", "junit")
            .filterOnHeaders(e -> e.lastHeader(KafkaHeaders.CORRELATION_ID) != null && "42"
                .equals(toString(e.lastHeader(KafkaHeaders.CORRELATION_ID).value())))
            .build();
        List<KeyValue<String, String>> observes = cluster.observe(observeRequest);
        Assert.assertEquals(1, observes.size());
        Assert.assertEquals("Good Morning", observes.get(0).getValue());
        Assert.assertEquals("key1", observes.get(0).getKey());
    }

    @Test
    public void serviceSimpleOkWithCorrelationId() throws Exception {
        KeyValue<String, String> record = new KeyValue<>("key1", "{\"hour\": 5}");
        record.addHeader(KafkaHeaders.METHOD_NAME, "Hello", StandardCharsets.UTF_8);
        record.addHeader(KafkaHeaders.REPLY_TOPIC, "hello-reply-topic", StandardCharsets.UTF_8);
        record.addHeader(KafkaHeaders.CORRELATION_ID, "42", StandardCharsets.UTF_8);
        cluster.send(SendKeyValues.to("hello-in-topic-2", Collections.singletonList(record)).useDefaults());

        ObserveKeyValues<String, String> observeRequest = ObserveKeyValues.on("hello-reply-topic", 1)
            .with("metadata.max.age.ms", 1000)
            .with("group.id", "junit")
            .filterOnHeaders(e -> e.lastHeader(KafkaHeaders.CORRELATION_ID) != null && "42"
                .equals(toString(e.lastHeader(KafkaHeaders.CORRELATION_ID).value())))
            .build();
        List<KeyValue<String, String>> observes = cluster.observe(observeRequest);
        Assert.assertEquals(1, observes.size());
        Assert.assertEquals("Good Morning", observes.get(0).getValue());
        Assert.assertEquals("key1", observes.get(0).getKey());
    }

    private String getHeaderValue(KeyValue<?, ?> v, String key) {
        if (v.getHeaders().lastHeader(key) != null) {
            return new String(v.getHeaders().lastHeader(key).value(), StandardCharsets.UTF_8);
        }
        return null;
    }

    @Test
    public void testDltHeaders() throws Exception {
        cluster.createTopic(TopicConfig.forTopic("hello-replydlt-topic").withNumberOfPartitions(10).build());

        KeyValue<String, String> record = new KeyValue<>(null, "5");
        record.addHeader(KafkaHeaders.METHOD_NAME, "Hello", StandardCharsets.UTF_8);
        record.addHeader(KafkaHeaders.REPLY_TOPIC, "hello-reply-topic", StandardCharsets.UTF_8);
        record.addHeader(KafkaHeaders.REPLY_PARTITION, "891", StandardCharsets.UTF_8);
        record.addHeader(KafkaHeaders.REPLY_DLT_TOPIC, "hello-replydlt-topic", StandardCharsets.UTF_8);
        record.addHeader(KafkaHeaders.REPLY_DLT_PARTITION, "5", StandardCharsets.UTF_8);
        record.addHeader(KafkaHeaders.CORRELATION_ID, "42", StandardCharsets.UTF_8);
        cluster.send(SendKeyValues.to("hello-in-topic-2", Collections.singletonList(record)).useDefaults());

        ObserveKeyValues<String, String> observeRequest = ObserveKeyValues.on("hello-replydlt-topic", 1)
            .with("metadata.max.age.ms", 1000)
            .with("group.id", "junit")
            .with("auto.offset.reset", "earliest")
            .build();

        List<KeyValue<String, String>> observedValues = cluster.observe(observeRequest);
        KeyValue<String, String> v = observedValues.get(0);
        Assert.assertEquals(1, observedValues.size());
        Assert.assertEquals("5", v.getValue());

        Assert.assertEquals("42", getHeaderValue(v, KafkaHeaders.CORRELATION_ID));
        Assert.assertEquals("Hello", getHeaderValue(v, KafkaHeaders.METHOD_NAME));
        Assert.assertEquals("hello-reply-topic", getHeaderValue(v, KafkaHeaders.REPLY_TOPIC));
        Assert.assertEquals("891", getHeaderValue(v, KafkaHeaders.REPLY_PARTITION));
        Assert.assertEquals("org.openl.rules.ruleservice.kafka.ser.RequestMessageFormatException",
            getHeaderValue(v, KafkaHeaders.DLT_EXCEPTION_FQCN));
        Assert.assertEquals("Invalid message format.", getHeaderValue(v, KafkaHeaders.DLT_EXCEPTION_MESSAGE));
        Assert.assertNotNull(v.getHeaders().lastHeader(KafkaHeaders.DLT_ORIGINAL_OFFSET));
        Assert.assertNotNull(v.getHeaders().lastHeader(KafkaHeaders.DLT_ORIGINAL_PARTITION));
        Assert.assertNotNull(v.getHeaders().lastHeader(KafkaHeaders.DLT_EXCEPTION_STACKTRACE));
        Assert.assertEquals("hello-in-topic-2", getHeaderValue(v, KafkaHeaders.DLT_ORIGINAL_TOPIC));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
        cluster.stop();
    }

}