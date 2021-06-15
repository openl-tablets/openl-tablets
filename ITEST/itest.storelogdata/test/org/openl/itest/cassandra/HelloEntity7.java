package org.openl.itest.cassandra;

import org.openl.rules.ruleservice.storelogdata.annotation.Value;
import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.EntitySupport;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;

@Entity
@EntitySupport(HelloEntity7Operations.class)
@CqlName("openl_logging_hello_entity7")
public class HelloEntity7 {

    @PartitionKey()
    @Value(value = "id")
    private String id;

    @Value(value = "response")
    private String response;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
