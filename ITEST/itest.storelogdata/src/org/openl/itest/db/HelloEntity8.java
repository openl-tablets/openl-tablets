package org.openl.itest.db;

import static org.openl.itest.db.DBFields.HOUR;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import org.openl.rules.ruleservice.storelogdata.annotation.Value;

@Entity(name = "openl_logging_hello_entity8")
public class HelloEntity8 {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "openl_logging_hello_entity8_generator")
    @SequenceGenerator(name = "openl_logging_hello_entity8_generator", sequenceName = "openl_logging_hello_entity8_generator", allocationSize = 50)
    private Long id;

    @Value("hour")
    @Column(name = HOUR)
    private Integer hour;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getHour() {
        return hour;
    }

    public void setHour(Integer hour) {
        this.hour = hour;
    }
}
