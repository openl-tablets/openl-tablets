package org.openl.itest;

import static org.apache.kafka.clients.consumer.ConsumerConfig.METADATA_MAX_AGE_CONFIG;
import static org.awaitility.Awaitility.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.openl.rules.ruleservice.kafka.KafkaHeaders.CORRELATION_ID;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;
import org.openl.rules.ruleservice.kafka.KafkaHeaders;

public class RunKafkaSmokeITest {
    private static JettyServer server;
    private static HttpClient client;

    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:latest")).withKraft();

    @BeforeAll
    public static void setUp() throws Exception {
        KAFKA_CONTAINER.start();

        server = JettyServer
            .start(Map.of("ruleservice.kafka.bootstrap.servers", KAFKA_CONTAINER.getBootstrapServers()));
        client = server.client();
    }

    @Test
    public void testRest() {
        client.send("simple1");
    }

    @Test
    public void methodSimpleOk() {
        var producerRecord = new ProducerRecord<>("hello-in-topic", "key1", "{\"hour\": 5}");
        addHeader(producerRecord, "X-Id", "Zulu");
        testKafka(producerRecord, "hello-out-topic", (response) -> {
            assertEquals("Good Morning", response.value());
            assertEquals(producerRecord.key(), response.key());
            assertEquals("Zulu", getHeaderValue(response, "X-Id"));
        });
    }

    @Test
    public void methodSimpleFail() {
        try (KafkaProducer<String, String> producer = createKafkaProducer(KAFKA_CONTAINER.getBootstrapServers());
                KafkaConsumer<String, String> consumer = createKafkaConsumer(KAFKA_CONTAINER.getBootstrapServers())) {
            consumer.subscribe(Collections.singletonList("hello-dlt-topic"));

            producer.send(new ProducerRecord<>("hello-in-topic", "key1", "5"));

            checkKafkaResponse(consumer, (response) -> {
                assertEquals(response.value(), "5");
                assertEquals(response.key(), "key1");
                assertEquals(36, getHeaderValue(response, "X-Id").length());
            });

            producer.send(new ProducerRecord<>("hello-in-topic", "key1", "{\"hour\": 22}"));
            checkKafkaResponse(consumer, (response) -> {
                assertEquals(response.value(), "{\"hour\": 22}");
                assertEquals(response.key(), "key1");
                assertEquals("fail", getHeaderValue(response, KafkaHeaders.DLT_EXCEPTION_MESSAGE));
            });

            consumer.unsubscribe();
        }
    }

    @Test
    public void serviceSimpleOk() {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>("hello-in-topic-2",
            "key1",
            "{\"hour\": 5}");
        addHeader(producerRecord, KafkaHeaders.METHOD_NAME, "Hello");
        testKafka(producerRecord, "hello-out-topic-2", (response) -> {
            assertEquals("Good Morning", response.value());
            assertEquals(producerRecord.key(), response.key());
            assertEquals(36, getHeaderValue(response, "X-Id").length());
        });
    }

    @Test
    public void serviceSimpleFail() {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>("hello-in-topic-2", "key1", "5");
        addHeader(producerRecord, KafkaHeaders.METHOD_NAME, "Hello");
        addHeader(producerRecord, "X-Id", "Yetty");
        testKafka(producerRecord, "hello-dlt-topic-2", (response) -> {
            assertEquals("5", response.value());
            assertEquals(producerRecord.key(), response.key());
            assertEquals("Yetty", getHeaderValue(response, "X-Id"));
        });
    }

    @Test
    public void methodSimpleOkWithReplyTopic() {
        final String replyTopic = UUID.randomUUID().toString();
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>("hello-in-topic", "key1", "{\"hour\": 5}");
        addHeader(producerRecord, KafkaHeaders.REPLY_TOPIC, replyTopic);
        addHeader(producerRecord, "X-Id", "x-ray");
        testKafka(producerRecord, replyTopic, (response) -> {
            assertEquals("Good Morning", response.value());
            assertEquals(producerRecord.key(), response.key());
            assertEquals("x-ray", getHeaderValue(response, "X-Id"));
        });
    }

    @Test
    public void serviceSimpleOkWithReplyTopic() {
        final String replyTopic = UUID.randomUUID().toString();
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>("hello-in-topic-2",
            "key1",
            "{\"hour\": 5}");
        addHeader(producerRecord, KafkaHeaders.METHOD_NAME, "Hello");
        addHeader(producerRecord, KafkaHeaders.REPLY_TOPIC, replyTopic);
        testKafka(producerRecord, replyTopic, "Good Morning");
    }

    @Test
    public void methodSimpleOkWithCorrelationId() {
        final String replyTopic = UUID.randomUUID().toString();
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>("hello-in-topic", "key1", "{\"hour\": 5}");
        addHeader(producerRecord, KafkaHeaders.REPLY_TOPIC, replyTopic);
        addHeader(producerRecord, CORRELATION_ID, "42");
        testKafka(producerRecord, replyTopic, "Good Morning");
    }

    @Test
    public void serviceSimpleOkWithCorrelationId() {
        final String replyTopic = UUID.randomUUID().toString();
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>("hello-in-topic-2",
            "key1",
            "{\"hour\": 5}");
        addHeader(producerRecord, KafkaHeaders.METHOD_NAME, "Hello");
        addHeader(producerRecord, KafkaHeaders.REPLY_TOPIC, replyTopic);
        addHeader(producerRecord, CORRELATION_ID, "42");
        testKafka(producerRecord, replyTopic, "Good Morning");
    }

    private static final String HELLO_REPLY_DLT_TOPIC = "hello-replydlt-topic";

    @Test
    public void testDltHeaders() throws Exception {
        try (KafkaProducer<String, String> producer = createKafkaProducer(KAFKA_CONTAINER.getBootstrapServers());
                KafkaConsumer<String, String> consumer = createKafkaConsumer(KAFKA_CONTAINER.getBootstrapServers())) {

            AdminClient adminClient = AdminClient
                .create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers()));
            Collection<NewTopic> topics = Collections.singletonList(new NewTopic(HELLO_REPLY_DLT_TOPIC, 10, (short) 1));
            adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS);

            final String replyTopic = UUID.randomUUID().toString();
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>("hello-in-topic-2", null, "5");
            addHeader(producerRecord, KafkaHeaders.METHOD_NAME, "Hello");
            addHeader(producerRecord, KafkaHeaders.REPLY_TOPIC, replyTopic);
            addHeader(producerRecord, KafkaHeaders.REPLY_PARTITION, "891");
            addHeader(producerRecord, KafkaHeaders.REPLY_DLT_PARTITION, "5");
            addHeader(producerRecord, KafkaHeaders.REPLY_DLT_TOPIC, HELLO_REPLY_DLT_TOPIC);
            addHeader(producerRecord, KafkaHeaders.CORRELATION_ID, "42");
            consumer.subscribe(Collections.singletonList(HELLO_REPLY_DLT_TOPIC));
            producer.send(producerRecord);

            checkKafkaResponse(consumer, (response) -> {
                assertEquals(response.value(), "5");

                assertEquals("42", getHeaderValue(response, KafkaHeaders.CORRELATION_ID));
                assertEquals("Hello", getHeaderValue(response, KafkaHeaders.METHOD_NAME));
                assertEquals(replyTopic, getHeaderValue(response, KafkaHeaders.REPLY_TOPIC));
                assertEquals("891", getHeaderValue(response, KafkaHeaders.REPLY_PARTITION));
                assertEquals("org.openl.rules.ruleservice.kafka.ser.RequestMessageFormatException",
                    getHeaderValue(response, KafkaHeaders.DLT_EXCEPTION_FQCN));
                assertEquals("Invalid message format.",
                    getHeaderValue(response, KafkaHeaders.DLT_EXCEPTION_MESSAGE));
                assertNotNull(response.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_OFFSET));
                assertNotNull(response.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_PARTITION));
                assertNotNull(response.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_STACKTRACE));
                assertEquals("hello-in-topic-2", getHeaderValue(response, KafkaHeaders.DLT_ORIGINAL_TOPIC));
            });
            consumer.unsubscribe();
        }
    }

    @AfterAll
    public static void tearDown() throws Exception {
        server.stop();
        KAFKA_CONTAINER.stop();
    }

    private static void addHeader(ProducerRecord<String, String> producerRecord, String key, String value) {
        producerRecord.headers().add(key, value.getBytes(StandardCharsets.UTF_8));
    }

    private static void testKafka(ProducerRecord<String, String> producerRecord,
            String outTopic,
            String expectedValue) {
        testKafka(producerRecord, outTopic, (response) -> {
            assertEquals(expectedValue, response.value());
            assertEquals(producerRecord.key(), response.key());
        });
    }

    private static void testKafka(ProducerRecord<String, String> producerRecord,
            String outTopic,
            Consumer<ConsumerRecord<String, String>> check ) {
        var servers = KAFKA_CONTAINER.getBootstrapServers();
        try (var producer = createKafkaProducer(servers); var consumer = createKafkaConsumer(servers)) {
            consumer.subscribe(Collections.singletonList(outTopic));
            producer.send(producerRecord);
            checkKafkaResponse(consumer, check);
            consumer.unsubscribe();
        }
    }

    private static void checkKafkaResponse(KafkaConsumer<String, String> consumer, Consumer<ConsumerRecord<String, String>> check) {
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

    private static KafkaProducer<String, String> createKafkaProducer(String bootstrapServers) {
        return new KafkaProducer<>(Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            bootstrapServers,
            ProducerConfig.CLIENT_ID_CONFIG,
            UUID.randomUUID().toString()), new StringSerializer(), new StringSerializer());
    }

    private static KafkaConsumer<String, String> createKafkaConsumer(String bootstrapServers) {
        return new KafkaConsumer<>(Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG,
            "junit",
            METADATA_MAX_AGE_CONFIG,
            1000,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
            "earliest"), new StringDeserializer(), new StringDeserializer());
    }

    private static String getHeaderValue(ConsumerRecord<String, String> response, String key) {
        if (response.headers().lastHeader(key) != null) {
            Header h = response.headers().lastHeader(key);
            if (h != null && h.value() != null) {
                return new String(h.value(), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

}
