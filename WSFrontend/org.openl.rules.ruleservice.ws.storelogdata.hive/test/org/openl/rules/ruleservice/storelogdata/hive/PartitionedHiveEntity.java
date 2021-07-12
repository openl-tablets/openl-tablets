package org.openl.rules.ruleservice.storelogdata.hive;

import java.time.ZonedDateTime;

import org.openl.rules.ruleservice.storelogdata.RandomUUID;
import org.openl.rules.ruleservice.storelogdata.annotation.IncomingTime;
import org.openl.rules.ruleservice.storelogdata.annotation.OutcomingTime;
import org.openl.rules.ruleservice.storelogdata.annotation.Request;
import org.openl.rules.ruleservice.storelogdata.annotation.Value;
import org.openl.rules.ruleservice.storelogdata.hive.annotation.Entity;
import org.openl.rules.ruleservice.storelogdata.hive.annotation.Partition;

@Entity("partitioned_data")
public class PartitionedHiveEntity {

    @Value(converter = RandomUUID.class)
    private String id;

    @IncomingTime
    @Partition
    private ZonedDateTime incomingTime;

    @OutcomingTime
    @Partition(1)
    private ZonedDateTime outcomingTime;

    @Request
    private String request;

    public PartitionedHiveEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ZonedDateTime getIncomingTime() {
        return incomingTime;
    }

    public void setIncomingTime(ZonedDateTime incomingTime) {
        this.incomingTime = incomingTime;
    }

    public ZonedDateTime getOutcomingTime() {
        return outcomingTime;
    }

    public void setOutcomingTime(ZonedDateTime outcomingTime) {
        this.outcomingTime = outcomingTime;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    @Override
    public String toString() {
        return "PartitionedEntity [id=" + id + "]";
    }
}
