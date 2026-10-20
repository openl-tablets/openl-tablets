package org.openl.rules.security.standalone.persistence;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsJdbcTimestampJdbcType;

/**
 * Reads and writes the instants of the entities on Oracle as timestamps in UTC.
 *
 * <p>The migrations make a column of a time a plain timestamp on every database, and the databases keep the time in
 * UTC. On Oracle, Hibernate reads an instant as a timestamp with a time zone, which the Oracle driver cannot make of a
 * plain timestamp: every read fails with {@code ORA-18716}. Read and written as a timestamp in UTC, an instant keeps
 * the value Oracle holds, so the rows written before read back as they were.
 *
 * <p>The other databases keep the mapping Hibernate gives them.
 */
public class OracleInstantTypeContributor implements TypeContributor {

    @Override
    public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        if (serviceRegistry.requireService(JdbcServices.class).getDialect() instanceof OracleDialect) {
            typeContributions.contributeJdbcType(TimestampUtcAsJdbcTimestampJdbcType.INSTANCE);
        }
    }
}
