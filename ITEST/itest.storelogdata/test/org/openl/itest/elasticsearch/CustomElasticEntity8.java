package org.openl.itest.elasticsearch;

import org.openl.rules.ruleservice.storelogdata.annotation.Value;
import org.openl.rules.ruleservice.storelogdata.cassandra.TimeBasedUUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "openl_log_custom_index_8")
public class CustomElasticEntity8 {

    @Id
    @Value(converter = TimeBasedUUID.class)
    private String id;

    @Value("hour")
    private Integer hour;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getHour() {
        return hour;
    }

    public void setHour(Integer hour) {
        this.hour = hour;
    }

    @Override
    public String toString() {
        return "CustomElasticEntity8 [id=" + id + "]";
    }

}
