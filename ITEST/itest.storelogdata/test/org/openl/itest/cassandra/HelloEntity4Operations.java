package org.openl.itest.cassandra;

import java.util.concurrent.CompletionStage;

import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.AbstractReflectiveEntityOperations;

public class HelloEntity4Operations extends AbstractReflectiveEntityOperations<HelloEntity4Mapper, HelloEntity4Dao, HelloEntity4> {

    @Override
    protected Class<HelloEntity4Mapper> getEntityMapperClass() {
        return HelloEntity4Mapper.class;
    }

    @Override
    protected HelloEntity4Dao getDao(HelloEntity4Mapper entityMapper) {
        return entityMapper.getDao();
    }

    @Override
    public CompletionStage<Void> insert(HelloEntity4Dao dao, HelloEntity4 entity) {
        return dao.insert(entity);
    }
}
