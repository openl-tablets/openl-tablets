package org.openl.itest.serviceclass;

import java.util.List;
import java.util.Map;

import org.openl.rules.ruleservice.storelogdata.advice.StoreLogDataAdvice;
import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.CassandraSession;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;

public class Simple6ServiceRetrieverBefore implements StoreLogDataAdvice {

    private static final String QUERY = "SELECT response FROM openl_logging_hello_entity6 WHERE id = '%s'";

    @CassandraSession
    private CqlSession cassandraSession;

    @Override
    public void prepare(Map<String, Object> values, Object[] args, Object result, Exception ex) {
        String id = (String) args[0];
        List<Row> rows = cassandraSession.execute(String.format(QUERY, id)).all();
        if (!rows.isEmpty()) {
            values.put("responseTemp", rows.get(0).getString("response"));
        }
    }
}
