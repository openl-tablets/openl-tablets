package org.openl.itest;

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static org.awaitility.Awaitility.given;
import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.persistence.Entity;

import org.apache.log4j.Logger;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.h2.tools.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
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
import org.openl.itest.elasticsearch.CustomElasticEntity1;
import org.openl.itest.elasticsearch.CustomElasticEntity2;
import org.openl.itest.elasticsearch.CustomElasticEntity3;
import org.openl.itest.elasticsearch.CustomElasticEntity4;
import org.openl.itest.elasticsearch.CustomElasticEntity8;
import org.openl.itest.elasticsearch.ElasticFields;
import org.openl.rules.ruleservice.kafka.KafkaHeaders;
import org.openl.rules.ruleservice.storelogdata.annotation.PublisherType;
import org.openl.rules.ruleservice.storelogdata.cassandra.DefaultCassandraEntity;
import org.openl.rules.ruleservice.storelogdata.db.DefaultEntity;
import org.openl.rules.ruleservice.storelogdata.elasticsearch.DefaultElasticEntity;
import org.openl.util.IOUtils;
import org.springframework.data.elasticsearch.annotations.Document;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;

import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig;
import net.mguenther.kafka.junit.EmbeddedKafkaConfig;
import net.mguenther.kafka.junit.KeyValue;
import net.mguenther.kafka.junit.ObserveKeyValues;
import net.mguenther.kafka.junit.SendKeyValues;

public class RunStoreLogDataITest {
    private static final Logger log = Logger.getLogger(RunStoreLogDataITest.class);

    public static final int POLL_INTERVAL_IN_MILLISECONDS = 500;
    private static final int AWAIT_TIMEOUT = 60;
    private static final String KEYSPACE = "openl_ws_logging";

    private static final String DEFAULT_ELASTIC_INDEX_NAME = DefaultElasticEntity.class.getAnnotation(Document.class)
        .indexName();
    private static final String DEFAULT_ELASTIC_CLUSTER_NAME = "ELASTIC-SEARCH-CLUSTER";

    private static final String DEFAULT_TABLE_NAME = DefaultCassandraEntity.class.getAnnotation(CqlName.class).value();

    private static final String DEFAULT_H2_TABLE_NAME = DefaultEntity.class.getAnnotation(Entity.class).name();

    private static final String HELLO_METHOD_NAME = "Hello";
    private static final String HELLO2_METHOD_NAME = "Hello2";
    private static final String SIMPLE1_SERVICE_NAME = "simple1";
    private static final String SIMPLE2_SERVICE_NAME = "simple2";
    private static final String SIMPLE3_SERVICE_NAME = "simple3";
    private static final String SIMPLE4_SERVICE_NAME = "simple4";
    private static final String SIMPLE5_SERVICE_NAME = "simple5";

    private static final String KAFKA_PUBLISHER_TYPE = PublisherType.KAFKA.name();
    private static final String RESTFUL_PUBLISHER_TYPE = PublisherType.RESTFUL.name();
    private static final String WEBSERVICE_PUBLISHER_TYPE = PublisherType.WEBSERVICE.name();

    private static JettyServer server;
    private static HttpClient client;
    private static EmbeddedKafkaCluster cluster;
    private static ElasticsearchClusterRunner elasticRunner;
    private static Client esClient;
    private static Connection h2Connection;
    private static Server h2Server;

    @BeforeClass
    public static void setUp() throws Exception {
        h2Server = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9110");
        h2Server.start();

        System.setProperty("es.set.netty.runtime.available.processors", "false");
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

        server = JettyServer.startSharingClassLoader();
        client = server.client();

        elasticRunner = new ElasticsearchClusterRunner();
        System.setProperty("elasticsearch.cluster", DEFAULT_ELASTIC_CLUSTER_NAME);
        elasticRunner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("http.cors.allow-origin", "*");
            settingsBuilder.putList("discovery.zen.ping.unicast.hosts", "localhost:9301-9305");
        }).build(newConfigs().clusterName(DEFAULT_ELASTIC_CLUSTER_NAME).numOfNode(1));
        elasticRunner.ensureYellow();
        esClient = elasticRunner.client();
        h2Connection = DriverManager.getConnection("jdbc:h2:tcp://localhost:9110/mem:mydb");
    }

    private void truncateCassandraTableIfExists(final String keyspace, final String table) {
        try {
            EmbeddedCassandraServerHelper.getSession().execute("TRUNCATE " + keyspace + "." + table);
        } catch (QueryExecutionException | InvalidQueryException ignored) {
        }
    }

    private void truncateH2TableIfExists(final String table) {
        try {
            CallableStatement statement = h2Connection.prepareCall("TRUNCATE TABLE " + table);
            statement.execute();
        } catch (SQLException ignored) {
        }
    }

    private static void createKeyspaceIfNotExists(Session session,
            String keyspaceName,
            String replicationStrategy,
            int replicationFactor) {
        String query = "CREATE KEYSPACE IF NOT EXISTS " + keyspaceName + " WITH replication = {" + "'class':'" + replicationStrategy + "','replication_factor':" + replicationFactor + "};";
        session.execute(query);
    }

    private void removeIndexIfExists(final String indexName) {
        if (elasticRunner.indexExists(indexName))
            elasticRunner.deleteIndex(indexName);
    }

    private Callable<Boolean> validateElastic(final ExpectedLogValues input) {
        return () -> {
            SearchHit[] hits = getElasticSearchHits(DEFAULT_ELASTIC_INDEX_NAME);
            if (hits.length == 0) {
                return false;
            }
            assertEquals(1, hits.length);
            SearchHit hit = hits[0];
            Map<String, Object> source = hit.getSourceAsMap();
            assertNotNull(source.get(ElasticFields.ID));
            assertEquals(input.getRequest(), source.get(ElasticFields.REQUEST_BODY));
            assertEquals(input.getMethodName(), source.get(ElasticFields.METHOD_NAME));
            assertEquals(input.getServiceName(), source.get(ElasticFields.SERVICE_NAME));
            assertNotNull(source.get(ElasticFields.INCOMING_TIME));
            assertNotNull(source.get(ElasticFields.OUTCOMING_TIME));
            String publisherType = (String) source.get(ElasticFields.PUBLISHER_TYPE);
            assertEquals(input.getPublisherType(), publisherType);
            if (publisherType.equals(RESTFUL_PUBLISHER_TYPE)) {
                assertNotNull(source.get(ElasticFields.REQUEST));
                assertNotNull(source.get(ElasticFields.RESPONSE));
            }
            if (publisherType.equals(RESTFUL_PUBLISHER_TYPE) || publisherType.equals(WEBSERVICE_PUBLISHER_TYPE)) {
                assertNotNull(source.get(ElasticFields.URL));
            }
            String responseValue = (String) source.get(ElasticFields.RESPONSE_BODY);
            if (input.isResponseProvided()) {
                String expected = input.getResponse();
                assertEquals(expected, responseValue);
            } else {
                assertNotNull(responseValue);
            }
            return true;
        };
    }

    private List<Row> getCassandraRows(final String tableName) {
        ResultSet resultSet = EmbeddedCassandraServerHelper.getSession()
            .execute("SELECT * FROM " + KEYSPACE + "." + tableName);
        return resultSet.all();
    }

    private SearchHit[] getElasticSearchHits(final String indexName) throws InterruptedException,
                                                                     java.util.concurrent.ExecutionException {
        ActionFuture<SearchResponse> search = esClient.search(new SearchRequest(indexName));
        return search.get().getHits().getHits();
    }

    private Callable<Boolean> validateCassandra(final ExpectedLogValues input) {
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
            assertNotNull(row.getTimestamp(CassandraFields.INCOMING_TIME));
            assertNotNull(row.getTimestamp(CassandraFields.OUTCOMING_TIME));
            return true;
        };
    }

    private Callable<Boolean> validateH2(final ExpectedLogValues input) {
        return () -> {
            final String query = "SELECT * FROM " + DEFAULT_H2_TABLE_NAME;
            try (Statement stmt = h2Connection.createStatement()) {
                java.sql.ResultSet rs = stmt.executeQuery(query);
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

    @Test
    public void testKafkaMethodServiceOk() throws Exception {
        final String REQUEST = "{\"hour\": 5}";
        final String RESPONSE = "Good Morning";

        truncateCassandraTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);
        truncateH2TableIfExists(DEFAULT_H2_TABLE_NAME);

        given().ignoreExceptions().await().atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS).until(() -> {
            removeIndexIfExists(DEFAULT_ELASTIC_INDEX_NAME);
            return !elasticRunner.indexExists(DEFAULT_ELASTIC_INDEX_NAME);
        }, equalTo(true));

        KeyValue<String, String> record = new KeyValue<>(null, REQUEST);
        cluster.send(SendKeyValues.to("hello-in-topic", Collections.singletonList(record)).useDefaults());
        ObserveKeyValues<String, String> observeRequest = ObserveKeyValues.on("hello-out-topic", 1)
            .with("metadata.max.age.ms", 1000)
            .build();
        List<String> observedValues = cluster.observeValues(observeRequest);
        assertEquals(1, observedValues.size());
        assertEquals(RESPONSE, observedValues.get(0));

        ExpectedLogValues values = new ExpectedLogValues(REQUEST,
            RESPONSE,
            HELLO_METHOD_NAME,
            SIMPLE1_SERVICE_NAME,
            KAFKA_PUBLISHER_TYPE);
        given().ignoreException(InvalidQueryException.class)
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .until(validateCassandra(values), equalTo(true));

        given().ignoreExceptions()
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .pollInterval(POLL_INTERVAL_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
            .until(validateElastic(values), equalTo(true));

        given().ignoreExceptions()
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .pollInterval(POLL_INTERVAL_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
            .until(validateH2(values), equalTo(true));
    }

    @Test
    public void testKafkaServiceOkWithNoOutputTopic() throws Exception {
        final String REQUEST = "{\"hour\": 5}";

        truncateCassandraTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);
        truncateH2TableIfExists(DEFAULT_H2_TABLE_NAME);

        given().ignoreExceptions().await().atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS).until(() -> {
            removeIndexIfExists(DEFAULT_ELASTIC_INDEX_NAME);
            return !elasticRunner.indexExists(DEFAULT_ELASTIC_INDEX_NAME);
        }, equalTo(true));

        KeyValue<String, String> record = new KeyValue<>(null, REQUEST);
        record.addHeader(KafkaHeaders.METHOD_NAME, HELLO_METHOD_NAME, StandardCharsets.UTF_8);
        cluster.send(SendKeyValues.to("hello-in-topic-5", Collections.singletonList(record)).useDefaults());

        ExpectedLogValues values = new ExpectedLogValues(REQUEST,
            null,
            HELLO_METHOD_NAME,
            SIMPLE5_SERVICE_NAME,
            KAFKA_PUBLISHER_TYPE);
        values.setResponseProvided(true);
        given().ignoreException(InvalidQueryException.class)
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .until(validateCassandra(values), equalTo(true));

        given().ignoreExceptions()
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .pollInterval(POLL_INTERVAL_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
            .until(validateElastic(values), equalTo(true));

        given().ignoreExceptions()
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .pollInterval(POLL_INTERVAL_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
            .until(validateH2(values), equalTo(true));
    }

    @Test
    public void testKafkaMethodServiceFail() throws Exception {
        final String REQUEST = "5";

        truncateCassandraTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);
        truncateH2TableIfExists(DEFAULT_H2_TABLE_NAME);

        given().ignoreExceptions().await().atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS).until(() -> {
            removeIndexIfExists(DEFAULT_ELASTIC_INDEX_NAME);
            return !elasticRunner.indexExists(DEFAULT_ELASTIC_INDEX_NAME);
        }, equalTo(true));

        KeyValue<String, String> record1 = new KeyValue<>(null, REQUEST);
        cluster.send(SendKeyValues.to("hello-in-topic", Collections.singletonList(record1)).useDefaults());

        ObserveKeyValues<String, String> observeRequestDlt = ObserveKeyValues.on("hello-dlt-topic", 1)
            .with("metadata.max.age.ms", 1000)
            .build();
        List<String> observedValuesDlt = cluster.observeValues(observeRequestDlt);
        assertEquals(1, observedValuesDlt.size());

        ExpectedLogValues values = new ExpectedLogValues(REQUEST,
            REQUEST,
            HELLO_METHOD_NAME,
            SIMPLE1_SERVICE_NAME,
            KAFKA_PUBLISHER_TYPE);

        given().ignoreException(InvalidQueryException.class)
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .until(validateCassandra(values), equalTo(true));

        given().ignoreExceptions()
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .pollInterval(POLL_INTERVAL_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
            .until(validateElastic(values), equalTo(true));

        given().ignoreExceptions()
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .pollInterval(POLL_INTERVAL_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
            .until(validateH2(values), equalTo(true));
    }

    @Test
    public void testKafkaServiceOk() throws Exception {
        final String REQUEST = "{\"hour\": 5}";
        final String RESPONSE = "Good Morning";

        truncateCassandraTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);
        truncateH2TableIfExists(DEFAULT_H2_TABLE_NAME);

        given().ignoreExceptions().await().atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS).until(() -> {
            removeIndexIfExists(DEFAULT_ELASTIC_INDEX_NAME);
            return !elasticRunner.indexExists(DEFAULT_ELASTIC_INDEX_NAME);
        }, equalTo(true));

        KeyValue<String, String> record = new KeyValue<>(null, REQUEST);
        record.addHeader(KafkaHeaders.METHOD_NAME, HELLO_METHOD_NAME, StandardCharsets.UTF_8);
        cluster.send(SendKeyValues.to("hello-in-topic-2", Collections.singletonList(record)).useDefaults());

        ObserveKeyValues<String, String> observeRequest = ObserveKeyValues.on("hello-out-topic-2", 1)
            .with("metadata.max.age.ms", 1000)
            .build();
        List<String> observedValues = cluster.observeValues(observeRequest);
        assertEquals(1, observedValues.size());
        assertEquals(RESPONSE, observedValues.get(0));

        ExpectedLogValues values = new ExpectedLogValues(REQUEST,
            RESPONSE,
            HELLO_METHOD_NAME,
            SIMPLE2_SERVICE_NAME,
            KAFKA_PUBLISHER_TYPE);

        given().ignoreException(InvalidQueryException.class)
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .until(validateCassandra(values), equalTo(true));

        given().ignoreExceptions()
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .pollInterval(POLL_INTERVAL_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
            .until(validateElastic(values), equalTo(true));

        given().ignoreExceptions()
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .pollInterval(POLL_INTERVAL_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
            .until(validateH2(values), equalTo(true));
    }

    @Test
    public void testKafkaServiceFail() throws Exception {
        final String REQUEST = "5";
        final String RESPONSE = "5";

        truncateCassandraTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);
        truncateH2TableIfExists(DEFAULT_H2_TABLE_NAME);

        given().ignoreExceptions().await().atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS).until(() -> {
            removeIndexIfExists(DEFAULT_ELASTIC_INDEX_NAME);
            return !elasticRunner.indexExists(DEFAULT_ELASTIC_INDEX_NAME);
        }, equalTo(true));

        KeyValue<String, String> record = new KeyValue<>(null, "5");
        record.addHeader(KafkaHeaders.METHOD_NAME, HELLO_METHOD_NAME, StandardCharsets.UTF_8);
        cluster.send(SendKeyValues.to("hello-in-topic-2", Collections.singletonList(record)).useDefaults());

        ObserveKeyValues<String, String> observeRequestDlt = ObserveKeyValues.on("hello-dlt-topic-2", 1)
            .with("metadata.max.age.ms", 1000)
            .build();
        List<String> observedValuesDlt = cluster.observeValues(observeRequestDlt);
        assertEquals(1, observedValuesDlt.size());

        ExpectedLogValues values = new ExpectedLogValues(REQUEST,
            RESPONSE,
            HELLO_METHOD_NAME,
            SIMPLE2_SERVICE_NAME,
            KAFKA_PUBLISHER_TYPE);
        given().ignoreException(InvalidQueryException.class)
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .until(validateCassandra(values), equalTo(true));

        given().ignoreExceptions()
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .pollInterval(POLL_INTERVAL_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
            .until(validateElastic(values), equalTo(true));

        given().ignoreExceptions()
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .pollInterval(POLL_INTERVAL_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
            .until(validateH2(values), equalTo(true));
    }

    @Test
    public void testSyncStoreFails() {
        client.send("simple7_Hello.json");
        client.send("simple7_Hello.gzip");
        truncateH2TableIfExists(getDBTableName(org.openl.itest.db.HelloEntity9.class));
        client.send("simple4_Hello3.post");
        client.send("simple4_Hello3.post.gzip");
        client.send("simple4_Hello3_fail.post");
    }

    @Test
    public void testRestServiceOk() throws Exception {
        final String REQUEST = getText("simple3_Hello.req.json");
        final String RESPONSE = getText("simple3_Hello.resp.txt");

        truncateCassandraTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);
        truncateH2TableIfExists(DEFAULT_H2_TABLE_NAME);

        given().ignoreExceptions().await().atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS).until(() -> {
            removeIndexIfExists(DEFAULT_ELASTIC_INDEX_NAME);
            return !elasticRunner.indexExists(DEFAULT_ELASTIC_INDEX_NAME);
        }, equalTo(true));

        client.post("/REST/deployment3/simple3/Hello", "/simple3_Hello.req.json", "/simple3_Hello.resp.txt");

        ExpectedLogValues parameters = new ExpectedLogValues(REQUEST,
            RESPONSE,
            HELLO_METHOD_NAME,
            SIMPLE3_SERVICE_NAME,
            RESTFUL_PUBLISHER_TYPE);

        given().ignoreException(InvalidQueryException.class)
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .until(validateCassandra(parameters), equalTo(true));

        given().ignoreExceptions()
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .pollInterval(POLL_INTERVAL_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
            .until(validateElastic(parameters), equalTo(true));

        given().ignoreExceptions()
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .pollInterval(POLL_INTERVAL_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
            .until(validateH2(parameters), equalTo(true));

    }

    private String getText(String file) throws Exception {
        return IOUtils.toStringAndClose(getClass().getResourceAsStream("/" + file));
    }

    @Test
    public void testRestServiceFail() throws Exception {
        final String REQUEST = getText("simple3_Hello_fail.req.json");

        truncateCassandraTableIfExists(KEYSPACE, DEFAULT_TABLE_NAME);
        truncateH2TableIfExists(DEFAULT_H2_TABLE_NAME);
        given().ignoreExceptions().await().atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS).until(() -> {
            removeIndexIfExists(DEFAULT_ELASTIC_INDEX_NAME);
            return !elasticRunner.indexExists(DEFAULT_ELASTIC_INDEX_NAME);
        }, equalTo(true));

        client.post("/REST/deployment3/simple3/Hello", "/simple3_Hello_fail.req.json", 400);

        ExpectedLogValues values = new ExpectedLogValues(REQUEST,
            null,
            null,
            SIMPLE3_SERVICE_NAME,
            RESTFUL_PUBLISHER_TYPE);
        given().ignoreException(InvalidQueryException.class)
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .until(validateCassandra(values), equalTo(true));

        given().ignoreExceptions()
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .pollInterval(POLL_INTERVAL_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
            .until(validateElastic(values), equalTo(true));

        given().ignoreExceptions()
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .pollInterval(POLL_INTERVAL_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
            .until(validateH2(values), equalTo(true));
    }

    @Test
    public void testStoreLogDataAnnotations() throws Exception {
        final String REQUEST = getText("simple4_Hello.req.json");
        final String RESPONSE = getText("simple4_Hello.resp.txt");

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

        final String customElasticIndexName1 = CustomElasticEntity1.class.getAnnotation(Document.class).indexName();
        final String customElasticIndexName2 = CustomElasticEntity2.class.getAnnotation(Document.class).indexName();
        final String customElasticIndexName3 = CustomElasticEntity3.class.getAnnotation(Document.class).indexName();
        final String customElasticIndexName4 = CustomElasticEntity4.class.getAnnotation(Document.class).indexName();
        final String customElasticIndexName8 = CustomElasticEntity8.class.getAnnotation(Document.class).indexName();

        given().ignoreExceptions().await().atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS).until(() -> {
            removeIndexIfExists(customElasticIndexName1);
            removeIndexIfExists(customElasticIndexName2);
            removeIndexIfExists(customElasticIndexName3);
            removeIndexIfExists(customElasticIndexName4);
            removeIndexIfExists(customElasticIndexName8);
            return !elasticRunner.indexExists(customElasticIndexName1) && !elasticRunner.indexExists(
                customElasticIndexName2) && !elasticRunner.indexExists(customElasticIndexName3) && !elasticRunner
                    .indexExists(customElasticIndexName4) && !elasticRunner.indexExists(customElasticIndexName8);
        }, equalTo(true));

        client.post("/deployment4/simple4/Hello", "/simple4_Hello.req.json", "/simple4_Hello.resp.txt");

        List<Row> rows = getCassandraRows(helloEntity1TableName);
        if (rows.size() == 0) { // Table is created but row is not created
            fail("Table is created but row is not created");
        }
        assertEquals(1, rows.size());
        Row row = rows.iterator().next();
        assertNotNull(row.getString(CassandraFields.ID));
        assertEquals(REQUEST, row.getString(CassandraFields.REQUEST));
        assertEquals(RESPONSE, row.getString(CassandraFields.RESPONSE));
        assertEquals(HELLO_METHOD_NAME, row.getString(CassandraFields.METHOD_NAME));
        assertEquals(SIMPLE4_SERVICE_NAME, row.getString(CassandraFields.SERVICE_NAME));
        assertNotNull(row.getTimestamp(CassandraFields.INCOMING_TIME));
        assertNotNull(row.getTimestamp(CassandraFields.OUTCOMING_TIME));
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
        assertEquals(HELLO_METHOD_NAME, row.getString(CassandraFields.METHOD_NAME));
        assertEquals(SIMPLE4_SERVICE_NAME, row.getString(CassandraFields.SERVICE_NAME));
        assertNotNull(row.getTimestamp(CassandraFields.INCOMING_TIME));
        assertNotNull(row.getTimestamp(CassandraFields.OUTCOMING_TIME));
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
        assertEquals(SIMPLE4_SERVICE_NAME, row.getString(CassandraFields.SERVICE_NAME));
        assertNull(row.getTimestamp(CassandraFields.INCOMING_TIME));
        assertNull(row.getTimestamp(CassandraFields.OUTCOMING_TIME));
        assertEquals(RESTFUL_PUBLISHER_TYPE, row.getString(CassandraFields.PUBLISHER_TYPE));

        assertNull(row.getString(CassandraFields.VALUE));
        assertNull(row.getString(CassandraFields.RESULT));

        assertNull(EmbeddedCassandraServerHelper.getCluster()
            .getMetadata()
            .getKeyspace(KEYSPACE)
            .getTable(helloEntity4TableName));

        assertNull(EmbeddedCassandraServerHelper.getCluster()
            .getMetadata()
            .getKeyspace(KEYSPACE)
            .getTable(helloEntity8TableName));
        // H2
        String query = "SELECT * FROM " + h2HelloEntity1TableName;
        try (Statement stmt = h2Connection.createStatement()) {
            java.sql.ResultSet rs = stmt.executeQuery(query);
            int count = 0;
            while (rs.next()) {
                count++;
                if (count > 1) {
                    break;
                }
                assertEquals(REQUEST, rs.getString(DBFields.REQUEST));
                assertEquals(RESPONSE, rs.getString(DBFields.RESPONSE));
                assertEquals(HELLO_METHOD_NAME, rs.getString(DBFields.METHOD_NAME));
                assertEquals(SIMPLE4_SERVICE_NAME, rs.getString(DBFields.SERVICE_NAME));
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
            java.sql.ResultSet rs = stmt.executeQuery(query);
            int count = 0;
            while (rs.next()) {
                count++;
                if (count > 1) {
                    break;
                }
                assertEquals(REQUEST, rs.getString(DBFields.REQUEST));
                assertEquals(RESPONSE, rs.getString(DBFields.RESPONSE));
                assertEquals(HELLO_METHOD_NAME, rs.getString(DBFields.METHOD_NAME));
                assertEquals(SIMPLE4_SERVICE_NAME, rs.getString(DBFields.SERVICE_NAME));
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
            java.sql.ResultSet rs = stmt.executeQuery(query);
            int count = 0;
            while (rs.next()) {
                count++;
                if (count > 1) {
                    break;
                }
                assertNull(rs.getString(DBFields.REQUEST));
                assertNull(rs.getString(DBFields.RESPONSE));
                assertNull(rs.getString(DBFields.METHOD_NAME));
                assertEquals(SIMPLE4_SERVICE_NAME, rs.getString(DBFields.SERVICE_NAME));
                assertNull(rs.getTimestamp(DBFields.INCOMING_TIME));
                assertNull(rs.getTimestamp(DBFields.OUTCOMING_TIME));
                assertEquals(RESTFUL_PUBLISHER_TYPE, rs.getString(DBFields.PUBLISHER_TYPE));

                assertNull(rs.getString(DBFields.VALUE));
                assertNull(rs.getString(DBFields.RESULT));
            }
            assertEquals(1, count);
        }

        java.sql.ResultSet rs = h2Connection.getMetaData().getTables(null, null, h2HelloEntity4TableName, null);
        if (rs.next()) {
            fail();
        }
        java.sql.ResultSet rs1 = h2Connection.getMetaData().getTables(null, null, h2HelloEntity8TableName, null);
        if (rs1.next()) {
            fail();
        }
        // Elastic
        SearchHit[] hits = getElasticSearchHits(customElasticIndexName1);
        if (hits.length == 0) {
            fail("SearchHit is not found.");
        }
        assertEquals(1, hits.length);
        SearchHit hit = hits[0];
        Map<String, Object> source = hit.getSourceAsMap();

        assertEquals("value1", hit.getSourceAsMap().get(ElasticFields.VALUE));
        assertEquals(5, source.get(ElasticFields.HOUR));
        assertEquals("Good Morning", source.get(ElasticFields.RESULT));
        assertTrue((Boolean) source.get(ElasticFields.AWARE_INSTANCES_FOUND));
        assertTrue((Boolean) source.get(ElasticFields.ELASTICSEARCH_OPERATIONS_FOUND));

        SearchHit[] hitsCustom2 = getElasticSearchHits(customElasticIndexName2);
        if (hitsCustom2.length == 0) {
            fail("SearchHit is not found.");
        }
        assertEquals(1, hitsCustom2.length);
        SearchHit hitCustom2 = hitsCustom2[0];
        Map<String, Object> source2 = hitCustom2.getSourceAsMap();

        assertEquals("value1", source2.get(ElasticFields.VALUE));
        assertEquals(5, source2.get(ElasticFields.HOUR));
        assertEquals("Good Morning", source2.get(ElasticFields.RESULT));

        SearchHit[] hitsCustom3 = getElasticSearchHits(customElasticIndexName3);
        if (hitsCustom3.length == 0) {
            fail("SearchHit is not found.");
        }
        assertEquals(1, hitsCustom3.length);
        SearchHit hitCustom3 = hitsCustom3[0];
        Map<String, Object> source3 = hitCustom3.getSourceAsMap();

        assertNotNull(source3.get(ElasticFields.ID));
        assertNull(source3.get(ElasticFields.REQUEST));
        assertNull(source3.get(ElasticFields.RESPONSE));
        assertNull(source3.get(ElasticFields.METHOD_NAME));
        assertEquals(SIMPLE4_SERVICE_NAME, source3.get(ElasticFields.SERVICE_NAME));
        assertNull(source3.get(ElasticFields.INCOMING_TIME));
        assertNull(source3.get(ElasticFields.OUTCOMING_TIME));
        assertEquals(RESTFUL_PUBLISHER_TYPE, source3.get(ElasticFields.PUBLISHER_TYPE));

        assertNull(source3.get(ElasticFields.VALUE));
        assertNull(source3.get(ElasticFields.RESULT));

        assertFalse(elasticRunner.indexExists(customElasticIndexName4));
        assertFalse(elasticRunner.indexExists(customElasticIndexName8));
    }

    @Test
    public void testStoreLogDataAnnotationsAdvanced() throws Exception {
        final String REQUEST = getText("simple4_Hello2.req.json");
        final String RESPONSE = getText("simple4_Hello2.resp.txt");

        final String helloEntity1TableName = getCassandraTableName(HelloEntity1.class);
        final String customElasticIndexName1 = CustomElasticEntity1.class.getAnnotation(Document.class).indexName();
        final String h2HelloEntity1TableName = getDBTableName(org.openl.itest.db.HelloEntity1.class);

        truncateCassandraTableIfExists(KEYSPACE, helloEntity1TableName);
        truncateH2TableIfExists(h2HelloEntity1TableName);

        given().ignoreExceptions().await().atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS).until(() -> {
            removeIndexIfExists(customElasticIndexName1);
            return !elasticRunner.indexExists(customElasticIndexName1);
        }, equalTo(true));

        client.post("/deployment4/simple4/Hello2", "/simple4_Hello2.req.json", "/simple4_Hello2.resp.txt");

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
                assertEquals(HELLO2_METHOD_NAME, row.getString(CassandraFields.METHOD_NAME));
                assertEquals(SIMPLE4_SERVICE_NAME, row.getString(CassandraFields.SERVICE_NAME));
                assertNotNull(row.getTimestamp(CassandraFields.INCOMING_TIME));
                assertNotNull(row.getTimestamp(CassandraFields.OUTCOMING_TIME));
                assertEquals(RESTFUL_PUBLISHER_TYPE, row.getString(CassandraFields.PUBLISHER_TYPE));

                assertEquals(5, row.getInt(CassandraFields.INT_VALUE1));
                assertEquals(22, row.getInt(CassandraFields.INT_VALUE2));
                assertEquals(22, row.getInt(CassandraFields.INT_VALUE3));
                assertEquals("Good Night", row.getString(CassandraFields.STRING_VALUE1));
                assertEquals("Good Night", row.getString(CassandraFields.STRING_VALUE2));
                assertEquals(RESPONSE, row.getString(CassandraFields.STRING_VALUE3));
                assertTrue(row.getBool(CassandraFields.BOOL_VALUE1));
                assertEquals("22", row.getString(CassandraFields.INT_VALUE_TO_STRING));
                return true;
            }, equalTo(true));

        given().ignoreExceptions()
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .pollInterval(POLL_INTERVAL_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
            .until(() -> {
                SearchHit[] hits = getElasticSearchHits(customElasticIndexName1);
                if (hits.length == 0) {
                    return false;
                }
                assertEquals(1, hits.length);
                SearchHit hit = hits[0];
                Map<String, Object> source = hit.getSourceAsMap();
                assertEquals(5, source.get(ElasticFields.INT_VALUE1));
                assertEquals(22, source.get(ElasticFields.INT_VALUE2));
                assertEquals(22, source.get(ElasticFields.INT_VALUE3));
                assertEquals("Good Night", source.get(ElasticFields.STRING_VALUE1));
                assertEquals("Good Night", source.get(ElasticFields.STRING_VALUE2));
                assertEquals(RESPONSE, source.get(ElasticFields.STRING_VALUE3));
                assertTrue((boolean) source.get(ElasticFields.BOOL_VALUE1));
                assertEquals("22", source.get(ElasticFields.INT_VALUE_TO_STRING));
                return true;
            }, equalTo(true));

        given().ignoreExceptions()
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .pollInterval(POLL_INTERVAL_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
            .until(() -> {
                final String query = "SELECT * FROM " + h2HelloEntity1TableName;
                try (Statement stmt = h2Connection.createStatement()) {
                    java.sql.ResultSet rs = stmt.executeQuery(query);
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        if (count > 1) {
                            break;
                        }
                        assertEquals(REQUEST, rs.getString(DBFields.REQUEST));
                        assertEquals(RESPONSE, rs.getString(DBFields.RESPONSE));
                        assertEquals(HELLO2_METHOD_NAME, rs.getString(DBFields.METHOD_NAME));
                        assertEquals(SIMPLE4_SERVICE_NAME, rs.getString(DBFields.SERVICE_NAME));
                        assertNotNull(rs.getTimestamp(DBFields.INCOMING_TIME));
                        assertNotNull(rs.getTimestamp(DBFields.OUTCOMING_TIME));
                        assertEquals(RESTFUL_PUBLISHER_TYPE, rs.getString(DBFields.PUBLISHER_TYPE));

                        assertEquals(5, rs.getInt(DBFields.INT_VALUE1));
                        assertEquals(22, rs.getInt(DBFields.INT_VALUE2));
                        assertEquals(22, rs.getInt(DBFields.INT_VALUE3));
                        assertEquals("Good Night", rs.getString(DBFields.STRING_VALUE1));
                        assertEquals("Good Night", rs.getString(DBFields.STRING_VALUE2));
                        assertEquals(RESPONSE, rs.getString(DBFields.STRING_VALUE3));
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
    public void testKafkaHeaderAnnotations() throws Exception {
        final String REQUEST = getText("simple4_Hello.req.json");
        final String RESPONSE = getText("simple4_Hello.resp.txt");

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

        final String customElasticIndexName1 = CustomElasticEntity1.class.getAnnotation(Document.class).indexName();
        given().ignoreExceptions().await().atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS).until(() -> {
            removeIndexIfExists(customElasticIndexName1);
            return !elasticRunner.indexExists(customElasticIndexName1);
        }, equalTo(true));

        KeyValue<String, String> record = new KeyValue<>(null, REQUEST);
        record.addHeader(KafkaHeaders.METHOD_NAME, HELLO_METHOD_NAME, StandardCharsets.UTF_8);
        record.addHeader(KafkaHeaders.METHOD_PARAMETERS, "*, *", StandardCharsets.UTF_8);
        record.addHeader("testHeader", "testHeaderValue", StandardCharsets.UTF_8);
        cluster.send(SendKeyValues.to("hello-in-topic-4", Collections.singletonList(record)).useDefaults());

        ObserveKeyValues<String, String> observeRequest = ObserveKeyValues.on("hello-out-topic-4", 1)
            .with("metadata.max.age.ms", 1000)
            .build();
        List<String> observedValues = cluster.observeValues(observeRequest);
        assertEquals(1, observedValues.size());
        assertEquals(RESPONSE, observedValues.get(0));

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
                assertEquals(HELLO_METHOD_NAME, row.getString(CassandraFields.METHOD_NAME));
                assertEquals(SIMPLE4_SERVICE_NAME, row.getString(CassandraFields.SERVICE_NAME));
                assertNotNull(row.getTimestamp(CassandraFields.INCOMING_TIME));
                assertNotNull(row.getTimestamp(CassandraFields.OUTCOMING_TIME));
                assertEquals(PublisherType.KAFKA.toString(), row.getString(CassandraFields.PUBLISHER_TYPE));

                assertEquals(HELLO_METHOD_NAME, row.getString(CassandraFields.HEADER1));
                assertEquals("testHeaderValue", row.getString(CassandraFields.HEADER2));

                return true;
            }, equalTo(true));

        given().ignoreExceptions()
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .pollInterval(POLL_INTERVAL_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
            .until(() -> {
                SearchHit[] hits = getElasticSearchHits(customElasticIndexName1);
                if (hits.length == 0) {
                    return false;
                }
                assertEquals(1, hits.length);
                SearchHit hit = hits[0];
                Map<String, Object> source = hit.getSourceAsMap();
                assertEquals(HELLO_METHOD_NAME, source.get(ElasticFields.HEADER1));
                assertEquals("testHeaderValue", source.get(ElasticFields.HEADER2));
                return true;
            }, equalTo(true));
        given().ignoreException(InvalidQueryException.class)
            .await()
            .atMost(AWAIT_TIMEOUT, TimeUnit.SECONDS)
            .until(() -> {
                final String query = "SELECT * FROM " + h2HelloEntity1TableName;
                try (Statement stmt = h2Connection.createStatement()) {
                    java.sql.ResultSet rs = stmt.executeQuery(query);
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        if (count > 1) {
                            break;
                        }
                        assertEquals(REQUEST, rs.getString(DBFields.REQUEST));
                        assertEquals(RESPONSE, rs.getString(DBFields.RESPONSE));
                        assertEquals(HELLO_METHOD_NAME, rs.getString(DBFields.METHOD_NAME));
                        assertEquals(SIMPLE4_SERVICE_NAME, rs.getString(DBFields.SERVICE_NAME));
                        assertNotNull(rs.getTimestamp(DBFields.INCOMING_TIME));
                        assertNotNull(rs.getTimestamp(DBFields.OUTCOMING_TIME));
                        assertEquals(PublisherType.KAFKA.toString(), rs.getString(DBFields.PUBLISHER_TYPE));

                        assertEquals(HELLO_METHOD_NAME, rs.getString(DBFields.HEADER1));
                        assertEquals("testHeaderValue", rs.getString(DBFields.HEADER2));
                    }
                    assertEquals(1, count);
                    return true;
                }
            }, equalTo(true));

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
            log.warn(e);
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
        doQuite(() -> {
            // close runner
            try {
                elasticRunner.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        doQuite(() -> {
            // delete all files
            elasticRunner.clean();
        });
        doQuite(EmbeddedCassandraServerHelper::cleanEmbeddedCassandra);
        doQuite(() -> cluster.stop());

        h2Server.stop();
    }

}
