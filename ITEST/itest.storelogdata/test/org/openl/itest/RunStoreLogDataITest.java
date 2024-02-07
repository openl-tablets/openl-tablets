package org.openl.itest;

import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.persistence.Entity;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException;
import com.datastax.oss.driver.api.core.servererrors.QueryExecutionException;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.h2.tools.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import org.openl.itest.cassandra.CassandraFields;
import org.openl.itest.cassandra.HelloEntity1;
import org.openl.itest.cassandra.HelloEntity2;
import org.openl.itest.cassandra.HelloEntity3;
import org.openl.itest.cassandra.HelloEntity4;
import org.openl.itest.cassandra.HelloEntity8;
import org.openl.itest.common.ExpectedLogValues;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;
import org.openl.itest.db.DBFields;
import org.openl.itest.db.HelloEntity9;
import org.openl.rules.ruleservice.kafka.KafkaHeaders;
import org.openl.rules.ruleservice.storelogdata.annotation.PublisherType;
import org.openl.rules.ruleservice.storelogdata.cassandra.DefaultCassandraEntity;
import org.openl.rules.ruleservice.storelogdata.db.DefaultEntity;

public class RunStoreLogDataITest {
    private static final Logger LOG = LoggerFactory.getLogger(RunStoreLogDataITest.class);

    public static final int POLL_INTERVAL_IN_MILLISECONDS = 500;
    private static final int AWAIT_TIMEOUT = 60;
    private static final String KEYSPACE = "openl_ws_logging";

    private static final String DEFAULT_TABLE_NAME = DefaultCassandraEntity.class.getAnnotation(CqlName.class).value();

    private static final String DEFAULT_H2_TABLE_NAME = DefaultEntity.class.getAnnotation(Entity.class).name();

    private static final String KAFKA_PUBLISHER_TYPE = PublisherType.KAFKA.name();
    private static final String RESTFUL_PUBLISHER_TYPE = PublisherType.RESTFUL.name();
    private static final String WEBSERVICE_PUBLISHER_TYPE = PublisherType.WEBSERVICE.name();

    private static JettyServer server;
    private static HttpClient client;
    private static Connection h2Connection;
    private static Server h2Server;

    private static final CassandraContainer CASSANDRA_CONTAINER = new CassandraContainer<>("cassandra:4");
    private static CqlSession cassandraSession;

    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:latest")).withKraft();

    @BeforeAll
    public static void setUp() throws Exception {
        h2Server = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-ifNotExists");
        h2Server.start();
        String dbUrl = "jdbc:h2:" + h2Server.getURL() + "/mem:mydb";
        h2Connection = DriverManager.getConnection(dbUrl);

        CASSANDRA_CONTAINER.start();
        InetSocketAddress contactPoint = CASSANDRA_CONTAINER.getContactPoint();
        cassandraSession = CqlSession.builder()
                .addContactPoint(contactPoint)
                .withLocalDatacenter(CASSANDRA_CONTAINER.getLocalDatacenter())
                .build();
        cassandraSession.execute("CREATE KEYSPACE IF NOT EXISTS " + KEYSPACE
                + " WITH replication = {'class':'SimpleStrategy','replication_factor':1};");

        KAFKA_CONTAINER.start();

        Map<String, String> params = Map.of("ruleservice.kafka.bootstrap.servers", KAFKA_CONTAINER.getBootstrapServers(),
                "datastax-java-driver.basic.contact-points.0", CASSANDRA_CONTAINER.getHost() + ":" + CASSANDRA_CONTAINER.getFirstMappedPort(),
                "hibernate.connection.url", dbUrl);
        server = JettyServer.start(params);
        client = server.client();

    }

    @Test
    public void testKafkaMethodServiceOk() {
        final String REQUEST = "{\"hour\": 5}";
        final String RESPONSE = "Good Morning";

        truncateCassandraTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);
        truncateH2TableIfExists(DEFAULT_H2_TABLE_NAME);

        testKafka("hello-in-topic", "hello-out-topic", null, REQUEST, RESPONSE);

        validateDatabases(REQUEST, RESPONSE, "Hello", "simple1", KAFKA_PUBLISHER_TYPE);
    }

    @Test
    public void testKafkaServiceOkWithNoOutputTopic() {
        final String REQUEST = "{\"hour\": 5}";

        truncateCassandraTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);
        truncateH2TableIfExists(DEFAULT_H2_TABLE_NAME);

        try (KafkaProducer<String, String> producer = createKafkaProducer(KAFKA_CONTAINER.getBootstrapServers())) {
            ProducerRecord<String, String> record = new ProducerRecord<>("hello-in-topic-5", null, REQUEST);
            record.headers().add(KafkaHeaders.METHOD_NAME, "Hello".getBytes(StandardCharsets.UTF_8));
            producer.send(record);
        }

        validateDatabases(REQUEST, null, true, "Hello", "simple5", KAFKA_PUBLISHER_TYPE);
    }

    @Test
    public void testKafkaMethodServiceFail() {
        final String REQUEST = "5";

        truncateCassandraTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);
        truncateH2TableIfExists(DEFAULT_H2_TABLE_NAME);

        testKafka("hello-in-topic", "hello-dlt-topic", null, REQUEST, null);

        validateDatabases(REQUEST, REQUEST, "Hello", "simple1", KAFKA_PUBLISHER_TYPE);
    }

    @Test
    public void testKafkaServiceOk() {
        final String REQUEST = "{\"hour\": 5}";
        final String RESPONSE = "Good Morning";

        truncateCassandraTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);
        truncateH2TableIfExists(DEFAULT_H2_TABLE_NAME);

        ProducerRecord<String, String> producerRecord = new ProducerRecord<>("hello-in-topic-2", null, REQUEST);
        producerRecord.headers().add(KafkaHeaders.METHOD_NAME, "Hello".getBytes(StandardCharsets.UTF_8));
        testKafka(producerRecord, "hello-out-topic-2", RESPONSE);

        validateDatabases(REQUEST, RESPONSE, "Hello", "simple2", KAFKA_PUBLISHER_TYPE);
    }

    @Test
    public void testKafkaServiceFail() {
        final String REQUEST = "5";
        final String RESPONSE = "5";

        truncateCassandraTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);
        truncateH2TableIfExists(DEFAULT_H2_TABLE_NAME);

        testKafka("hello-in-topic-2", "hello-dlt-topic-2", null, REQUEST, null);

        validateDatabases(REQUEST, RESPONSE, null, "simple2", KAFKA_PUBLISHER_TYPE);
    }

    @Test
    public void testSyncStoreFails() {
        client.send("simple7_Hello.json");
        client.send("simple7_Hello.gzip");
        truncateH2TableIfExists(getDBTableName(HelloEntity9.class));
        client.send("simple4_Hello3.post");
        client.send("simple4_Hello3.post.gzip");
        client.send("simple4_Hello3_fail.post");
    }

    @Test
    public void testRestServiceOk() {
        truncateCassandraTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);
        truncateH2TableIfExists(DEFAULT_H2_TABLE_NAME);

        client.send("simple3_Hello");

        validateDatabases("{\r\n  \"hour\": 5\r\n}\r\n", "Good Morning", "Hello", "simple3", RESTFUL_PUBLISHER_TYPE);
    }

    @Test
    public void testRestServiceFail() {
        truncateCassandraTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);
        truncateH2TableIfExists(DEFAULT_H2_TABLE_NAME);

        client.send("simple3_Hello_fail");

        validateDatabases("5\r\n", null, null, "simple3", RESTFUL_PUBLISHER_TYPE);
    }

    @Test
    public void testStoreLogDataAnnotations() throws Exception {
        final String REQUEST = "{\r\n  \"hour\": 5\r\n}\r\n";
        final String RESPONSE = "Good Morning";

        final String helloEntity1TableName = getCassandraTableName(HelloEntity1.class);
        final String helloEntity2TableName = getCassandraTableName(HelloEntity2.class);
        final String helloEntity3TableName = getCassandraTableName(HelloEntity3.class);
        final String helloEntity4TableName = getCassandraTableName(HelloEntity4.class);
        final String helloEntity8TableName = getCassandraTableName(HelloEntity8.class);

        truncateCassandraTableIfExists(KEYSPACE, helloEntity1TableName);
        truncateCassandraTableIfExists(KEYSPACE, helloEntity2TableName);
        truncateCassandraTableIfExists(KEYSPACE, helloEntity3TableName);
        truncateCassandraTableIfExists(KEYSPACE, helloEntity4TableName);
        truncateCassandraTableIfExists(KEYSPACE, helloEntity8TableName);

        final String h2HelloEntity1TableName = getDBTableName(org.openl.itest.db.HelloEntity1.class);
        final String h2HelloEntity2TableName = getDBTableName(org.openl.itest.db.HelloEntity2.class);
        final String h2HelloEntity3TableName = getDBTableName(org.openl.itest.db.HelloEntity3.class);
        final String h2HelloEntity4TableName = getDBTableName(org.openl.itest.db.HelloEntity4.class);
        final String h2HelloEntity8TableName = getDBTableName(org.openl.itest.db.HelloEntity8.class);

        truncateH2TableIfExists(h2HelloEntity1TableName);
        truncateH2TableIfExists(h2HelloEntity2TableName);
        truncateH2TableIfExists(h2HelloEntity3TableName);
        truncateH2TableIfExists(h2HelloEntity4TableName);
        truncateH2TableIfExists(h2HelloEntity8TableName);

        client.send("simple4_Hello");

        List<Row> rows = getCassandraRows(helloEntity1TableName);
        if (rows.size() == 0) { // Table is created but row is not created
            fail("Table is created but row is not created");
        }
        assertEquals(1, rows.size());
        Row row = rows.iterator().next();
        assertNotNull(row.getString(CassandraFields.ID));
        assertEquals(REQUEST, row.getString(CassandraFields.REQUEST));
        assertEquals(RESPONSE, row.getString(CassandraFields.RESPONSE));
        assertEquals("Hello", row.getString(CassandraFields.METHOD_NAME));
        assertEquals("simple4", row.getString(CassandraFields.SERVICE_NAME));
        assertNotNull(row.getInstant(CassandraFields.INCOMING_TIME));
        assertNotNull(row.getInstant(CassandraFields.OUTCOMING_TIME));
        assertEquals(RESTFUL_PUBLISHER_TYPE, row.getString(CassandraFields.PUBLISHER_TYPE));

        assertEquals("value1", row.getString(CassandraFields.VALUE));
        assertEquals(5, row.getInt(CassandraFields.HOUR));
        assertEquals("Good Morning", row.getString(CassandraFields.RESULT));
        assertTrue(row.getBool(CassandraFields.AWARE_INSTANCES_FOUND));
        assertTrue(row.getBool(CassandraFields.CASSANDRA_SESSION_FOUND));

        rows = getCassandraRows(helloEntity2TableName);
        if (rows.size() == 0) { // Table is created but row is not created
            fail("Table is created but row is not created");
        }
        assertEquals(1, rows.size());
        row = rows.iterator().next();
        assertNotNull(row.getString(CassandraFields.ID));
        assertEquals(REQUEST, row.getString(CassandraFields.REQUEST));
        assertEquals(RESPONSE, row.getString(CassandraFields.RESPONSE));
        assertEquals("Hello", row.getString(CassandraFields.METHOD_NAME));
        assertEquals("simple4", row.getString(CassandraFields.SERVICE_NAME));
        assertNotNull(row.getInstant(CassandraFields.INCOMING_TIME));
        assertNotNull(row.getInstant(CassandraFields.OUTCOMING_TIME));
        assertEquals(RESTFUL_PUBLISHER_TYPE, row.getString(CassandraFields.PUBLISHER_TYPE));

        assertEquals("value1", row.getString(CassandraFields.VALUE));
        assertEquals(5, row.getInt(CassandraFields.HOUR));
        assertEquals("Good Morning", row.getString(CassandraFields.RESULT));

        rows = getCassandraRows(helloEntity3TableName);
        if (rows.size() == 0) { // Table is created but row is not created
            fail("Table is created but row is not created");
        }
        assertEquals(1, rows.size());
        row = rows.iterator().next();

        assertNotNull(row.getString(CassandraFields.ID));
        assertNull(row.getString(CassandraFields.REQUEST));
        assertNull(row.getString(CassandraFields.RESPONSE));
        assertNull(row.getString(CassandraFields.METHOD_NAME));
        assertEquals("simple4", row.getString(CassandraFields.SERVICE_NAME));
        assertNull(row.getInstant(CassandraFields.INCOMING_TIME));
        assertNull(row.getInstant(CassandraFields.OUTCOMING_TIME));
        assertEquals(RESTFUL_PUBLISHER_TYPE, row.getString(CassandraFields.PUBLISHER_TYPE));

        assertNull(row.getString(CassandraFields.VALUE));
        assertNull(row.getString(CassandraFields.RESULT));

        assertTrue(cassandraSession
                .getMetadata()
                .getKeyspace(KEYSPACE)
                .get()
                .getTable(helloEntity4TableName).isEmpty());

        assertTrue(cassandraSession
                .getMetadata()
                .getKeyspace(KEYSPACE)
                .get()
                .getTable(helloEntity8TableName).isEmpty());
        // H2
        String query = "SELECT * FROM " + h2HelloEntity1TableName;
        try (Statement stmt = h2Connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            int count = 0;
            while (rs.next()) {
                count++;
                if (count > 1) {
                    break;
                }
                assertEquals(REQUEST, rs.getString(DBFields.REQUEST));
                assertEquals(RESPONSE, rs.getString(DBFields.RESPONSE));
                assertEquals("Hello", rs.getString(DBFields.METHOD_NAME));
                assertEquals("simple4", rs.getString(DBFields.SERVICE_NAME));
                assertNotNull(rs.getTimestamp(DBFields.INCOMING_TIME));
                assertNotNull(rs.getTimestamp(DBFields.OUTCOMING_TIME));
                assertEquals(RESTFUL_PUBLISHER_TYPE, rs.getString(DBFields.PUBLISHER_TYPE));

                assertEquals("value1", rs.getString(DBFields.VALUE));
                assertEquals(5, rs.getInt(DBFields.HOUR));
                assertEquals("Good Morning", rs.getString(DBFields.RESULT));
                assertTrue(rs.getBoolean(DBFields.AWARE_INSTANCES_FOUND));
                assertTrue(rs.getBoolean(DBFields.DB_CONNECTION_FOUND));
            }
            assertEquals(1, count);
        }
        query = "SELECT * FROM " + h2HelloEntity2TableName;
        try (Statement stmt = h2Connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            int count = 0;
            while (rs.next()) {
                count++;
                if (count > 1) {
                    break;
                }
                assertEquals(REQUEST, rs.getString(DBFields.REQUEST));
                assertEquals(RESPONSE, rs.getString(DBFields.RESPONSE));
                assertEquals("Hello", rs.getString(DBFields.METHOD_NAME));
                assertEquals("simple4", rs.getString(DBFields.SERVICE_NAME));
                assertNotNull(rs.getTimestamp(DBFields.INCOMING_TIME));
                assertNotNull(rs.getTimestamp(DBFields.OUTCOMING_TIME));
                assertEquals(RESTFUL_PUBLISHER_TYPE, rs.getString(DBFields.PUBLISHER_TYPE));

                assertEquals("value1", rs.getString(DBFields.VALUE));
                assertEquals(5, rs.getInt(DBFields.HOUR));
                assertEquals("Good Morning", rs.getString(DBFields.RESULT));
            }
            assertEquals(1, count);
        }
        query = "SELECT * FROM " + h2HelloEntity3TableName;
        try (Statement stmt = h2Connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            int count = 0;
            while (rs.next()) {
                count++;
                if (count > 1) {
                    break;
                }
                assertNull(rs.getString(DBFields.REQUEST));
                assertNull(rs.getString(DBFields.RESPONSE));
                assertNull(rs.getString(DBFields.METHOD_NAME));
                assertEquals("simple4", rs.getString(DBFields.SERVICE_NAME));
                assertNull(rs.getTimestamp(DBFields.INCOMING_TIME));
                assertNull(rs.getTimestamp(DBFields.OUTCOMING_TIME));
                assertEquals(RESTFUL_PUBLISHER_TYPE, rs.getString(DBFields.PUBLISHER_TYPE));

                assertNull(rs.getString(DBFields.VALUE));
                assertNull(rs.getString(DBFields.RESULT));
            }
            assertEquals(1, count);
        }

        ResultSet rs = h2Connection.getMetaData().getTables(null, null, h2HelloEntity4TableName, null);
        if (rs.next()) {
            fail();
        }
        ResultSet rs1 = h2Connection.getMetaData().getTables(null, null, h2HelloEntity8TableName, null);
        if (rs1.next()) {
            fail();
        }
    }

    @Test
    public void testStoreLogDataAnnotationsAdvanced() {
        final String REQUEST = "{\r\n  \"hour\": 5\r\n}\r\n";
        final String RESPONSE = "I don't know";

        final String helloEntity1TableName = getCassandraTableName(HelloEntity1.class);
        final String h2HelloEntity1TableName = getDBTableName(org.openl.itest.db.HelloEntity1.class);

        truncateCassandraTableIfExists(KEYSPACE, helloEntity1TableName);
        truncateH2TableIfExists(h2HelloEntity1TableName);

        client.send("simple4_Hello2");

        given().ignoreException(InvalidQueryException.class)
                .await()
                .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    List<Row> rows = getCassandraRows(helloEntity1TableName);
                    if (rows.size() == 0) { // Table is created but row is not created
                        return false;
                    }
                    assertEquals(1, rows.size());
                    Row row = rows.iterator().next();
                    assertNotNull(row.getString(CassandraFields.ID));
                    assertEquals(REQUEST, row.getString(CassandraFields.REQUEST));
                    assertEquals(RESPONSE, row.getString(CassandraFields.RESPONSE));
                    assertEquals("Hello2", row.getString(CassandraFields.METHOD_NAME));
                    assertEquals("simple4", row.getString(CassandraFields.SERVICE_NAME));
                    assertNotNull(row.getInstant(CassandraFields.INCOMING_TIME));
                    assertNotNull(row.getInstant(CassandraFields.OUTCOMING_TIME));
                    assertEquals(RESTFUL_PUBLISHER_TYPE, row.getString(CassandraFields.PUBLISHER_TYPE));

                    assertEquals(5, row.getInt(CassandraFields.INT_VALUE1));
                    assertEquals(22, row.getInt(CassandraFields.INT_VALUE2));
                    assertEquals(22, row.getInt(CassandraFields.INT_VALUE3));
                    assertEquals("Good Night", row.getString(CassandraFields.STRING_VALUE1));
                    assertEquals("Good Night", row.getString(CassandraFields.STRING_VALUE2));
                    assertEquals("Good Night", row.getString(CassandraFields.STRING_VALUE3));
                    assertTrue(row.getBool(CassandraFields.BOOL_VALUE1));
                    assertEquals("22", row.getString(CassandraFields.INT_VALUE_TO_STRING));
                    return true;
                }, equalTo(true));

        given().ignoreExceptions()
                .await()
                .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
                .until(() -> {
                    final String query = "SELECT * FROM " + h2HelloEntity1TableName;
                    try (Statement stmt = h2Connection.createStatement()) {
                        ResultSet rs = stmt.executeQuery(query);
                        int count = 0;
                        while (rs.next()) {
                            count++;
                            if (count > 1) {
                                break;
                            }
                            assertEquals(REQUEST, rs.getString(DBFields.REQUEST));
                            assertEquals(RESPONSE, rs.getString(DBFields.RESPONSE));
                            assertEquals("Hello2", rs.getString(DBFields.METHOD_NAME));
                            assertEquals("simple4", rs.getString(DBFields.SERVICE_NAME));
                            assertNotNull(rs.getTimestamp(DBFields.INCOMING_TIME));
                            assertNotNull(rs.getTimestamp(DBFields.OUTCOMING_TIME));
                            assertEquals(RESTFUL_PUBLISHER_TYPE, rs.getString(DBFields.PUBLISHER_TYPE));

                            assertEquals(5, rs.getInt(DBFields.INT_VALUE1));
                            assertEquals(22, rs.getInt(DBFields.INT_VALUE2));
                            assertEquals(22, rs.getInt(DBFields.INT_VALUE3));
                            assertEquals("Good Night", rs.getString(DBFields.STRING_VALUE1));
                            assertEquals("Good Night", rs.getString(DBFields.STRING_VALUE2));
                            assertEquals("Good Night", rs.getString(DBFields.STRING_VALUE3));
                            assertTrue(rs.getBoolean(DBFields.BOOL_VALUE1));
                            assertEquals("22", rs.getString(DBFields.INT_VALUE_TO_STRING));
                        }
                        assertEquals(1, count);
                        return true;
                    }
                }, equalTo(true));
    }

    private static String getCassandraTableName(Class<?> entityClass) {
        CqlName cqlName = entityClass.getAnnotation(CqlName.class);
        if (cqlName != null) {
            return cqlName.value();
        }
        throw new IllegalStateException("Only @CqlName annotated classes are supported.");
    }

    private static String getDBTableName(Class<?> entityClass) {
        Entity entity = entityClass.getAnnotation(Entity.class);
        if (entity != null) {
            return entity.name();
        }
        throw new IllegalStateException("Only @Entity annotated classes are supported.");
    }

    @Test
    public void testKafkaHeaderAnnotations() {
        final String REQUEST = "{\r\n  \"hour\": 5\r\n}\r\n";
        final String RESPONSE = "Good Morning";

        final String helloEntity1TableName = getCassandraTableName(HelloEntity1.class);
        final String helloEntity2TableName = getCassandraTableName(HelloEntity2.class);
        final String helloEntity3TableName = getCassandraTableName(HelloEntity3.class);
        final String helloEntity4TableName = getCassandraTableName(HelloEntity4.class);

        truncateCassandraTableIfExists(KEYSPACE, helloEntity1TableName);
        truncateCassandraTableIfExists(KEYSPACE, helloEntity2TableName);
        truncateCassandraTableIfExists(KEYSPACE, helloEntity3TableName);
        truncateCassandraTableIfExists(KEYSPACE, helloEntity4TableName);

        final String h2HelloEntity1TableName = getDBTableName(org.openl.itest.db.HelloEntity1.class);
        final String h2HelloEntity2TableName = getDBTableName(org.openl.itest.db.HelloEntity2.class);
        final String h2HelloEntity3TableName = getDBTableName(org.openl.itest.db.HelloEntity3.class);
        final String h2HelloEntity4TableName = getDBTableName(org.openl.itest.db.HelloEntity4.class);

        truncateH2TableIfExists(h2HelloEntity1TableName);
        truncateH2TableIfExists(h2HelloEntity2TableName);
        truncateH2TableIfExists(h2HelloEntity3TableName);
        truncateH2TableIfExists(h2HelloEntity4TableName);

        ProducerRecord<String, String> producerRecord = new ProducerRecord<>("hello-in-topic-4", null, REQUEST);
        producerRecord.headers().add(KafkaHeaders.METHOD_NAME, "Hello".getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add(KafkaHeaders.METHOD_PARAMETERS, "*, *".getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add("testHeader", "testHeaderValue".getBytes(StandardCharsets.UTF_8));
        testKafka(producerRecord, "hello-out-topic-4", RESPONSE);

        given().ignoreException(InvalidQueryException.class)
                .await()
                .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    List<Row> rows = getCassandraRows(helloEntity1TableName);
                    if (rows.size() == 0) { // Table is created but row is not created
                        return false;
                    }
                    assertEquals(1, rows.size());
                    Row row = rows.iterator().next();
                    assertNotNull(row.getString(CassandraFields.ID));
                    assertEquals(REQUEST, row.getString(CassandraFields.REQUEST));
                    assertEquals(RESPONSE, row.getString(CassandraFields.RESPONSE));
                    assertEquals("Hello", row.getString(CassandraFields.METHOD_NAME));
                    assertEquals("simple4", row.getString(CassandraFields.SERVICE_NAME));
                    assertNotNull(row.getInstant(CassandraFields.INCOMING_TIME));
                    assertNotNull(row.getInstant(CassandraFields.OUTCOMING_TIME));
                    assertEquals(PublisherType.KAFKA.toString(), row.getString(CassandraFields.PUBLISHER_TYPE));

                    assertEquals("Hello", row.getString(CassandraFields.HEADER1));
                    assertEquals("testHeaderValue", row.getString(CassandraFields.HEADER2));

                    return true;
                }, equalTo(true));

        given().ignoreException(InvalidQueryException.class)
                .await()
                .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    final String query = "SELECT * FROM " + h2HelloEntity1TableName;
                    try (Statement stmt = h2Connection.createStatement()) {
                        ResultSet rs = stmt.executeQuery(query);
                        int count = 0;
                        while (rs.next()) {
                            count++;
                            if (count > 1) {
                                break;
                            }
                            assertEquals(REQUEST, rs.getString(DBFields.REQUEST));
                            assertEquals(RESPONSE, rs.getString(DBFields.RESPONSE));
                            assertEquals("Hello", rs.getString(DBFields.METHOD_NAME));
                            assertEquals("simple4", rs.getString(DBFields.SERVICE_NAME));
                            assertNotNull(rs.getTimestamp(DBFields.INCOMING_TIME));
                            assertNotNull(rs.getTimestamp(DBFields.OUTCOMING_TIME));
                            assertEquals(PublisherType.KAFKA.toString(), rs.getString(DBFields.PUBLISHER_TYPE));

                            assertEquals("Hello", rs.getString(DBFields.HEADER1));
                            assertEquals("testHeaderValue", rs.getString(DBFields.HEADER2));
                        }
                        assertEquals(1, count);
                        return true;
                    }
                }, equalTo(true));

    }

    private void testKafka(String inTopic, String outTopic, String key, String value, String expectedValue) {
        testKafka(new ProducerRecord<>(inTopic, key, value), outTopic, expectedValue);
    }

    private void testKafka(ProducerRecord<String, String> producerRecord, String outTopic, String expectedValue) {
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

    private void checkKafkaResponse(KafkaConsumer<String, String> consumer, Consumer<ConsumerRecord<String, String>> check) {
        given().ignoreExceptions().await().atMost(20, TimeUnit.SECONDS).until(() -> {
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

    @Test
    public void testStoreAndGetFromCassandra() {
        client.send("simple6_openapi.json.get");

        client.send("simple6_Hello1.post");
        client.send("simple6_ResponseById1.get");

        client.send("simple6_Hello2.post");
        client.send("simple6_ResponseById2.get");

        client.send("simple6_Hello3.post");
        client.send("simple6_ResponseById3.get");

        client.send("simple6_Hello4.post");
        client.send("simple6_ResponseById4.get");

        client.send("simple6_DoSomethingExtra.get");

        client.send("simple6_AlwaysThrowExceptionAfterCall.get");
        client.send("simple6_AlwaysThrowExceptionBeforeCall.get");
    }

    private interface Procedure {
        void invoke();
    }

    private static void doQuite(Procedure procedure) {
        try {
            procedure.invoke();
        } catch (RuntimeException e) {
            LOG.warn("Error when trying to close server", e);
        }
    }

    @AfterAll
    public static void tearDown() throws Exception {

        server.stop();

        doQuite(cassandraSession::close);
        doQuite(CASSANDRA_CONTAINER::stop);
        doQuite(KAFKA_CONTAINER::stop);

        h2Server.stop();
    }

    private static void validateDatabases(String REQUEST, String RESPONSE, boolean isResponseProvided, String methodName, String serviceName, String publisherType) {
        ExpectedLogValues values = new ExpectedLogValues(REQUEST,
                RESPONSE,
                methodName,
                serviceName,
                publisherType);
        values.setResponseProvided(isResponseProvided);
        given().ignoreException(InvalidQueryException.class)
                .await()
                .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
                .until(validateCassandra(values), equalTo(true));

        given().ignoreExceptions()
                .await()
                .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
                .until(validateH2(values), equalTo(true));
    }

    private static void validateDatabases(String REQUEST, String RESPONSE, String methodName, String serviceName, String publisherType) {
        validateDatabases(REQUEST, RESPONSE, RESPONSE != null, methodName, serviceName, publisherType);
    }

    private static KafkaProducer<String, String> createKafkaProducer(String bootstrapServers) {
        return new KafkaProducer<>(
                Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString()
                ),
                new StringSerializer(),
                new StringSerializer()
        );
    }

    private static KafkaConsumer<String, String> createKafkaConsumer(String bootstrapServers) {
        return new KafkaConsumer<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ConsumerConfig.GROUP_ID_CONFIG, "tc-" + UUID.randomUUID(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
                ),
                new StringDeserializer(),
                new StringDeserializer()
        );
    }

    private static void truncateCassandraTableIfExists(final String keyspace, final String table) {
        try {
            cassandraSession.execute("TRUNCATE " + keyspace + "." + table);
        } catch (QueryExecutionException | InvalidQueryException ignored) {
        }
    }

    private static void truncateH2TableIfExists(final String table) {
        try {
            CallableStatement statement = h2Connection.prepareCall("TRUNCATE TABLE " + table);
            statement.execute();
        } catch (SQLException ignored) {
        }
    }

    private static List<Row> getCassandraRows(final String tableName) {
        return cassandraSession.execute("SELECT * FROM " + KEYSPACE + "." + tableName).all();
    }

    private static Callable<Boolean> validateCassandra(final ExpectedLogValues input) {
        return () -> {
            List<Row> rows = getCassandraRows(DEFAULT_TABLE_NAME);
            if (rows.size() == 0) { // Table is created but row is not created
                return false;
            }
            assertEquals(1, rows.size());
            Row row = rows.iterator().next();

            assertNotNull(row.getString(CassandraFields.ID));
            assertEquals(input.getRequest(), row.getString(CassandraFields.REQUEST));
            assertEquals(input.getMethodName(), row.getString(CassandraFields.METHOD_NAME));
            assertEquals(input.getServiceName(), row.getString(CassandraFields.SERVICE_NAME));
            String publisherType = input.getPublisherType();
            assertEquals(publisherType, row.getString(CassandraFields.PUBLISHER_TYPE));
            if (publisherType.equals(RESTFUL_PUBLISHER_TYPE) || publisherType.equals(WEBSERVICE_PUBLISHER_TYPE)) {
                assertNotNull(row.getString(CassandraFields.URL));
            }
            String value = row.getString(CassandraFields.RESPONSE);
            if (input.isResponseProvided()) {
                String expected = input.getResponse();
                assertEquals(expected, value);
            } else {
                assertNotNull(value);
            }
            assertNotNull(row.getInstant(CassandraFields.INCOMING_TIME));
            assertNotNull(row.getInstant(CassandraFields.OUTCOMING_TIME));
            return true;
        };
    }

    private static Callable<Boolean> validateH2(final ExpectedLogValues input) {
        return () -> {
            final String query = "SELECT * FROM " + DEFAULT_H2_TABLE_NAME;
            try (Statement stmt = h2Connection.createStatement()) {
                ResultSet rs = stmt.executeQuery(query);
                int count = 0;
                while (rs.next()) {
                    count++;
                    assertNotNull(rs.getString(DBFields.ID));
                    assertEquals(input.getRequest(), rs.getString(DBFields.REQUEST));
                    assertEquals(input.getMethodName(), rs.getString(DBFields.METHOD_NAME));
                    assertEquals(input.getServiceName(), rs.getString(DBFields.SERVICE_NAME));
                    String publisherType = input.getPublisherType();
                    assertEquals(publisherType, rs.getString(DBFields.PUBLISHER_TYPE));
                    if (publisherType.equals(RESTFUL_PUBLISHER_TYPE) || publisherType
                            .equals(WEBSERVICE_PUBLISHER_TYPE)) {
                        assertNotNull(rs.getString(DBFields.URL));
                    }
                    String value = rs.getString(DBFields.RESPONSE);
                    if (input.isResponseProvided()) {
                        String expected = input.getResponse();
                        assertEquals(expected, value);
                    } else {
                        assertNotNull(value);
                    }
                    assertNotNull(rs.getTimestamp(DBFields.INCOMING_TIME));
                    assertNotNull(rs.getTimestamp(DBFields.OUTCOMING_TIME));
                }
                assertEquals(1, count);
                return true;
            } catch (SQLException e) {
                return false;
            }
        };
    }

}
