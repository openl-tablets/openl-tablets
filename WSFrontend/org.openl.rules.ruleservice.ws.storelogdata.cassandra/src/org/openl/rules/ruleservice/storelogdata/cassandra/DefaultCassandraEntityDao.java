package org.openl.rules.ruleservice.storelogdata.cassandra;

import java.util.concurrent.CompletionStage;

import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Insert;

@Dao
public interface DefaultCassandraEntityDao {
    @Insert
    CompletionStage<Void> insert(DefaultCassandraEntity entity);
}
