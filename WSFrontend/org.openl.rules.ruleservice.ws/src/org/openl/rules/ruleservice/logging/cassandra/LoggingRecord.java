package org.openl.rules.ruleservice.logging.cassandra;

import java.util.Date;

import org.springframework.data.cassandra.mapping.PrimaryKey;
import org.springframework.data.cassandra.mapping.Table;

@Table
public class LoggingRecord {
    @PrimaryKey
    private String id;

    private Date incomingTime;
    private Date outcomingTime;

    private String request;
    private String response;

    private String serviceName;
    private String url;
    private String inputName;

    public LoggingRecord(String id,
            String request,
            String response,
            Date incomingTime,
            Date outcomingTime,
            String serviceName,
            String url,
            String inputName) {
        this.id = id;
        this.request = request;
        this.response = response;
        this.incomingTime = incomingTime;
        this.outcomingTime = outcomingTime;
        this.inputName = inputName;
        this.url = url;
        this.serviceName = serviceName;
    }

    public String getId() {
        return id;
    }

    public String getRequest() {
        return request;
    }

    public String getResponse() {
        return response;
    }

    public Date getIncomingTime() {
        return incomingTime;
    }

    public Date getOutcomingTime() {
        return outcomingTime;
    }

    public String getInputName() {
        return inputName;
    }

    public String getServiceName() {
        return serviceName;
    }
    
    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "LoggingRecord [id=" + id + "]";
    }
}
