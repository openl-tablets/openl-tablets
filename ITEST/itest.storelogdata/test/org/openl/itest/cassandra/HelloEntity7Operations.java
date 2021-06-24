package org.openl.itest.cassandra;

import java.util.concurrent.CompletionStage;

import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.AbstractReflectiveEntityOperations;

public class HelloEntity7Operations extends AbstractReflectiveEntityOperations<HelloEntity7Mapper, HelloEntity7Dao, HelloEntity7> {

    @Override
    protected Class<HelloEntity7Mapper> getEntityMapperClass() {
        return HelloEntity7Mapper.class;
    }

    @Override
    protected HelloEntity7Dao getDao(HelloEntity7Mapper entityMapper) {
        return entityMapper.getDao();
    }

    @Override
    public CompletionStage<Void> insert(HelloEntity7Dao dao, HelloEntity7 entity) {
        return dao.insert(entity);
    }
}
