package org.openl.rules.ruleservice.storelogdata.cassandra.annotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.datastax.oss.driver.api.core.CqlSession;

public abstract class AbstractReflectiveEntityOperations<M, T, E> implements EntityOperations<T, E> {

    protected abstract Class<M> getEntityMapperClass();

    protected abstract T getDao(M entityMapper);

    @Override
    public T buildDao(CqlSession session) throws DaoCreationException {
        try {
            Class<M> entityMapperClass = getEntityMapperClass();
            Class<?> mapperBuilderClass = entityMapperClass.getClassLoader()
                .loadClass(entityMapperClass.getName() + "Builder");
            Constructor<?> entityMapperBuilderConstructor = mapperBuilderClass.getConstructor(CqlSession.class);
            Object entityMapperBuilder = entityMapperBuilderConstructor.newInstance(session);
            Method entityMapperBuilderBuildMethod = mapperBuilderClass.getMethod("build");
            @SuppressWarnings("unchecked")
            M target = (M) entityMapperBuilderBuildMethod.invoke(entityMapperBuilder);
            return getDao(target);
        } catch (ReflectiveOperationException e) {
            throw new DaoCreationException(e);
        }
    }
}
