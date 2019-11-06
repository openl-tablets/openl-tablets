package org.openl.itest.cassandra;

import java.util.concurrent.CompletionStage;

import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.DaoCreationException;
import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.EntityOperations;

import com.datastax.oss.driver.api.core.CqlSession;

public class HelloEntity1Operations implements EntityOperations<HelloEntity1Dao, HelloEntity1> {

    @Override
    public HelloEntity1Dao buildDao(CqlSession arg0) throws DaoCreationException {
        HelloEntity1Mapper helloEntity1Mapper = new HelloEntity1MapperBuilder(arg0).build();
        return helloEntity1Mapper.getDao();
    }

    @Override
    public CompletionStage<Void> insert(HelloEntity1Dao dao, HelloEntity1 entity) {
        return dao.insert(entity);
    }
}
