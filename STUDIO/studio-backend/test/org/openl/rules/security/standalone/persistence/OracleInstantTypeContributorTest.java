package org.openl.rules.security.standalone.persistence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsJdbcTimestampJdbcType;
import org.junit.jupiter.api.Test;

class OracleInstantTypeContributorTest {

    @Test
    void readsInstantsAsTimestampsInUtcOnOracle() {
        var contributions = contributeFor(new OracleDialect());

        verify(contributions).contributeJdbcType(TimestampUtcAsJdbcTimestampJdbcType.INSTANCE);
    }

    @Test
    void keepsTheMappingOfAnotherDatabase() {
        var contributions = contributeFor(new PostgreSQLDialect());

        verify(contributions, never()).contributeJdbcType(any());
    }

    private static TypeContributions contributeFor(Dialect dialect) {
        var jdbcServices = mock(JdbcServices.class);
        when(jdbcServices.getDialect()).thenReturn(dialect);
        var serviceRegistry = mock(ServiceRegistry.class);
        when(serviceRegistry.requireService(JdbcServices.class)).thenReturn(jdbcServices);
        var contributions = mock(TypeContributions.class);
        new OracleInstantTypeContributor().contribute(contributions, serviceRegistry);
        return contributions;
    }
}
