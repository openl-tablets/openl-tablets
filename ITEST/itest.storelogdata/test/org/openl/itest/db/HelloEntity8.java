package org.openl.itest.db;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.openl.rules.ruleservice.storelogdata.annotation.Value;

@Entity(name = "openl_logging_hello_entity8")
public class HelloEntity8 {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "openl_logging_hello_entity8_generator")
    @SequenceGenerator(name = "openl_logging_hello_entity8_generator", sequenceName = "openl_logging_hello_entity8_generator", allocationSize = 50)
    private Long id;

    @Value("hour")
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
