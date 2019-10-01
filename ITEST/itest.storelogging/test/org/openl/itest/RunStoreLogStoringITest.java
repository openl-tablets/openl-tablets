package org.openl.itest;

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static org.hamcrest.CoreMatchers.equalTo;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.awaitility.Awaitility;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.openl.itest.cassandra.HelloEntity1;
import org.openl.itest.cassandra.HelloEntity2;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;
import org.openl.rules.ruleservice.kafka.KafkaHeaders;
import org.openl.rules.ruleservice.logging.annotation.PublisherType;
import org.openl.rules.ruleservice.logging.cassandra.DefaultCassandraEntity;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import com.datastax.driver.mapping.annotations.Table;

import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig;
import net.mguenther.kafka.junit.EmbeddedKafkaConfig;
import net.mguenther.kafka.junit.KeyValue;
import net.mguenther.kafka.junit.ObserveKeyValues;
import net.mguenther.kafka.junit.SendKeyValues;

public class RunStoreLogStoringITest {
    // private static final int TIMEOUT = Integer.MAX_VALUE;
    private static final int AWAIT_TIMEOUT = 30;
    private static final String KEYSPACE = "openl_ws_logging";

    private static final String DEFAULT_TABLE_NAME = DefaultCassandraEntity.class.getAnnotation(Table.class).name();

    private static JettyServer server;
    private static HttpClient client;
    private static String host;

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

        System.setProperty("cassandra.contactpoints", EmbeddedCassandraServerHelper.getHost());
        System.setProperty("cassandra.port", String.valueOf(EmbeddedCassandraServerHelper.getNativeTransportPort()));

        createKeyspaceIfNotExists(EmbeddedCassandraServerHelper.getSession(), KEYSPACE, "SimpleStrategy", 1);

        server = new JettyServer(true);
        host = server.start();

        client = server.client();
    }

    @Rule
    public EmbeddedKafkaCluster cluster = provisionWith(EmbeddedKafkaClusterConfig.create()
        .provisionWith(EmbeddedKafkaConfig.create().with("listeners", "PLAINTEXT://:61099").build())
        .build());

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
        final String RESPONSE = "\"Good Morning\"";

        truncateTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);

        KeyValue<String, String> record = new KeyValue<>(null, REQUEST);
        cluster.send(SendKeyValues.to("hello-in-topic", Collections.singletonList(record)).useDefaults());
        ObserveKeyValues<String, String> observeRequest = ObserveKeyValues.on("hello-out-topic", 1)
            .with("metadata.max.age.ms", 1000)
            .build();
        List<String> observedValues = cluster.observeValues(observeRequest);
        Assert.assertEquals(1, observedValues.size());
        Assert.assertEquals(RESPONSE, observedValues.get(0));

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
                Assert.assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                Assert.assertNotNull(row.getString("id"));
                Assert.assertEquals(REQUEST, row.getString("request"));
                Assert.assertEquals(RESPONSE, row.getString("response"));
                Assert.assertEquals("Hello", row.getString("methodName"));
                Assert.assertEquals("simple1", row.getString("serviceName"));
                Assert.assertNotNull(row.getTimestamp("incomingTime"));
                Assert.assertNotNull(row.getTimestamp("outcomingTime"));
                Assert.assertEquals(PublisherType.KAFKA.toString(), row.getString("publisherType"));

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
        Assert.assertEquals(1, observedValuesDlt.size());

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
                Assert.assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                Assert.assertNotNull(row.getString("id"));
                Assert.assertEquals(REQUEST, row.getString("request"));
                Assert.assertEquals(RESPONSE, row.getString("response"));
                Assert.assertEquals("Hello", row.getString("methodName"));
                Assert.assertEquals("simple1", row.getString("serviceName"));
                Assert.assertNotNull(row.getTimestamp("incomingTime"));
                Assert.assertNotNull(row.getTimestamp("outcomingTime"));
                Assert.assertEquals(PublisherType.KAFKA.toString(), row.getString("publisherType"));
                return true;
            }, equalTo(true));
    }

    @Test
    public void testKafkaServiceOk() throws Exception {
        final String REQUEST = "{\"hour\": 5}";
        final String RESPONSE = "\"Good Morning\"";

        final String METHOD_NAME = "Hello";

        truncateTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);

        KeyValue<String, String> record = new KeyValue<>(null, REQUEST);
        record.addHeader(KafkaHeaders.METHOD_NAME, METHOD_NAME, Charset.forName("UTF8"));
        cluster.send(SendKeyValues.to("hello-in-topic-2", Collections.singletonList(record)).useDefaults());

        ObserveKeyValues<String, String> observeRequest = ObserveKeyValues.on("hello-out-topic-2", 1)
            .with("metadata.max.age.ms", 1000)
            .build();
        List<String> observedValues = cluster.observeValues(observeRequest);
        Assert.assertEquals(1, observedValues.size());
        Assert.assertEquals(RESPONSE, observedValues.get(0));

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
                Assert.assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                Assert.assertNotNull(row.getString("id"));
                Assert.assertEquals(REQUEST, row.getString("request"));
                Assert.assertEquals(RESPONSE, row.getString("response"));
                Assert.assertEquals(METHOD_NAME, row.getString("methodName"));
                Assert.assertEquals("simple2", row.getString("serviceName"));
                Assert.assertNotNull(row.getTimestamp("incomingTime"));
                Assert.assertNotNull(row.getTimestamp("outcomingTime"));
                Assert.assertEquals(PublisherType.KAFKA.toString(), row.getString("publisherType"));

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
        Assert.assertEquals(1, observedValuesDlt.size());

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
                Assert.assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                Assert.assertNotNull(row.getString("id"));
                Assert.assertEquals(REQUEST, row.getString("request"));
                Assert.assertEquals(RESPONSE, row.getString("response"));
                Assert.assertEquals(METHOD_NAME, row.getString("methodName"));
                Assert.assertEquals("simple2", row.getString("serviceName"));
                Assert.assertNotNull(row.getTimestamp("incomingTime"));
                Assert.assertNotNull(row.getTimestamp("outcomingTime"));
                Assert.assertEquals(PublisherType.KAFKA.toString(), row.getString("publisherType"));
                return true;
            }, equalTo(true));
    }

    @Test
    public void testRestServiceOk() throws Exception {
        final String REQUEST = IOUtils.toString(
            Thread.currentThread().getContextClassLoader().getResourceAsStream("simple3_Hello.req.json"),
            StandardCharsets.UTF_8);
        final String RESPONSE = IOUtils.toString(
            Thread.currentThread().getContextClassLoader().getResourceAsStream("simple3_Hello.resp.json"),
            StandardCharsets.UTF_8);

        truncateTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);

        client.post("/REST/deployment3/simple3/Hello", "/simple3_Hello.req.json", "/simple3_Hello.resp.json");

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
                Assert.assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                Assert.assertNotNull(row.getString("id"));
                Assert.assertEquals(REQUEST, row.getString("request"));
                Assert.assertEquals(RESPONSE, row.getString("response"));
                Assert.assertEquals("Hello", row.getString("methodName"));
                Assert.assertEquals("simple3", row.getString("serviceName"));
                Assert.assertNotNull(row.getTimestamp("incomingTime"));
                Assert.assertNotNull(row.getTimestamp("outcomingTime"));
                Assert.assertEquals(PublisherType.RESTFUL.toString(), row.getString("publisherType"));

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
                Assert.assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                Assert.assertNotNull(row.getString("id"));
                Assert.assertEquals(REQUEST, row.getString("request"));
                Assert.assertNotNull(row.getString("response"));
                Assert.assertEquals("simple3", row.getString("serviceName"));
                Assert.assertNotNull(row.getTimestamp("incomingTime"));
                Assert.assertNotNull(row.getTimestamp("outcomingTime"));
                Assert.assertEquals(PublisherType.RESTFUL.toString(), row.getString("publisherType"));

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
                Assert.assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                Assert.assertNotNull(row.getString("id"));
                Assert.assertEquals(REQUEST, row.getString("request"));
                Assert.assertNotNull(row.getString("response"));
                Assert.assertEquals("Hello", row.getString("methodName"));
                Assert.assertEquals("simple3", row.getString("serviceName"));
                Assert.assertNotNull(row.getTimestamp("incomingTime"));
                Assert.assertNotNull(row.getTimestamp("outcomingTime"));
                Assert.assertEquals(PublisherType.WEBSERVICE.toString(), row.getString("publisherType"));

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
                Assert.assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                Assert.assertNotNull(row.getString("id"));
                Assert.assertEquals(REQUEST, row.getString("request"));
                Assert.assertNotNull(row.getString("response"));
                Assert.assertEquals("Hello", row.getString("methodName"));
                Assert.assertEquals("simple3", row.getString("serviceName"));
                Assert.assertNotNull(row.getTimestamp("incomingTime"));
                Assert.assertNotNull(row.getTimestamp("outcomingTime"));
                Assert.assertEquals(PublisherType.WEBSERVICE.toString(), row.getString("publisherType"));

                return true;
            }, equalTo(true));
    }

    @Test
    public void testLogDataAnnotations() throws Exception {
        final String REQUEST = IOUtils.toString(
            Thread.currentThread().getContextClassLoader().getResourceAsStream("simple4_Hello.req.json"),
            StandardCharsets.UTF_8);
        final String RESPONSE = IOUtils.toString(
            Thread.currentThread().getContextClassLoader().getResourceAsStream("simple4_Hello.resp.json"),
            StandardCharsets.UTF_8);

        final String helloEntity1TableName = HelloEntity1.class.getAnnotation(Table.class).name();
        final String helloEntity2TableName = HelloEntity2.class.getAnnotation(Table.class).name();

        truncateTableIfExists(KEYSPACE, helloEntity1TableName);
        truncateTableIfExists(KEYSPACE, helloEntity2TableName);

        client.post("/REST/deployment4/simple4/Hello", "/simple4_Hello.req.json", "/simple4_Hello.resp.json");

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
                Assert.assertEquals(1, rows.size());
                Row row = rows.iterator().next();
                Assert.assertNotNull(row.getString("id"));
                Assert.assertEquals(REQUEST, row.getString("request"));
                Assert.assertEquals(RESPONSE, row.getString("response"));
                Assert.assertEquals("Hello", row.getString("methodName"));
                Assert.assertEquals("simple4", row.getString("serviceName"));
                Assert.assertNotNull(row.getTimestamp("incomingTime"));
                Assert.assertNotNull(row.getTimestamp("outcomingTime"));
                Assert.assertEquals(PublisherType.RESTFUL.toString(), row.getString("publisherType"));

                resultSet = EmbeddedCassandraServerHelper.getSession()
                    .execute("SELECT * FROM " + KEYSPACE + "." + helloEntity2TableName);
                rows = resultSet.all();
                if (rows.size() == 0) { // Table is created but row is not created
                    return false;
                }
                Assert.assertEquals(1, rows.size());
                row = rows.iterator().next();
                Assert.assertNotNull(row.getString("id"));
                Assert.assertEquals(REQUEST, row.getString("request"));
                Assert.assertEquals(RESPONSE, row.getString("response"));
                Assert.assertEquals("Hello", row.getString("methodName"));
                Assert.assertEquals("simple4", row.getString("serviceName"));
                Assert.assertNotNull(row.getTimestamp("incomingTime"));
                Assert.assertNotNull(row.getTimestamp("outcomingTime"));
                Assert.assertEquals(PublisherType.RESTFUL.toString(), row.getString("publisherType"));

                return true;
            }, equalTo(true));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Thread.sleep(Long.MAX_VALUE);

        server.stop();
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
    }

}