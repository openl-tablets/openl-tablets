package org.openl.itest.cassandra;

import com.datastax.oss.driver.api.mapper.annotations.DaoFactory;
import com.datastax.oss.driver.api.mapper.annotations.Mapper;

@Mapper
public interface HelloEntity1Mapper {
    @DaoFactory
    public HelloEntity1Dao getDao();
}
