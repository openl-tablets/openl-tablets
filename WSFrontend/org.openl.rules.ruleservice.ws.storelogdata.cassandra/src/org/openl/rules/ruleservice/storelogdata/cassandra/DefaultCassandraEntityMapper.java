package org.openl.rules.ruleservice.storelogdata.cassandra;

import com.datastax.oss.driver.api.mapper.annotations.DaoFactory;
import com.datastax.oss.driver.api.mapper.annotations.Mapper;

@Mapper
public interface DefaultCassandraEntityMapper {
    @DaoFactory
    public DefaultCassandraEntityDao getDao();
}
