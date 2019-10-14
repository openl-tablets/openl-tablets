package org.openl.itest.cassandra;

import java.util.concurrent.CompletionStage;

import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.AbstractReflectiveEntityOperations;

public class HelloEntity2Operations extends AbstractReflectiveEntityOperations<HelloEntity2Mapper, HelloEntity2Dao, HelloEntity2> {

    @Override
    protected Class<HelloEntity2Mapper> getEntityMapperClass() {
        return HelloEntity2Mapper.class;
    }

    @Override
    protected HelloEntity2Dao getDao(HelloEntity2Mapper entityMapper) {
        return entityMapper.getDao();
    }

    @Override
    public CompletionStage<Void> insert(HelloEntity2Dao dao, HelloEntity2 entity) {
        return dao.insert(entity);
    }
}
