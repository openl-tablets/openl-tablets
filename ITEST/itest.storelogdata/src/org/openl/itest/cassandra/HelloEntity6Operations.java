package org.openl.itest.cassandra;

import java.util.concurrent.CompletionStage;

import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.AbstractReflectiveEntityOperations;

public class HelloEntity6Operations extends AbstractReflectiveEntityOperations<HelloEntity6Mapper, HelloEntity6Dao, HelloEntity6> {

    @Override
    protected Class<HelloEntity6Mapper> getEntityMapperClass() {
        return HelloEntity6Mapper.class;
    }

    @Override
    protected HelloEntity6Dao getDao(HelloEntity6Mapper entityMapper) {
        return entityMapper.getDao();
    }

    @Override
    public CompletionStage<Void> insert(HelloEntity6Dao dao, HelloEntity6 entity) {
        return dao.insert(entity);
    }
}
