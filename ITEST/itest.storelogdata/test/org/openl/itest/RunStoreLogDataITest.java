package org.openl.itest;

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.IOUtils;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.cassandra.HelloEntity1;
import org.openl.itest.cassandra.HelloEntity2;
import org.openl.itest.cassandra.HelloEntity3;
import org.openl.itest.cassandra.HelloEntity4;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;
import org.openl.rules.ruleservice.kafka.KafkaHeaders;
import org.openl.rules.ruleservice.storelogdata.annotation.PublisherType;
import org.openl.rules.ruleservice.storelogdata.cassandra.DefaultCassandraEntity;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;

import net.mguenther.kafka.junit.*;

public class RunStoreLogDataITest {
    // private static final int TIMEOUT = Integer.MAX_VALUE;
    private static final int AWAIT_TIMEOUT = 60;
    private static final String KEYSPACE = "openl_ws_logging";

    private static final String DEFAULT_TABLE_NAME = DefaultCassandraEntity.class.getAnnotation(CqlName.class).value();

    private static JettyServer server;
    private static HttpClient client;
    private static EmbeddedKafkaCluster cluster;

    private static void createKeyspaceIfNotExists(Session session,
            String keyspaceName,
            String replicationStrategy,
            int replicationFactor) {
        StringBuilder sb = new StringBuilder("CREATE KEYSPACE IF NOT EXISTS ").append(keyspaceName)
            .append(" WITH replication = {")
            .append("'class':'")
            .append(replicationStrategy)
            .append("','replication_factor':")
            .append(replicationFactor)
            .append("};");

        String query = sb.toString();
        session.execute(query);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra(EmbeddedCassandraServerHelper.CASSANDRA_RNDPORT_YML_FILE);
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();

        System.setProperty("datastax-java-driver.basic.contact-points.0",
            EmbeddedCassandraServerHelper.getHost() + ":" + EmbeddedCassandraServerHelper.getNativeTransportPort());
        System.setProperty("datastax-java-driver.basic.load-balancing-policy.local-datacenter", "datacenter1");

        createKeyspaceIfNotExists(EmbeddedCassandraServerHelper.getSession(), KEYSPACE, "SimpleStrategy", 1);

        cluster = provisionWith(EmbeddedKafkaClusterConfig.create()
            .provisionWith(EmbeddedKafkaConfig.create().with("listeners", "PLAINTEXT://:61099").build())
            .build());
        cluster.start();

        server = new JettyServer(true);
        server.start();

        client = server.client();
    }

    private boolean truncateTableIfExists(final String keyspace, final String table) {
        try {
            EmbeddedCassandraServerHelper.getSession().execute("TRUNCATE " + keyspace + "." + table);
            return true;
        } catch (QueryExecutionException | InvalidQueryException e) {
            return false;
        }
    }

    @Test
    public void testKafkaMethodServiceOk() throws Exception {
        final String REQUEST = "{\"hour\": 5}";
        final String RESPONSE = "Good Morning";

        truncateTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);

        KeyValue<String, String> record = new KeyValue<>(null, REQUEST);
        cluster.send(SendKeyValues.to("hello-in-topic", Collections.singletonList(record)).useDefaults());
        ObserveKeyValues<String, String> observeRequest = ObserveKeyValues.on("hello-out-topic", 1)
            .with("metadata.max.age.ms", 1000)
            .build();
        List<String> observedValues = cluster.observeValues(observeRequest);
        assertEquals(1, observedValues.size());
        assertEquals(RESPONSE, observedValues.get(0));

        Awaitility.given()
            .ignoreException(InvalidQueryException.class)
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .until(() -> {
                ResultSet resultSet = EmbeddedCassandraServerHelper.getSession()
                    .execute("SELECT * FROM " + KEYSPACE + "." + DEFAULT_TABLE_NAME);
                List<Row> rows = resultSet.all();
                if (rows.size() == 0) { // Table is created but row is not created
                    return false;
                }
                assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                assertNotNull(row.getString("id"));
                assertEquals(REQUEST, row.getString("request"));
                assertEquals(RESPONSE, row.getString("response"));
                assertEquals("Hello", row.getString("method_name"));
                assertEquals("simple1", row.getString("service_name"));
                assertNotNull(row.getTimestamp("incoming_time"));
                assertNotNull(row.getTimestamp("outcoming_time"));
                assertEquals(PublisherType.KAFKA.toString(), row.getString("publisher_type"));

                return true;
            }, equalTo(true));
    }

    @Test
    public void testKafkaMethodServiceFail() throws Exception {
        final String REQUEST = "5";
        final String RESPONSE = REQUEST;

        truncateTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);

        KeyValue<String, String> record1 = new KeyValue<>(null, REQUEST);
        cluster.send(SendKeyValues.to("hello-in-topic", Collections.singletonList(record1)).useDefaults());

        ObserveKeyValues<String, String> observeRequestDlt = ObserveKeyValues.on("hello-dlt-topic", 1)
            .with("metadata.max.age.ms", 1000)
            .build();
        List<String> observedValuesDlt = cluster.observeValues(observeRequestDlt);
        assertEquals(1, observedValuesDlt.size());

        Awaitility.given()
            .ignoreException(InvalidQueryException.class)
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .until(() -> {
                ResultSet resultSet = EmbeddedCassandraServerHelper.getSession()
                    .execute("SELECT * FROM " + KEYSPACE + "." + DEFAULT_TABLE_NAME);
                List<Row> rows = resultSet.all();
                if (rows.size() == 0) { // Table is created but row is not created
                    return false;
                }
                assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                assertNotNull(row.getString("id"));
                assertEquals(REQUEST, row.getString("request"));
                assertEquals(RESPONSE, row.getString("response"));
                assertEquals("Hello", row.getString("method_name"));
                assertEquals("simple1", row.getString("service_name"));
                assertNotNull(row.getTimestamp("incoming_time"));
                assertNotNull(row.getTimestamp("outcoming_time"));
                assertEquals(PublisherType.KAFKA.toString(), row.getString("publisher_type"));
                return true;
            }, equalTo(true));
    }

    @Test
    public void testKafkaServiceOk() throws Exception {
        final String REQUEST = "{\"hour\": 5}";
        final String RESPONSE = "Good Morning";

        final String METHOD_NAME = "Hello";

        truncateTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);

        KeyValue<String, String> record = new KeyValue<>(null, REQUEST);
        record.addHeader(KafkaHeaders.METHOD_NAME, METHOD_NAME, Charset.forName("UTF8"));
        cluster.send(SendKeyValues.to("hello-in-topic-2", Collections.singletonList(record)).useDefaults());

        ObserveKeyValues<String, String> observeRequest = ObserveKeyValues.on("hello-out-topic-2", 1)
            .with("metadata.max.age.ms", 1000)
            .build();
        List<String> observedValues = cluster.observeValues(observeRequest);
        assertEquals(1, observedValues.size());
        assertEquals(RESPONSE, observedValues.get(0));

        Awaitility.given()
            .ignoreException(InvalidQueryException.class)
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .until(() -> {
                ResultSet resultSet = EmbeddedCassandraServerHelper.getSession()
                    .execute("SELECT * FROM " + KEYSPACE + "." + DEFAULT_TABLE_NAME);
                List<Row> rows = resultSet.all();
                if (rows.size() == 0) { // Table is created but row is not created
                    return false;
                }
                assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                assertNotNull(row.getString("id"));
                assertEquals(REQUEST, row.getString("request"));
                assertEquals(RESPONSE, row.getString("response"));
                assertEquals(METHOD_NAME, row.getString("method_name"));
                assertEquals("simple2", row.getString("service_name"));
                assertNotNull(row.getTimestamp("incoming_time"));
                assertNotNull(row.getTimestamp("outcoming_time"));
                assertEquals(PublisherType.KAFKA.toString(), row.getString("publisher_type"));

                return true;
            }, equalTo(true));
    }

    @Test
    public void testKafkaServiceWithCassandra10000Ok() throws Exception {
        final String REQUEST = "{\"hour\": 5}";
        final String METHOD_NAME = "Hello";
        final int COUNT_OF_RECORDS = 10000;

        truncateTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);
        List<KeyValue<String, String>> records = IntStream.range(0, COUNT_OF_RECORDS).mapToObj(e -> {
            KeyValue<String, String> record = new KeyValue<>(null, REQUEST);
            record.addHeader(KafkaHeaders.METHOD_NAME, METHOD_NAME, Charset.forName("UTF8"));
            return record;
        }).collect(Collectors.toList());
        cluster.send(SendKeyValues.to("hello-in-topic-2", records).useDefaults());

        Awaitility.given()
            .ignoreException(InvalidQueryException.class)
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .pollInterval(Duration.ONE_SECOND)
            .until(() -> {
                ResultSet resultSet = EmbeddedCassandraServerHelper.getSession()
                    .execute("SELECT count(*) FROM " + KEYSPACE + "." + DEFAULT_TABLE_NAME);
                Row row = resultSet.one();
                if (row.getLong(0) != COUNT_OF_RECORDS) { // Table is created but rows is not created
                    return false;
                }
                return true;
            }, equalTo(true));
    }

    @Test
    public void testKafkaServiceFail() throws Exception {
        final String REQUEST = "5";
        final String RESPONSE = "5";

        final String METHOD_NAME = "Hello";

        truncateTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);

        KeyValue<String, String> record = new KeyValue<>(null, "5");
        record.addHeader(KafkaHeaders.METHOD_NAME, METHOD_NAME, Charset.forName("UTF8"));
        cluster.send(SendKeyValues.to("hello-in-topic-2", Collections.singletonList(record)).useDefaults());

        ObserveKeyValues<String, String> observeRequestDlt = ObserveKeyValues.on("hello-dlt-topic-2", 1)
            .with("metadata.max.age.ms", 1000)
            .build();
        List<String> observedValuesDlt = cluster.observeValues(observeRequestDlt);
        assertEquals(1, observedValuesDlt.size());

        Awaitility.given()
            .ignoreException(InvalidQueryException.class)
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .until(() -> {
                ResultSet resultSet = EmbeddedCassandraServerHelper.getSession()
                    .execute("SELECT * FROM " + KEYSPACE + "." + DEFAULT_TABLE_NAME);
                List<Row> rows = resultSet.all();
                if (rows.size() == 0) { // Table is created but row is not created
                    return false;
                }
                assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                assertNotNull(row.getString("id"));
                assertEquals(REQUEST, row.getString("request"));
                assertEquals(RESPONSE, row.getString("response"));
                assertEquals(METHOD_NAME, row.getString("method_name"));
                assertEquals("simple2", row.getString("service_name"));
                assertNotNull(row.getTimestamp("incoming_time"));
                assertNotNull(row.getTimestamp("outcoming_time"));
                assertEquals(PublisherType.KAFKA.toString(), row.getString("publisher_type"));

                return true;
            }, equalTo(true));
    }

    @Test
    public void testRestServiceOk() throws Exception {
        final String REQUEST = IOUtils.toString(
            Thread.currentThread().getContextClassLoader().getResourceAsStream("simple3_Hello.req.json"),
            StandardCharsets.UTF_8);
        final String RESPONSE = IOUtils.toString(
            Thread.currentThread().getContextClassLoader().getResourceAsStream("simple3_Hello.resp.txt"),
            StandardCharsets.UTF_8);

        truncateTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);

        client.post("/REST/deployment3/simple3/Hello", "/simple3_Hello.req.json", "/simple3_Hello.resp.txt");

        Awaitility.given()
            .ignoreException(InvalidQueryException.class)
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .until(() -> {
                ResultSet resultSet = EmbeddedCassandraServerHelper.getSession()
                    .execute("SELECT * FROM " + KEYSPACE + "." + DEFAULT_TABLE_NAME);
                List<Row> rows = resultSet.all();
                if (rows.size() == 0) { // Table is created but row is not created
                    return false;
                }
                assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                assertNotNull(row.getString("id"));
                assertEquals(REQUEST, row.getString("request"));
                assertEquals(RESPONSE, row.getString("response"));
                assertEquals("Hello", row.getString("method_name"));
                assertEquals("simple3", row.getString("service_name"));
                assertNotNull(row.getTimestamp("incoming_time"));
                assertNotNull(row.getTimestamp("outcoming_time"));
                assertEquals(PublisherType.RESTFUL.toString(), row.getString("publisher_type"));

                return true;
            }, equalTo(true));
    }

    @Test
    public void testRestServiceFail() throws Exception {
        final String REQUEST = IOUtils.toString(
            Thread.currentThread().getContextClassLoader().getResourceAsStream("simple3_Hello_fail.req.json"),
            StandardCharsets.UTF_8);

        truncateTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);

        client.post("/REST/deployment3/simple3/Hello", "/simple3_Hello_fail.req.json", 400);

        Awaitility.given()
            .ignoreException(InvalidQueryException.class)
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .until(() -> {
                ResultSet resultSet = EmbeddedCassandraServerHelper.getSession()
                    .execute("SELECT * FROM " + KEYSPACE + "." + DEFAULT_TABLE_NAME);
                List<Row> rows = resultSet.all();
                if (rows.size() == 0) { // Table is created but row is not created
                    return false;
                }
                assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                assertNotNull(row.getString("id"));
                assertEquals(REQUEST, row.getString("request"));
                assertNotNull(row.getString("response"));
                assertEquals("simple3", row.getString("service_name"));
                assertNotNull(row.getTimestamp("incoming_time"));
                assertNotNull(row.getTimestamp("outcoming_time"));
                assertEquals(PublisherType.RESTFUL.toString(), row.getString("publisher_type"));

                return true;
            }, equalTo(true));
    }

    @Test
    public void testSoapServiceOk() throws Exception {
        final String REQUEST = IOUtils.toString(
            Thread.currentThread().getContextClassLoader().getResourceAsStream("simple3_Hello.req.xml"),
            StandardCharsets.UTF_8);

        truncateTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);

        client.post("/deployment3/simple3", "/simple3_Hello.req.xml", "/simple3_Hello.resp.xml");

        Awaitility.given()
            .ignoreException(InvalidQueryException.class)
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .until(() -> {
                ResultSet resultSet = EmbeddedCassandraServerHelper.getSession()
                    .execute("SELECT * FROM " + KEYSPACE + "." + DEFAULT_TABLE_NAME);
                List<Row> rows = resultSet.all();
                if (rows.size() == 0) { // Table is created but row is not created
                    return false;
                }
                assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                assertNotNull(row.getString("id"));
                assertEquals(REQUEST, row.getString("request"));
                assertNotNull(row.getString("response"));
                assertEquals("Hello", row.getString("method_name"));
                assertEquals("simple3", row.getString("service_name"));
                assertNotNull(row.getTimestamp("incoming_time"));
                assertNotNull(row.getTimestamp("outcoming_time"));
                assertEquals(PublisherType.WEBSERVICE.toString(), row.getString("publisher_type"));

                return true;
            }, equalTo(true));
    }

    @Test
    public void testSoapServiceFail() throws Exception {
        final String REQUEST = IOUtils.toString(
            Thread.currentThread().getContextClassLoader().getResourceAsStream("simple3_Hello_fail.req.xml"),
            StandardCharsets.UTF_8);

        truncateTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);

        client.post("/deployment3/simple3", "/simple3_Hello_fail.req.xml", 200);

        Awaitility.given()
            .ignoreException(InvalidQueryException.class)
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .until(() -> {
                ResultSet resultSet = EmbeddedCassandraServerHelper.getSession()
                    .execute("SELECT * FROM " + KEYSPACE + "." + DEFAULT_TABLE_NAME);
                List<Row> rows = resultSet.all();
                if (rows.size() == 0) { // Table is created but row is not created
                    return false;
                }
                assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                assertNotNull(row.getString("id"));
                assertEquals(REQUEST, row.getString("request"));
                assertNotNull(row.getString("response"));
                assertEquals("Hello", row.getString("method_name"));
                assertEquals("simple3", row.getString("service_name"));
                assertNotNull(row.getTimestamp("incoming_time"));
                assertNotNull(row.getTimestamp("outcoming_time"));
                assertEquals(PublisherType.WEBSERVICE.toString(), row.getString("publisher_type"));

                return true;
            }, equalTo(true));
    }

    @Test
    public void testStoreLogDataAnnotations() throws Exception {
        final String REQUEST = IOUtils.toString(
            Thread.currentThread().getContextClassLoader().getResourceAsStream("simple4_Hello.req.json"),
            StandardCharsets.UTF_8);
        final String RESPONSE = IOUtils.toString(
            Thread.currentThread().getContextClassLoader().getResourceAsStream("simple4_Hello.resp.txt"),
            StandardCharsets.UTF_8);

        final String helloEntity1TableName = getTableName(HelloEntity1.class);
        final String helloEntity2TableName = getTableName(HelloEntity2.class);
        final String helloEntity3TableName = getTableName(HelloEntity3.class);
        final String helloEntity4TableName = getTableName(HelloEntity4.class);

        truncateTableIfExists(KEYSPACE, helloEntity1TableName);
        truncateTableIfExists(KEYSPACE, helloEntity2TableName);
        truncateTableIfExists(KEYSPACE, helloEntity3TableName);
        truncateTableIfExists(KEYSPACE, helloEntity4TableName);

        client.post("/REST/deployment4/simple4/Hello", "/simple4_Hello.req.json", "/simple4_Hello.resp.txt");

        Awaitility.given()
            .ignoreException(InvalidQueryException.class)
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .until(() -> {
                ResultSet resultSet = EmbeddedCassandraServerHelper.getSession()
                    .execute("SELECT * FROM " + KEYSPACE + "." + helloEntity1TableName);
                List<Row> rows = resultSet.all();
                if (rows.size() == 0) { // Table is created but row is not created
                    return false;
                }
                assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                assertNotNull(row.getString("id"));
                assertEquals(REQUEST, row.getString("request"));
                assertEquals(RESPONSE, row.getString("response"));
                assertEquals("Hello", row.getString("method_name"));
                assertEquals("simple4", row.getString("service_name"));
                assertNotNull(row.getTimestamp("incoming_time"));
                assertNotNull(row.getTimestamp("outcoming_time"));
                assertEquals(PublisherType.RESTFUL.toString(), row.getString("publisher_type"));

                assertEquals("value1", row.getString("value"));
                assertEquals(5, row.getInt("hour"));
                assertEquals("Good Morning", row.getString("result"));
                assertTrue(row.getBool("object_serializer_found"));

                resultSet = EmbeddedCassandraServerHelper.getSession()
                    .execute("SELECT * FROM " + KEYSPACE + "." + helloEntity2TableName);
                rows = resultSet.all();
                if (rows.size() == 0) { // Table is created but row is not created
                    return false;
                }
                assertEquals(1, rows.size());
                row = rows.iterator().next();
                assertNotNull(row.getString("id"));
                assertEquals(REQUEST, row.getString("request"));
                assertEquals(RESPONSE, row.getString("response"));
                assertEquals("Hello", row.getString("method_name"));
                assertEquals("simple4", row.getString("service_name"));
                assertNotNull(row.getTimestamp("incoming_time"));
                assertNotNull(row.getTimestamp("outcoming_time"));
                assertEquals(PublisherType.RESTFUL.toString(), row.getString("publisher_type"));

                assertEquals("value1", row.getString("value"));
                assertEquals(5, row.getInt("hour"));
                assertEquals("Good Morning", row.getString("result"));

                resultSet = EmbeddedCassandraServerHelper.getSession()
                    .execute("SELECT * FROM " + KEYSPACE + "." + helloEntity3TableName);
                rows = resultSet.all();
                if (rows.size() == 0) { // Table is created but row is not created
                    return false;
                }
                assertEquals(1, rows.size());
                row = rows.iterator().next();
                assertNotNull(row.getString("id"));
                assertNull(row.getString("request"));
                assertNull(row.getString("response"));
                assertNull(row.getString("method_name"));
                assertEquals("simple4", row.getString("service_name"));
                assertNull(row.getTimestamp("incoming_time"));
                assertNull(row.getTimestamp("outcoming_time"));
                assertEquals(PublisherType.RESTFUL.toString(), row.getString("publisher_type"));

                assertNull(row.getString("value"));
                assertNull(row.getString("result"));

                assertNull(EmbeddedCassandraServerHelper.getCluster()
                    .getMetadata()
                    .getKeyspace(KEYSPACE)
                    .getTable(helloEntity4TableName));

                return true;
            }, equalTo(true));
    }

    @Test
    public void testStoreLogDataAnnotationsAdvanced() throws Exception {
        final String REQUEST = IOUtils.toString(
            Thread.currentThread().getContextClassLoader().getResourceAsStream("simple4_Hello2.req.json"),
            StandardCharsets.UTF_8);
        final String RESPONSE = IOUtils.toString(
            Thread.currentThread().getContextClassLoader().getResourceAsStream("simple4_Hello2.resp.txt"),
            StandardCharsets.UTF_8);

        final String helloEntity1TableName = getTableName(HelloEntity1.class);

        truncateTableIfExists(KEYSPACE, helloEntity1TableName);

        client.post("/REST/deployment4/simple4/Hello2", "/simple4_Hello2.req.json", "/simple4_Hello2.resp.txt");

        Awaitility.given()
            .ignoreException(InvalidQueryException.class)
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .until(() -> {
                ResultSet resultSet = EmbeddedCassandraServerHelper.getSession()
                    .execute("SELECT * FROM " + KEYSPACE + "." + helloEntity1TableName);
                List<Row> rows = resultSet.all();
                if (rows.size() == 0) { // Table is created but row is not created
                    return false;
                }
                assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                assertNotNull(row.getString("id"));
                assertEquals(REQUEST, row.getString("request"));
                assertEquals(RESPONSE, row.getString("response"));
                assertEquals("Hello2", row.getString("method_name"));
                assertEquals("simple4", row.getString("service_name"));
                assertNotNull(row.getTimestamp("incoming_time"));
                assertNotNull(row.getTimestamp("outcoming_time"));
                assertEquals(PublisherType.RESTFUL.toString(), row.getString("publisher_type"));

                assertEquals(5, row.getInt("int_value1"));
                assertEquals(22, row.getInt("int_value2"));
                assertEquals(22, row.getInt("int_value3"));
                assertEquals("Good Night", row.getString("string_value1"));
                assertEquals("Good Night", row.getString("string_value2"));
                assertEquals(RESPONSE, row.getString("string_value3"));
                assertTrue(row.getBool("bool_value1"));
                assertEquals("22", row.getString("int_value_to_string"));
                return true;
            }, equalTo(true));
    }

    private static String getTableName(Class<?> entityClass) {
        CqlName cqlName = entityClass.getAnnotation(CqlName.class);
        if (cqlName != null) {
            return cqlName.value();
        }
        throw new IllegalStateException("Only @CqlName annotated classes are supported.");
    }

    @Test
    public void testKafkaHeaderAnnotations() throws Exception {
        final String REQUEST = IOUtils.toString(
            Thread.currentThread().getContextClassLoader().getResourceAsStream("simple4_Hello.req.json"),
            StandardCharsets.UTF_8);
        final String RESPONSE = IOUtils.toString(
            Thread.currentThread().getContextClassLoader().getResourceAsStream("simple4_Hello.resp.txt"),
            StandardCharsets.UTF_8);

        final String METHOD_NAME = "Hello";

        final String helloEntity1TableName = getTableName(HelloEntity1.class);
        final String helloEntity2TableName = getTableName(HelloEntity2.class);
        final String helloEntity3TableName = getTableName(HelloEntity3.class);
        final String helloEntity4TableName = getTableName(HelloEntity4.class);

        truncateTableIfExists(KEYSPACE, helloEntity1TableName);
        truncateTableIfExists(KEYSPACE, helloEntity2TableName);
        truncateTableIfExists(KEYSPACE, helloEntity3TableName);
        truncateTableIfExists(KEYSPACE, helloEntity4TableName);

        KeyValue<String, String> record = new KeyValue<>(null, REQUEST);
        record.addHeader(KafkaHeaders.METHOD_NAME, METHOD_NAME, Charset.forName("UTF8"));
        record.addHeader(KafkaHeaders.METHOD_PARAMETERS, "*, *", Charset.forName("UTF8"));
        record.addHeader("testHeader", "testHeaderValue", Charset.forName("UTF8"));
        cluster.send(SendKeyValues.to("hello-in-topic-4", Collections.singletonList(record)).useDefaults());

        ObserveKeyValues<String, String> observeRequest = ObserveKeyValues.on("hello-out-topic-4", 1)
            .with("metadata.max.age.ms", 1000)
            .build();
        List<String> observedValues = cluster.observeValues(observeRequest);
        assertEquals(1, observedValues.size());
        assertEquals(RESPONSE, observedValues.get(0));

        Awaitility.given()
            .ignoreException(InvalidQueryException.class)
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .until(() -> {
                ResultSet resultSet = EmbeddedCassandraServerHelper.getSession()
                    .execute("SELECT * FROM " + KEYSPACE + "." + helloEntity1TableName);
                List<Row> rows = resultSet.all();
                if (rows.size() == 0) { // Table is created but row is not created
                    return false;
                }
                assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                assertNotNull(row.getString("id"));
                assertEquals(REQUEST, row.getString("request"));
                assertEquals(RESPONSE, row.getString("response"));
                assertEquals(METHOD_NAME, row.getString("method_name"));
                assertEquals("simple4", row.getString("service_name"));
                assertNotNull(row.getTimestamp("incoming_time"));
                assertNotNull(row.getTimestamp("outcoming_time"));
                assertEquals(PublisherType.KAFKA.toString(), row.getString("publisher_type"));

                assertEquals(METHOD_NAME, row.getString("header1"));
                assertEquals("testHeaderValue", row.getString("header2"));

                return true;
            }, equalTo(true));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Thread.sleep(Long.MAX_VALUE);

        server.stop();
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
        cluster.stop();
    }

}