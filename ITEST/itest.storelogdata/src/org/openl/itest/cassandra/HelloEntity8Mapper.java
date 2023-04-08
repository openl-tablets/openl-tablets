package org.openl.itest.cassandra;

import com.datastax.oss.driver.api.mapper.annotations.DaoFactory;
import com.datastax.oss.driver.api.mapper.annotations.Mapper;

@Mapper
public interface HelloEntity8Mapper {
    @DaoFactory
    HelloEntity8Dao getDao();
}
