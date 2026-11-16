package org.openl.itest.db;

import static org.openl.itest.db.DBFields.ERROR_MESSAGE;
import static org.openl.itest.db.DBFields.HOUR;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import lombok.Getter;
import lombok.Setter;

import org.openl.rules.ruleservice.storelogdata.annotation.Value;

/**
 * A log record whose hour is mandatory: it is saved only when the store log data advices of the call have run.
 */
@Entity(name = "openl_logging_hello_entity10")
@Getter
@Setter
public class HelloEntity10 {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "openl_logging_hello_entity10_generator")
    @SequenceGenerator(name = "openl_logging_hello_entity10_generator", sequenceName = "openl_logging_hello_entity10_generator", allocationSize = 50)
    private Long id;

    @Value("hour")
    @Column(name = HOUR, nullable = false)
    private Integer hour;

    @Value("errorMessage")
    @Column(name = ERROR_MESSAGE)
    private String errorMessage;
}
