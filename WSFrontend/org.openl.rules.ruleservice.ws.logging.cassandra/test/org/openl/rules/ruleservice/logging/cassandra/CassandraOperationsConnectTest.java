package org.openl.rules.ruleservice.logging.cassandra;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

@RunWith(MockitoJUnitRunner.class)
public class CassandraOperationsConnectTest {
    private CassandraOperations operations;
    @Mock
    private Cluster cluster;

    @Before
    public void init(){
        operations = new CassandraOperations();
        operations.setPort("9042");
        operations.setCluster(cluster);
        Session session = mock(Session.class);
        when(cluster.connect(anyString())).thenReturn(session);
    }

    @Test
    public void emptyContactPointsTest() {
        operations.setContactpoints("");
        operations.connect();
    }
    @Test
    public void singleContactPointsTest() {
        operations.setContactpoints("127.0.0.1");
        operations.connect();
    }

    @Test
    public void multipleContactPointsTest() {
        operations.setContactpoints("127.0.0.2,127.0.0.3");
        operations.connect();
    }

    @Test
    public void multipleContactPointsTest_withSpaces() {
        operations.setContactpoints("127.0.0.2 , 127.0.0.3");
        operations.connect();
    }

}
