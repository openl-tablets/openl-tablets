package org.openl.rules.ruleservice.storelogdata.db;

import java.time.ZonedDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;

import lombok.Getter;
import lombok.Setter;

import org.openl.rules.ruleservice.storelogdata.annotation.IncomingTime;
import org.openl.rules.ruleservice.storelogdata.annotation.MethodName;
import org.openl.rules.ruleservice.storelogdata.annotation.OutcomingTime;
import org.openl.rules.ruleservice.storelogdata.annotation.Publisher;
import org.openl.rules.ruleservice.storelogdata.annotation.Request;
import org.openl.rules.ruleservice.storelogdata.annotation.Response;
import org.openl.rules.ruleservice.storelogdata.annotation.ServiceName;
import org.openl.rules.ruleservice.storelogdata.annotation.Url;

@Entity(name = "openl_log_data")
public class DefaultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "openl_log_data_generator")
    @Getter
    @SequenceGenerator(name = "openl_log_data_generator", sequenceName = "openl_log_data_generator", allocationSize = 50)
    @Setter
    private Long id;

    @Getter
    @IncomingTime
    @Setter
    private ZonedDateTime incomingTime;

    @Getter
    @OutcomingTime
    @Setter
    private ZonedDateTime outcomingTime;

    @Getter
    @Request
    @Setter
    @Lob
    private String request;

    @Getter
    @Response
    @Setter
    @Lob
    private String response;

    @Getter
    @ServiceName
    @Setter
    private String serviceName;

    @Getter
    @Setter
    @Url
    private String url;

    @Getter
    @MethodName
    @Setter
    private String methodName;

    @Getter
    @Publisher
    @Setter
    private String publisherType;

    @Override
    public String toString() {
        return "DefaultEntity [id=" + id + "]";
    }
}
