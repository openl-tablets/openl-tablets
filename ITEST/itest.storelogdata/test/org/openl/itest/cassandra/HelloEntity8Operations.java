package org.openl.itest.cassandra;

import java.util.concurrent.CompletionStage;

import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.AbstractReflectiveEntityOperations;

public class HelloEntity8Operations extends AbstractReflectiveEntityOperations<HelloEntity8Mapper, HelloEntity8Dao, HelloEntity8> {

    @Override
    protected Class<HelloEntity8Mapper> getEntityMapperClass() {
        return HelloEntity8Mapper.class;
    }

    @Override
    protected HelloEntity8Dao getDao(HelloEntity8Mapper entityMapper) {
        return entityMapper.getDao();
    }

    @Override
    public CompletionStage<Void> insert(HelloEntity8Dao dao, HelloEntity8 entity) {
        return dao.insert(entity);
    }
}
