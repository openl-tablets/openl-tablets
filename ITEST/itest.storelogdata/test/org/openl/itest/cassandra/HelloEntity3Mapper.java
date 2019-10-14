package org.openl.itest.cassandra;

import com.datastax.oss.driver.api.mapper.annotations.DaoFactory;
import com.datastax.oss.driver.api.mapper.annotations.Mapper;

@Mapper
public interface HelloEntity3Mapper {
    @DaoFactory
    public HelloEntity3Dao getDao();
}
