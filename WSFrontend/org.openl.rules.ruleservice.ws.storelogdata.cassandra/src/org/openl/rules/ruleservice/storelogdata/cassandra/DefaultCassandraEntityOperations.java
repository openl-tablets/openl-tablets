package org.openl.rules.ruleservice.storelogdata.cassandra;

import java.util.concurrent.CompletionStage;

import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.AbstractReflectiveEntityOperations;

public class DefaultCassandraEntityOperations extends AbstractReflectiveEntityOperations<DefaultCassandraEntityMapper, DefaultCassandraEntityDao, DefaultCassandraEntity> {

    @Override
    protected Class<DefaultCassandraEntityMapper> getEntityMapperClass() {
        return DefaultCassandraEntityMapper.class;
    }

    @Override
    protected DefaultCassandraEntityDao getDao(DefaultCassandraEntityMapper entityMapper) {
        return entityMapper.getDao();
    }

    @Override
    public CompletionStage<Void> insert(DefaultCassandraEntityDao dao, DefaultCassandraEntity entity) {
        return dao.insert(entity);
    }
}
