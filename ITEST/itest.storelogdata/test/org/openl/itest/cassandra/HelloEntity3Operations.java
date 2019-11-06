package org.openl.itest.cassandra;

import java.util.concurrent.CompletionStage;

import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.AbstractReflectiveEntityOperations;

public class HelloEntity3Operations extends AbstractReflectiveEntityOperations<HelloEntity3Mapper, HelloEntity3Dao, HelloEntity3> {

    @Override
    protected Class<HelloEntity3Mapper> getEntityMapperClass() {
        return HelloEntity3Mapper.class;
    }

    @Override
    protected HelloEntity3Dao getDao(HelloEntity3Mapper entityMapper) {
        return entityMapper.getDao();
    }

    @Override
    public CompletionStage<Void> insert(HelloEntity3Dao dao, HelloEntity3 entity) {
        return dao.insert(entity);
    }
}
